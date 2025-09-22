package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.*
import com.fantamomo.kevent.manager.config.DispatchConfig
import com.fantamomo.kevent.manager.config.DispatchConfigKey
import com.fantamomo.kevent.manager.internal.all
import com.fantamomo.kevent.manager.internal.rethrowIfFatal
import com.fantamomo.kevent.manager.settings.Settings
import com.fantamomo.kevent.manager.settings.getSetting
import com.fantamomo.kevent.processor.HandlerDefinition
import com.fantamomo.kevent.processor.InternalProcessorApi
import com.fantamomo.kevent.utils.InjectionName
import kotlinx.coroutines.*
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

/**
 * Default implementation of the EventManager interface for managing event listeners
 * and dispatching events.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class DefaultEventManager internal constructor(
    components: EventManagerComponent<*>, // Injected components for customization (e.g., exception handling, coroutine scope)
) : EventManager {

    // Maps event types to their handler collections
    private val handlers: ConcurrentHashMap<KClass<out Dispatchable>, HandlerBucket<out Dispatchable>> =
        ConcurrentHashMap()

    // Stores last dispatched "sticky" events so late listeners can still receive them
    private val stickyEvents: ConcurrentHashMap<KClass<out Dispatchable>, Dispatchable> = ConcurrentHashMap()

    // Exception handler to report listener/dispatch errors
    private val exceptionHandler = components.getOrThrow(ExceptionHandler)

    // List of resolvers for injecting extra parameters into listener methods
    private val parameterResolver: List<ListenerParameterResolver<*>>

    // Shared lock management for exclusive listener execution
    private val sharedExclusiveExecution = components[SharedExclusiveExecution] ?: SharedExclusiveExecution()

    // Coroutine scope for async dispatch; uses provided scope or default SupervisorJob
    private val scope: CoroutineScope =
        components[EventCoroutineScope]?.scope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Invoker for calling the methods from Listener
    private val invoker: ListenerInvoker = components.getOrThrow(ListenerInvoker.Key)

    // ListenerProcessorRegistry for optimized listener registration due to K-Event-Processor
    private val listenerProcessorRegistry = components[ListenerProcessorRegistry] ?: ListenerProcessorRegistry.Empty

    // Indicates if manager has been closed (no more registration/dispatch possible)
    private var isClosed = false

    // Global setting: whether to dispatch DeadEvent when no listener handled an event
    private val dispatchDeadEvents = components.getSetting(Settings.DISPATCH_DEAD_EVENTS)

    init {
        var comps = components

        // Optional injection: make the manager itself available to listeners
        if (!comps.getSetting(Settings.DISABLE_EVENTMANAGER_INJECTION)) comps += ListenerParameterResolver.static(
            "manager",
            EventManager::class,
            this
        )

        // Optional injection: provide logger instance to listeners
        if (!comps.getSetting(Settings.DISABLE_LOGGER_INJECTION)) ListenerParameterResolver.static(
            "logger",
            Logger::class,
            logger
        )
        // Optional injection: provide new coroutine scope to listeners
        if (!comps.getSetting(Settings.DISABLE_SCOPE_INJECTION)) ListenerParameterResolver.static(
            "scope",
            CoroutineScope::class,
            CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))
        )

        // Internal parameter resolvers (waiting/sticky/config info)
        if (!comps.getSetting(Settings.DISABLE_IS_WAITING_INJECTION)) comps += IsWaitingParameterResolver
        if (!comps.getSetting(Settings.DISABLE_IS_STICKY_INJECTION)) comps += IsStickyParameterResolver
        if (!comps.getSetting(Settings.DISABLE_CONFIG_INJECTION)) comps += ConfigParameterResolver

        // Finalize list of all available resolvers
        parameterResolver = comps.getAll(ListenerParameterResolver.Key)
    }

    // ---------
    // Functions
    // ---------
    /** Checks if at least one listener class of this type is already registered */
    private fun existListener(clazz: KClass<out Listener>) = handlers.values.any { it.existListener(clazz) }

    /** Finds all registered listener methods for a specific listener class */
    private fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
        return handlers.values.flatMap { it.findAllListeners(clazz) }
    }

    /**
     * Registers an already-known listener definition (e.g., on re-register of same class)
     */
    private fun registerRegisteredListener(listenerClass: KClass<out Listener>, listener: Listener) {
        val listeners = findAllListeners(listenerClass)
        out@ for (registered in listeners) {
            @Suppress("UNCHECKED_CAST")
            registered as RegisteredKFunctionListener<Dispatchable>
            var newConfiguration: EventConfiguration<Dispatchable>? = null

            // If event parameter is non-nullable, use default configuration
            if (!registered.kFunction.parameters[1].type.isMarkedNullable) {
                newConfiguration =
                    (listener as? ListenerConfiguration)?.defaultConfiguration ?: EventConfiguration.default()
            } else {
                // Otherwise, call method with null to capture custom configuration via exception
                val args = registered.kFunction.parameters.map { param ->
                    when (param.index) {
                        0 -> listener // "this" for the function
                        1 -> null // event argument is null
                        else -> when (registered) {
                            is RegisteredKFunctionListenerNormal -> registered.resolvers[param]?.valueByConfiguration
                            is RegisteredKFunctionListenerProcessor -> registered.resolvers.getOrNull(param.index - 2)?.valueByConfiguration
                        }
                    }
                }
                val method = registered.kFunction
                try {
                    if (method.isSuspend) {
                        var exception: InvocationTargetException? = null

                        // Launch a coroutine using Dispatchers.Unconfined so that the code runs
                        // immediately in the current thread. This avoids switching threads.
                        val job = scope.launch(Dispatchers.Unconfined) {
                            try {
                                method.callSuspend(*args.toTypedArray())
                                exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                    onMethodDidNotThrowConfiguredException(listener, method)
                                }
                            } catch (e: InvocationTargetException) {
                                exception = e
                            }
                        }
                        // Immediately cancel the coroutine.
                        // Since we're using Unconfined, if a suspend point was reached, the coroutine
                        // would switch threads, and cancellation ensures it doesn't continue execution.
                        job.cancel()
                        if (exception != null) throw exception
                    } else {
                        method.call(*args.toTypedArray())
                        exceptionHandler("onMethodDidNotThrowConfiguredException") {
                            onMethodDidNotThrowConfiguredException(listener, method)
                        }
                    }
                } catch (e: InvocationTargetException) {
                    // Custom config captured from thrown exception
                    val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                    if (config !is EventConfiguration<*>) continue@out
                    @Suppress("UNCHECKED_CAST")
                    newConfiguration = config as EventConfiguration<Dispatchable>
                } catch (e: Throwable) {
                    e.rethrowIfFatal()
                    exceptionHandler("onUnexpectedExceptionDuringRegistration") {
                        onUnexpectedExceptionDuringRegistration(listener, method, e)
                    }
                    continue@out
                }
            }
            if (newConfiguration == null) continue@out

            // Replace with a new instance bound to this listener
            val new = registered.withNewListener(listener)
            getOrCreateHandlerBucket(registered.type).add(new)
        }
    }

    @OptIn(InternalProcessorApi::class)
    private fun registerWithProcessor(clazz: KClass<out Listener>, listener: Listener): Boolean {
        val registry = listenerProcessorRegistry.getRegistry(clazz) ?: return false

        val defaultListenerConfig = (listener as? ListenerConfiguration)?.defaultConfiguration

        out@ for (handler in registry.listeners) {
            check(handler.listener == clazz) { "Handler for $clazz is not the same as the given listener." }

            val method = handler.method

            val resolvers = handler.args.mapIndexed { index, param ->
                parameterResolver.find { it.name == param.name && it.type == param.type } ?: run {
                    exceptionHandler("onParameterHasNoResolver") {
                        val parameter = method.parameters[index + 2]
                        onParameterHasNoResolver(
                            listener,
                            method,
                            parameter,
                            parameter.findAnnotation<InjectionName>()?.value ?: parameter.name!!,
                            parameter.type
                        )
                    }
                    return false
                }
            }

            @Suppress("UNCHECKED_CAST")
            val typedEventClass = handler.event as KClass<Dispatchable>

            // Prepare arguments for capturing config if nullable
            val args = (0..(resolvers.size + 2)).map { index ->
                when (index) {
                    0 -> listener
                    1 -> null
                    else -> resolvers[index - 2].valueByConfiguration
                }
            }

            val defaultConfigOrCaptured: EventConfiguration<Dispatchable>? =
                if (handler.isNullable) {
                    defaultListenerConfig ?: EventConfiguration.default()
                } else {
                    try {
                        method.isAccessible = true
                        if (handler.isSuspend) {
                            var exception: InvocationTargetException? = null

                            // Launch a coroutine using Dispatchers.Unconfined so that the code runs
                            // immediately in the current thread. This avoids switching threads.
                            val job = scope.launch(Dispatchers.Unconfined) {
                                try {
                                    method.callSuspend(*args.toTypedArray())
                                    exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                        onMethodDidNotThrowConfiguredException(listener, method)
                                    }
                                } catch (e: InvocationTargetException) {
                                    exception = e
                                }
                            }
                            // Immediately cancel the coroutine.
                            // Since we're using Unconfined, if a suspend point was reached, the coroutine
                            // would switch threads, and cancellation ensures it doesn't continue execution.
                            job.cancel()
                            if (exception != null) throw exception
                        } else {
                            method.call(*args.toTypedArray())
                            exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                onMethodDidNotThrowConfiguredException(listener, method)
                            }
                        }
                        null
                    } catch (_: InaccessibleObjectException) {
                        exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                        null
                    } catch (e: InvocationTargetException) {
                        // Check if method threw captured configuration
                        val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                        if (config !is EventConfiguration<*>) {
                            exceptionHandler("onMethodThrewUnexpectedException") {
                                onMethodThrewUnexpectedException(listener, method, e.targetException)
                            }
                            null
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            config as EventConfiguration<Dispatchable>
                        }
                    } catch (t: Throwable) {
                        t.rethrowIfFatal()
                        exceptionHandler("onUnexpectedExceptionDuringRegistration") {
                            onUnexpectedExceptionDuringRegistration(listener, method, t)
                        }
                        null
                    }
                }

            if (defaultConfigOrCaptured == null) continue@out

            try {
                method.isAccessible = true
            } catch (_: InaccessibleObjectException) {
                exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                continue@out
            }

            // Wrap into RegisteredKFunctionListener and store
            val handler = RegisteredKFunctionListenerProcessor(
                type = typedEventClass,
                listener = listener,
                kFunction = method,
                configuration = defaultConfigOrCaptured,
                resolvers = resolvers,
                handlerDefinition = handler,
                manager = this,
            )
            getOrCreateHandlerBucket(typedEventClass).add(handler)
        }
        return true
    }

    override fun register(listener: Listener) {
        checkClosed()
        val listenerClass = listener::class

        // If the K-Event-Processor was used, we use more efficient registration
        if (listenerClass in listenerProcessorRegistry) {
            if (registerWithProcessor(listenerClass, listener)) {
                return
            }
        }

        // If we already have methods from this class, just rebind
        if (existListener(listenerClass)) {
            registerRegisteredListener(listenerClass, listener)
            return
        }

        val defaultListenerConfig = (listener as? ListenerConfiguration)?.defaultConfiguration

        // Scan methods for @Register annotation
        out@ for (method in listenerClass.declaredMemberFunctions) {
            if (!method.hasAnnotation<Register>()) continue
            // filter out not public methods
            if (method.visibility != KVisibility.PUBLIC) {
                exceptionHandler("onMethodNotPublic") { onMethodNotPublic(listener, method, method.visibility) }
                continue@out
            }

            // Must have at least 2 (this@Listener, event) parameters
            val parameters = method.parameters
            if (parameters.size < 2) {
                exceptionHandler("onMethodHasNoParameters") { onMethodHasNoParameters(listener, method) }
                continue@out
            }

            // Resolve extra parameters (beyond listener + event)
            val resolvers = parameters.dropWhile { it.index < 2 }.associateWith { parameter ->
                val name = (parameter.findAnnotation<InjectionName>()?.value ?: parameter.name)
                parameterResolver.find {
                    it.name == name && it.type == parameter.type.classifier
                } ?: run {
                    if (name == null) {
                        // If execution reaches this point, something has gone seriously wrong.
                        // Every parameter should have a name.
                        // `INSTANCE` and `EXTENSION_RECEIVER` are exceptions — they have no names,
                        // but we’ve already handled them, which is why we log this.
                        logger.severe(
                            "A Parameter which should have a name, has none. " +
                                    "(Parameter index: ${parameter.index}) (Type: ${parameter.type}) " +
                                    "(Kind: ${parameter.kind} (Method: ${listenerClass.jvmName}#${method.name})"
                        )
                    } else {
                        // Log missing resolver
                        exceptionHandler("onParameterHasNoResolver") {
                            onParameterHasNoResolver(listener, method, parameter, name, parameter.type)
                        }
                    }
                    continue@out
                }
            }

            // Verify second parameter type is a Dispatchable event
            val eventClass = parameters[1].type.classifier as? KClass<*> ?: continue
            if (!Dispatchable::class.isSuperclassOf(eventClass)) {
                exceptionHandler("onMethodHasNoDispatchableParameter") {
                    onMethodHasNoDispatchableParameter(listener, method, parameters[1].type)
                }
                continue@out
            }

            @Suppress("UNCHECKED_CAST")
            val typedEventClass = eventClass as KClass<Dispatchable>

            // Prepare arguments for capturing config if nullable
            val args = method.parameters.map { param ->
                when (param.index) {
                    0 -> listener
                    1 -> null
                    else -> resolvers[param]?.valueByConfiguration
                }
            }

            val defaultConfigOrCaptured: EventConfiguration<Dispatchable>? =
                if (!parameters[1].type.isMarkedNullable) {
                    defaultListenerConfig ?: EventConfiguration.default()
                } else {
                    try {
                        method.isAccessible = true
                        if (method.isSuspend) {
                            var exception: InvocationTargetException? = null

                            // Launch a coroutine using Dispatchers.Unconfined so that the code runs
                            // immediately in the current thread. This avoids switching threads.
                            val job = scope.launch(Dispatchers.Unconfined) {
                                try {
                                    method.callSuspend(*args.toTypedArray())
                                    exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                        onMethodDidNotThrowConfiguredException(listener, method)
                                    }
                                } catch (e: InvocationTargetException) {
                                    exception = e
                                }
                            }
                            // Immediately cancel the coroutine.
                            // Since we're using Unconfined, if a suspend point was reached, the coroutine
                            // would switch threads, and cancellation ensures it doesn't continue execution.
                            job.cancel()
                            if (exception != null) throw exception
                        } else {
                            method.call(*args.toTypedArray())
                            exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                onMethodDidNotThrowConfiguredException(listener, method)
                            }
                        }
                        null
                    } catch (_: InaccessibleObjectException) {
                        exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                        null
                    } catch (e: InvocationTargetException) {
                        // Check if method threw captured configuration
                        val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                        if (config !is EventConfiguration<*>) {
                            exceptionHandler("onMethodThrewUnexpectedException") {
                                onMethodThrewUnexpectedException(listener, method, e.targetException)
                            }
                            null
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            config as EventConfiguration<Dispatchable>
                        }
                    } catch (t: Throwable) {
                        t.rethrowIfFatal()
                        exceptionHandler("onUnexpectedExceptionDuringRegistration") {
                            onUnexpectedExceptionDuringRegistration(listener, method, t)
                        }
                        null
                    }
                }

            if (defaultConfigOrCaptured == null) continue@out

            try {
                method.isAccessible = true
            } catch (_: InaccessibleObjectException) {
                exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                continue@out
            }

            // Wrap into RegisteredKFunctionListener and store
            val handler = RegisteredKFunctionListenerNormal(
                type = typedEventClass,
                listener = listener,
                kFunction = method,
                configuration = defaultConfigOrCaptured,
                resolvers = resolvers,
                manager = this,
            )
            getOrCreateHandlerBucket(typedEventClass).add(handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: SimpleConfiguration<*>) {
        checkClosed()
        try {
            val simpleListener = when (listener) {
                is SimpleListener<*> -> RegisteredSimpleListener(this, listener)
                is SimpleSuspendListener<*> -> RegisteredSimpleSuspendListener(this, listener)
            }
            val registered = simpleListener as RegisteredListener<Dispatchable>
            getOrCreateHandlerBucket(registered.type).add(registered)
        } catch (e: NoResolverException) {
            exceptionHandler("onParameterHasNoResolver") { onParameterHasNoResolver(listener, e.name, e.type) }
        }
    }

    override fun <D : Dispatchable> dispatch(event: D, config: DispatchConfig<D>) {
        checkClosed()
        // if we have no handlers there is no need for DeadEvent
        if (handlers.isEmpty()) return
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: emptyList()

        for ((registeredClass, bucket) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or bucket.call(event, genericTypes)
        }

        // If event is marked sticky, store latest instance
        if (config.getOrDefault(DispatchConfigKey.STICKY)) {
            stickyEvents[eventClass] = event
        }

        // No handlers found → maybe dispatch DeadEvent
        if (dispatchDeadEvents && config.getOrDefault(DispatchConfigKey.DISPATCH_DEAD_EVENT) && !called && eventClass != DeadEvent::class) {
            dispatch(DeadEvent(event))
        }
    }

    override suspend fun <D : Dispatchable> dispatchSuspend(event: D, config: DispatchConfig<D>) {
        checkClosed()
        // if we have no handlers there is no need for DeadEvent
        if (handlers.isEmpty()) return
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: emptyList()

        for ((registeredClass, bucket) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or bucket.callSuspend(event, genericTypes)
        }

        // If event is marked sticky, store latest instance
        if (config.getOrDefault(DispatchConfigKey.STICKY)) {
            stickyEvents[eventClass] = event
        }

        // No handlers found → maybe dispatch DeadEvent
        if (dispatchDeadEvents && config.getOrDefault(DispatchConfigKey.DISPATCH_DEAD_EVENT) && !called && eventClass != DeadEvent::class) {
            dispatchSuspend(DeadEvent(event))
        }
    }

    override fun clearStickyEvents() {
        checkClosed()
        stickyEvents.clear()
    }

    override fun removeStickyEvent(clazz: KClass<out Dispatchable>) {
        checkClosed()
        stickyEvents.remove(clazz)
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        val listener = RegisteredFunctionListener(
            type = event,
            method = handler,
            configuration = configuration,
            manager = this,
        )
        getOrCreateHandlerBucket(event).add(listener)
        // return RegisteredLambdaHandler so caller can unregister the handler
        return RegisteredLambdaHandler {
            @Suppress("UNCHECKED_CAST")
            (handlers[event] as? HandlerBucket<E>)?.remove(listener)
        }
    }

    override fun <E : Dispatchable> registerSuspend(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: suspend (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        val listener = RegisteredSuspendFunctionListener(
            type = event,
            configuration = configuration,
            manager = this,
            suspendMethod = handler,
        )
        getOrCreateHandlerBucket(event).add(listener)
        // return RegisteredLambdaHandler so caller can unregister the handler
        return RegisteredLambdaHandler {
            @Suppress("UNCHECKED_CAST")
            (handlers[event] as? HandlerBucket<E>)?.remove(listener)
        }
    }

    override fun unregister(listener: Listener) {
        checkClosed()
        handlers.values.forEach { it.remove(listener) }
    }

    override fun unregister(listener: SimpleConfiguration<*>) {
        checkClosed()
        handlers.values.forEach { it.remove(listener) }
    }

    override fun close() {
        if (isClosed) throw IllegalStateException("The event manager is already closed.")
        isClosed = true

        // cleanup resources
        handlers.forEach { it.value.close() }
        handlers.clear()
        stickyEvents.clear()

        // cancel scope
        scope.cancel()

        // remove unused handlers
        sharedExclusiveExecution.clear()
    }

    private fun checkClosed() {
        if (isClosed) throw IllegalStateException("The event manager is closed.")
    }

    private fun handleException(e: Throwable, listener: RegisteredListener<*>) {
        try {
            exceptionHandler.handle(
                e,
                (listener as? RegisteredKFunctionListener)?.listener,
                (listener as? RegisteredKFunctionListener)?.kFunction
            )
        } catch (handlerException: Throwable) {
            // rethrow it if it is a fatal exception
            handlerException.rethrowIfFatal()
            // so the exception handler has thrown an exception, we simply log it
            logger.log(
                Level.SEVERE,
                "Exception-Handler failed while handling an exception",
                handlerException
            )

            // describe the handler
            val listenerDescription = when (listener) {
                is RegisteredFunctionListener<*> -> "lambda: ${listener.method::class.jvmName}"
                is RegisteredKFunctionListener<*> -> "from: ${listener.listener::class.jvmName}#${listener.kFunction.name}"
                is RegisteredSuspendFunctionListener<*> -> "suspend lambda: ${listener.suspendMethod::class.jvmName}"
                is RegisteredSimpleListener<*> -> "simple listener: ${listener.simpleListener::class.jvmName}"
                is RegisteredSimpleSuspendListener<*> -> "simple suspend listener: ${listener.simpleListener::class.jvmName}"
            }

            logger.log(Level.SEVERE, "Original exception from ($listenerDescription) was:", e)
        }
    }

    @Suppress("WRONG_INVOCATION_KIND")
    @OptIn(ExperimentalContracts::class)
    private inline fun exceptionHandler(methodName: String, block: ExceptionHandler.() -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        try {
            exceptionHandler.block()
        } catch (e: Throwable) {
            // rethrow it if it is a fatal exception
            e.rethrowIfFatal()
            // so the exception handler has thrown an exception, we simply log it
            logger.log(
                Level.WARNING,
                "Method '$methodName' in ${exceptionHandler::class.jvmName} threw an exception.",
                e
            )
        }
    }

    private fun <E : Dispatchable> getOrCreateHandlerBucket(type: KClass<E>): HandlerBucket<E> {
        @Suppress("UNCHECKED_CAST")
        return handlers.computeIfAbsent(type) { HandlerBucket<E>() } as HandlerBucket<E>
    }

    private fun <D : Dispatchable> bindListener(
        listener: Listener,
        function: KFunction<*>,
        args: () -> Array<KClass<*>>,
    ): ListenerInvoker.CallHandler<D> {
        val reflection = ListenerInvoker.reflection()
        return if (invoker === reflection) reflection.bindListener(listener, function, args)
        else try {
            invoker.bindListener(listener, function, args)
        } catch (_: Exception) {
            reflection.bindListener(listener, function, args)
        }
    }

    companion object {
        /**
         * The logger for the DefaultEventManager.
         * It only logs [Level.SEVERE].
         *
         * If you want to disable it use:
         * ```
         * val logger = Logger.getLogger(DefaultEventManager::class.jvmName)
         * logger.level = Level.OFF
         * ```
         */
        private val logger = Logger.getLogger(DefaultEventManager::class.jvmName)
            .apply {
                level = Level.SEVERE
            }

        @Suppress("UNCHECKED_CAST")
        private fun <E : Dispatchable> ListenerParameterResolver<*>?.toStrategy(registered: RegisteredListener<E>): ArgStrategy<E> =
            when (this) {
                IsWaitingParameterResolver -> WaitingStrategy
                IsStickyParameterResolver -> StickyStrategy
                ConfigParameterResolver -> ConfigStrategy(registered.configuration)
                is ListenerParameterResolver<*> -> ResolverStrategy(
                    (registered as? RegisteredKFunctionListener)?.listener,
                    (registered as? RegisteredKFunctionListener)?.kFunction,
                    this
                )

                else -> NullStrategy
            } as ArgStrategy<E>
    }

    // -------------
    // HandlerBucket
    // -------------
    private inner class HandlerBucket<E : Dispatchable> {

        // Holder keeps both the unsorted (raw) list and an optional cached sorted view.
        // If sorted == null, it means the list is "dirty" and needs to be re-sorted on next access.
        private inner class Holder(
            val raw: List<RegisteredListener<E>>,
            val sorted: List<RegisteredListener<E>>? = null,
        )

        private val holder = AtomicReference(Holder(emptyList(), emptyList()))

        // ------------------------------
        // Add / Remove
        // ------------------------------

        fun add(listener: RegisteredListener<E>) {
            // Just append to raw list; mark sorted view as dirty (null).
            holder.getAndUpdate { h -> Holder(h.raw + listener) }
            // Immediately dispatch sticky events to the new listener.
            dispatchSticky(listener)
        }

        fun remove(listener: RegisteredListener<E>) {
            // Remove by identity (===) and mark sorted as dirty.
            holder.getAndUpdate { h -> Holder(h.raw.filterNot { it === listener }) }
        }

        fun remove(target: SimpleConfiguration<*>) {
            // Remove all listeners bound to this SimpleConfiguration.
            holder.getAndUpdate { h ->
                Holder(
                    h.raw.filterNot {
                        (it as? RegisteredSimpleListener<E>)?.simpleListener === target ||
                                (it as? RegisteredSimpleSuspendListener<E>)?.simpleListener === target
                    }
                )
            }
        }

        fun remove(target: Listener) {
            // Remove listeners that match the given Listener instance.
            holder.getAndUpdate { h ->
                Holder(h.raw.filterNot { (it as? RegisteredKFunctionListener<E>)?.listener === target })
            }
        }

        fun close() {
            // Clear all listeners (both raw and sorted).
            holder.set(Holder(emptyList(), emptyList()))
        }

        // ------------------------------
        // Internal: Lazy sorted access
        // ------------------------------

        private fun getOrderedListeners(): List<RegisteredListener<E>> {
            while (true) {
                val cur = holder.get()
                val cached = cur.sorted
                if (cached != null) return cached // Already sorted → return cached list.

                // Compute sorted view by priority (descending).
                val sorted = cur.raw.sortedByDescending { it.configuration.priority }
                val updated = Holder(cur.raw, sorted)

                // Try to update atomically. If CAS fails, another thread modified raw → retry.
                if (holder.compareAndSet(cur, updated)) return sorted
            }
        }

        // ------------------------------
        // Sticky Dispatch
        // ------------------------------

        private fun dispatchSticky(listener: RegisteredListener<E>) {
            if (listener.configuration.ignoreStickyEvents) return

            for ((type, event) in stickyEvents) {
                @Suppress("UNCHECKED_CAST")
                val typedEvent = event as E

                // Type must be compatible.
                if (!type.isSubclassOf(listener.type)) continue
                if (listener.configuration.disallowSubtypes && type != listener.type) continue

                // Call suspend or regular listener accordingly.
                if (listener.isSuspend) {
                    scope.launch(Dispatchers.Default) {
                        runCatching {
                            listener.invokeSuspend(typedEvent, isWaiting = false, isSticky = true)
                        }.onFailure { handleException(it, listener) }
                    }
                } else {
                    runCatching {
                        listener.invoke(typedEvent, true)
                    }.onFailure { handleException(it, listener) }
                }
            }
        }

        // ------------------------------
        // Lookup helpers
        // ------------------------------

        fun existListener(clazz: KClass<out Listener>): Boolean =
            holder.get().raw.any {
                (it as? RegisteredKFunctionListener<*>)?.listener?.let { l -> l::class == clazz } == true
            }

        @Suppress("UNCHECKED_CAST")
        fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
            // Only keep the *first* unique instance of a given class.
            val ordered = getOrderedListeners()
            val result = mutableListOf<RegisteredKFunctionListener<*>>()
            var firstMatch: Listener? = null

            for (registered in ordered) {
                val k = registered as? RegisteredKFunctionListener<E> ?: continue
                if (k.listener::class != clazz) continue
                if (firstMatch == null) {
                    firstMatch = k.listener
                    result += k
                } else if (k.listener === firstMatch) {
                    // If the exact same listener instance is duplicated, keep it.
                    result += k
                }
            }
            return result
        }

        // ------------------------------
        // Dispatch: Sync & Suspend
        // ------------------------------

        fun call(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            val list = getOrderedListeners()
            if (list.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            var called = false

            for (handler in list) {
                // Subtype restrictions.
                if (handler.configuration.disallowSubtypes && typedEvent::class != handler.type) continue
                // Generic type restrictions.
                if (typedEvent::class == handler.type &&
                    genericTypes.isNotEmpty() &&
                    handler is RegisteredKFunctionListener<E> &&
                    !handler.allowGenericTypes(genericTypes)
                ) continue

                val silent = handler.configuration.silent
                if (handler.isSuspend) {
                    val failed = AtomicBoolean(false) // Flag to track if the suspend handler failed

                    // Run suspend handlers asynchronously in a coroutine.
                    // Dispatchers.Unconfined is used so the handler can modify the event immediately
                    // before the dispatch continues or completes.
                    // Additionally: because of Dispatchers.Unconfined, we can still access the return value
                    // before dispatch finishes. The handler returns false only if exclusiveListenerProcessing = true
                    // and it is already running, meaning execution was refused.
                    // Important: if exclusiveListenerProcessing is fine and the handler reaches a suspend point,
                    // the scope.launch continues → we know the handler has started successfully.
                    scope.launch(Dispatchers.Unconfined) {
                        runCatching {
                            // Call the suspend handler
                            val success = handler.invokeSuspend(typedEvent, isWaiting = false, isSticky = false)
                            if (!success) failed.set(true) // Mark as failed/refused if handler returned false
                        }.onFailure {
                            // If an exception occurs, handle it and mark as failed
                            handleException(it, handler)
                            failed.set(true)
                        }
                    }

                    // If not in silent mode and no failure/refusal detected, mark handler as called
                    if (!silent && !failed.get()) called = true
                } else {
                    try {
                        // Call normal (non-suspend) handler directly
                        // Returns false only if exclusiveListenerProcessing = true and handler is already running.
                        if (handler(typedEvent, false) && !silent) called = true
                    } catch (e: Throwable) {
                        // Handle any exception thrown by the handler
                        handleException(e, handler)
                    }
                }

            }
            return called
        }

        suspend fun callSuspend(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            val list = getOrderedListeners()
            if (list.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            var called = false

            for (handler in list) {
                if (handler.configuration.disallowSubtypes && typedEvent::class != handler.type) continue
                if (typedEvent::class == handler.type &&
                    genericTypes.isNotEmpty() &&
                    handler is RegisteredKFunctionListener<E> &&
                    !handler.allowGenericTypes(genericTypes)
                ) continue

                val silent = handler.configuration.silent
                try {
                    val success = if (handler.isSuspend) {
                        handler.invokeSuspend(typedEvent, isWaiting = true, isSticky = false)
                    } else {
                        handler.invoke(typedEvent, false)
                    }
                    if (success && !silent) called = true
                } catch (e: Throwable) {
                    handleException(e, handler)
                }
            }
            return called
        }
    }

    // --------------------
    // Registered Listeners
    // --------------------
    private sealed class RegisteredListener<E : Dispatchable>(
        val type: KClass<E>,
        val configuration: EventConfiguration<E>,
        val manager: DefaultEventManager,
    ) {
        /** Whether this listener is a suspend (asynchronous) listener */
        open val isSuspend: Boolean = false

        /** Unique identifier for the listener (used for exclusive execution) */
        abstract val handlerId: String

        /** Operator function to invoke the listener for non-suspend events */
        operator fun invoke(event: E, isSticky: Boolean): Boolean {
            if (isSuspend) throw UnsupportedOperationException(
                "invoke is not supported for suspend functions."
            )

            // If exclusive execution is enabled, try to acquire the lock
            if (configuration.exclusiveListenerProcessing) {
                if (!manager.sharedExclusiveExecution.tryAcquire(handlerId)) return false
            }

            try {
                // Call the listener's internal implementation
                invokeInternal(event, isSticky)
            } finally {
                // Always release the exclusive lock
                manager.sharedExclusiveExecution.release(handlerId)
            }
            return true
        }

        /** Suspend version of invoke, for asynchronous listeners */
        suspend fun invokeSuspend(event: E, isWaiting: Boolean, isSticky: Boolean): Boolean {
            // If exclusive execution is enabled, try to acquire the lock
            if (configuration.exclusiveListenerProcessing) {
                if (!manager.sharedExclusiveExecution.tryAcquire(handlerId)) return false
            }

            try {
                // Call the suspend internal implementation
                invokeSuspendInternal(event, isWaiting, isSticky)
            } finally {
                // Always release the exclusive lock
                manager.sharedExclusiveExecution.release(handlerId)
            }
            return true
        }

        /** Default suspend internal method (must be overridden if isSuspend == true) */
        @Throws(UnsupportedOperationException::class)
        protected open suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean, isSticky: Boolean) {
            throw UnsupportedOperationException(
                this::class.jvmName +
                        if (isSuspend) " has not overridden invokeSuspendInternal"
                        else " does not support suspend invocation"
            )
        }

        /** Non-suspend internal invocation — must be implemented by subclasses */
        protected abstract fun invokeInternal(event: E, isSticky: Boolean)
    }

    private class RegisteredFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        val method: (E) -> Unit,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, configuration, manager) {
        override val handlerId: String = "RegisteredFunctionListener@${type.jvmName}@${method.hashCode()}"

        override fun invokeInternal(event: E, isSticky: Boolean) {
            // delegate to method
            method(event)
        }
    }

    private class RegisteredSuspendFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
        val suspendMethod: suspend (E) -> Unit,
    ) : RegisteredListener<E>(type, configuration, manager) {
        override val isSuspend: Boolean = true
        override val handlerId: String = "RegisteredSuspendFunctionListener@${type.jvmName}@${suspendMethod.hashCode()}"

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean, isSticky: Boolean) {
            // delegate to method
            suspendMethod(event)
        }

        override fun invokeInternal(event: E, isSticky: Boolean) {
            throw UnsupportedOperationException("${this::class.jvmName} does not support none suspend listeners.")
        }
    }

    private sealed class RegisteredKFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        val listener: Listener,
        val kFunction: KFunction<*>,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, configuration, manager) {
        protected abstract val argTypes: Array<KClass<*>>

        protected val handler: ListenerInvoker.CallHandler<E> by lazy {
            manager.bindListener(listener, kFunction, ::argTypes)
        }

        protected abstract val actualType: KType

        protected val typeArguments by lazy { actualType.arguments }

        protected val hasTypeArguments: Boolean by lazy { typeArguments.any { it.type != null } }


        protected abstract fun buildArgs(event: E, isWaiting: Boolean, isSticky: Boolean): Array<Any?>

        override fun invokeInternal(event: E, isSticky: Boolean) {
            val args = buildArgs(event, true, isSticky)
            handler.invoke(event, args)
        }

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean, isSticky: Boolean) {
            val args = buildArgs(event = event, isWaiting = isWaiting, isSticky = isSticky)
            handler.invokeSuspend(event, args)
        }

        fun allowGenericTypes(types: List<KClass<*>>): Boolean {
            if (!hasTypeArguments) return true // If no generics, always allow
            if (typeArguments.size != types.size) return false // Must have matching number of type arguments

            return typeArguments.mapIndexed { index, projection ->
                if (projection.variance == null) return@mapIndexed true
                val type = projection.type?.classifier as? KClass<*> ?: return@mapIndexed false
                @Suppress("SENSELESS_NULL_IN_WHEN") // Suppress false-positive inspection
                when (projection.variance) {
                    // Invariant: Must match exactly
                    KVariance.INVARIANT -> type == types[index]
                    // "in" variance: provided type must be a superclass
                    KVariance.IN -> types[index].isSuperclassOf(type)
                    // "out" variance: provided type must be a subclass
                    KVariance.OUT -> types[index].isSubclassOf(type)
                    // "*" no variance specified: allow
                    null -> true
                }
            }.all() // All type argument checks must pass
        }

        @Suppress("UNCHECKED_CAST")
        abstract val extraStrategies: Array<ArgStrategy<E>>

        abstract fun withNewListener(newListener: Listener): RegisteredKFunctionListener<E>
    }

    @OptIn(InternalProcessorApi::class)
    private class RegisteredKFunctionListenerProcessor<E : Dispatchable>(
        type: KClass<E>,
        listener: Listener,
        kFunction: KFunction<*>,
        configuration: EventConfiguration<E>,
        val resolvers: List<ListenerParameterResolver<*>>,
        val handlerDefinition: HandlerDefinition,
        manager: DefaultEventManager,
    ) : RegisteredKFunctionListener<E>(type, listener, kFunction, configuration, manager) {
        override val argTypes: Array<KClass<*>> by lazy {
            Array(2 + handlerDefinition.args.size) { index ->
                when (index) {
                    0 -> listener::class
                    1 -> type
                    else -> handlerDefinition.args[index - 2].type
                }
            }
        }
        override val actualType: KType by lazy {
            kFunction.parameters[1].type
        }

        override fun buildArgs(
            event: E,
            isWaiting: Boolean,
            isSticky: Boolean,
        ): Array<Any?> = Array(extraStrategies.size) { index ->
            extraStrategies[index].resolve(event, isWaiting, isSticky)
        }

        override val extraStrategies: Array<ArgStrategy<E>> by lazy {
            handlerDefinition.args
                .mapIndexed { index, _ ->
                    resolvers[index].toStrategy(this) // Create resolution strategy for each extra parameter
                }
                .toTypedArray()
        }

        override fun withNewListener(newListener: Listener) = RegisteredKFunctionListenerProcessor(
            type, newListener, kFunction, configuration, resolvers, handlerDefinition, manager
        )

        override val handlerId: String =
            "RegisteredKFunctionListenerProcessor@${type.jvmName}@${listener::class.jvmName}#${kFunction.hashCode()}"
    }

    private class RegisteredKFunctionListenerNormal<E : Dispatchable>(
        type: KClass<E>,
        listener: Listener,
        kFunction: KFunction<*>,
        configuration: EventConfiguration<E>,
        val resolvers: Map<KParameter, ListenerParameterResolver<*>>,
        manager: DefaultEventManager,
    ) : RegisteredKFunctionListener<E>(type, listener, kFunction, configuration, manager) {

        /** Reference to the event parameter (2nd parameter of the function: listener, event, ...) */
        val eventParameter = kFunction.parameters[1]

        override val actualType: KType = eventParameter.type

        /** Unique identifier for the handler (used for exclusive processing) */
        override val handlerId: String =
            "RegisteredKFunctionListenerNormal@${type.jvmName}@${listener::class.jvmName}#${kFunction.hashCode()}"

        override val argTypes: Array<KClass<*>> by lazy {
            kFunction.parameters // Skip first two (listener, event)
                .mapIndexed { index, param ->
                    when (index) {
                        0 -> listener::class
                        1 -> type
                        else -> resolvers[param]?.type ?: throw IllegalArgumentException(
                            "Parameter '${param.name}' of function '${kFunction.name}' in ${listener::class.jvmName} " +
                                    "has no resolver. Please specify a resolver using @ListenerParameterResolver."
                        )
                    }
                }
                .toTypedArray()
        }

        @Suppress("UNCHECKED_CAST")
        /** Strategies for resolving additional parameters beyond the listener and event */
        override val extraStrategies: Array<ArgStrategy<E>> by lazy {
            kFunction.parameters.drop(2) // Skip first two (listener, event)
                .map { param ->
                    resolvers[param].toStrategy(this) // Create resolution strategy for each extra parameter
                }
                .toTypedArray()
        }

        override fun withNewListener(newListener: Listener) = RegisteredKFunctionListenerNormal(
            type, newListener, kFunction, configuration, resolvers, manager
        )

        /** Builds the full argument array for the function call */
        override fun buildArgs(event: E, isWaiting: Boolean, isSticky: Boolean): Array<Any?> =
            Array(extraStrategies.size) { index ->
                extraStrategies[index].resolve(event, isWaiting, isSticky)
            }

    }

    @Suppress("UNCHECKED_CAST")
    private class RegisteredSimpleListener<E : Dispatchable>(
        manager: DefaultEventManager,
        val simpleListener: SimpleListener<E>,
    ) : RegisteredListener<E>(
        simpleListener.type ?: simpleListener::handle.parameters[1].type.classifier as KClass<E>,
        simpleListener.configuration(),
        manager
    ) {
        private val args = simpleListener.args()
        private val resolvers: Map<String, ArgStrategy<E>> = args.map { arg ->
            arg.key to (manager.parameterResolver
                .find { it.name == arg.key && it.type == arg.value }
                ?: throw NoResolverException(arg.key, arg.value)
                    ).toStrategy(this)
        }.toMap()

        override fun invokeInternal(event: E, isSticky: Boolean) {
            simpleListener.handleArgs(event, resolvers.mapValues { it.value.resolve(event, true, isSticky) })
        }

        override val handlerId: String = "RegisteredSimpleListener@${type.jvmName}@${simpleListener.hashCode()}"
    }

    @Suppress("UNCHECKED_CAST")
    private class RegisteredSimpleSuspendListener<E : Dispatchable>(
        manager: DefaultEventManager,
        val simpleListener: SimpleSuspendListener<E>,
    ) : RegisteredListener<E>(
        simpleListener.type ?: simpleListener::handle.parameters[1].type.classifier as KClass<E>,
        simpleListener.configuration(),
        manager
    ) {
        private val args = simpleListener.args()
        private val resolvers: Map<String, ArgStrategy<E>> = args.map { arg ->
            arg.key to (manager.parameterResolver
                .find { it.name == arg.key && it.type == arg.value }
                ?: throw NoResolverException(arg.key, arg.value)
                    ).toStrategy(this)
        }.toMap()

        override fun invokeInternal(event: E, isSticky: Boolean) {
            throw UnsupportedOperationException("${this::class.jvmName} does not support none suspend listeners.")
        }

        override val handlerId: String = "RegisteredSimpleSuspendListener@${type.jvmName}@${simpleListener.hashCode()}"

        override val isSuspend: Boolean = true

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean, isSticky: Boolean) {
            simpleListener.handleArgs(event, resolvers.mapValues { it.value.resolve(event, isWaiting, isSticky) })
        }
    }

    // -----------
    // ArgStrategy
    // -----------
    private sealed interface ArgStrategy<E : Dispatchable> {
        fun resolve(event: E, isWaiting: Boolean, isSticky: Boolean): Any?
    }

    private object WaitingStrategy : ArgStrategy<Dispatchable> {
        override fun resolve(event: Dispatchable, isWaiting: Boolean, isSticky: Boolean) = isWaiting
    }

    private object StickyStrategy : ArgStrategy<Dispatchable> {
        override fun resolve(event: Dispatchable, isWaiting: Boolean, isSticky: Boolean) = isSticky
    }

    private class ConfigStrategy<E : Dispatchable>(
        private val configuration: EventConfiguration<E>,
    ) : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean, isSticky: Boolean) = configuration
    }

    private class ResolverStrategy<E : Dispatchable>(
        private val listener: Listener?,
        private val kFunction: KFunction<*>?,
        private val resolver: ListenerParameterResolver<*>,
    ) : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean, isSticky: Boolean): Any =
            resolver.resolve(listener, kFunction, event)
    }

    private object NullStrategy : ArgStrategy<Dispatchable> {
        override fun resolve(event: Dispatchable, isWaiting: Boolean, isSticky: Boolean) = null
    }

    // -------------------------
    // InternalParameterResolver
    // -------------------------
    /** Marker interface for parameter resolvers that behave like ListenerParameterResolver<T>
     * but provide access to special internal values that are NOT available through the normal
     * resolve() mechanism.
     *
     * These resolvers never resolve their values directly via resolve(). Instead, when a parameter
     * needs to be injected into a listener method, the event manager checks:
     *   1. If the resolver is an InternalParameterResolver
     *   2. If yes, it uses a `when` statement to match the specific implementation
     *      (e.g., [IsWaitingParameterResolver], [ConfigParameterResolver], etc.)
     *   3. It inserts the requested value
     *
     * If resolve() were called directly on an InternalParameterResolver, that would mean
     * something went wrong in the parameter resolution process — so it throws an exception.
     */
    private sealed interface InternalParameterResolver<T : Any> : ListenerParameterResolver<T> {
        override fun resolve(listener: Listener?, methode: KFunction<*>?, event: Dispatchable): T {
            throw IllegalStateException(
                "InternalParameterResolver#resolve was called for ${this::class.qualifiedName ?: this::class.jvmName}, " +
                        "this is a marker and should never be resolved directly."
            )
        }
    }

    private data object IsWaitingParameterResolver : InternalParameterResolver<Boolean> {
        override val name: String = "isWaiting"
        override val type = Boolean::class
        override val valueByConfiguration: Boolean = true
    }

    private data object ConfigParameterResolver : InternalParameterResolver<EventConfiguration<*>> {
        override val name: String = "config"
        override val type = EventConfiguration::class
        override val valueByConfiguration: EventConfiguration<*> = EventConfiguration
    }

    private data object IsStickyParameterResolver : InternalParameterResolver<Boolean> {
        override val name: String = "isSticky"
        override val type = Boolean::class
        override val valueByConfiguration: Boolean = false
    }

    // -----
    // Utils
    // -----
    private class NoResolverException(val name: String, val type: KClass<*>) :
        IllegalArgumentException("No Resolver with the name `$name` and the type `${type.jvmName}` was found.")
}