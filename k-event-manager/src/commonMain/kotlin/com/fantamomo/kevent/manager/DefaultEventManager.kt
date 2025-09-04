package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.*
import com.fantamomo.kevent.manager.settings.Settings
import com.fantamomo.kevent.manager.settings.getSetting
import com.fantamomo.kevent.utils.InjectionName
import kotlinx.coroutines.*
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * Default implementation of the EventManager interface for managing event listeners
 * and dispatching events.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class DefaultEventManager internal constructor(
    components: EventManagerComponent<*>,
) : EventManager {

    private val handlers: ConcurrentHashMap<KClass<out Dispatchable>, HandlerBucket<out Dispatchable>> =
        ConcurrentHashMap()

    private val exceptionHandler = components.getOrThrow(ExceptionHandler)

    private val parameterResolver: List<ListenerParameterResolver<*>>

    private val sharedExclusiveExecution = components.getOrThrow(SharedExclusiveExecution)

    private val scope: CoroutineScope =
        components[EventCoroutineScope]?.scope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isClosed = false

    private val dispatchDeadEvents = components.getSetting(Settings.DISPATCH_DEAD_EVENTS)

    init {
        var comps = components

        if (!comps.getSetting(Settings.DISABLE_EVENTMANAGER_INJECTION)) comps += ListenerParameterResolver.static(
            "manager",
            EventManager::class,
            this
        )

        if (!comps.getSetting(Settings.DISABLE_LOGGER_INJECTION)) ListenerParameterResolver.static(
            "logger",
            Logger::class,
            logger
        )
        if (!comps.getSetting(Settings.DISABLE_SCOPE_INJECTION)) ListenerParameterResolver.static(
            "scope",
            CoroutineScope::class,
            CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))
        )

        if (!comps.getSetting(Settings.DISABLE_IS_WAITING_INJECTION)) comps += IsWaitingParameterResolver

        if (!comps.getSetting(Settings.DISABLE_CONFIG_INJECTION)) comps += ConfigParameterResolver

        parameterResolver = comps.getAll(ListenerParameterResolver.Key)
    }

    private fun existListener(clazz: KClass<out Listener>) = handlers.values.any { it.existListener(clazz) }

    private fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
        return handlers.values.flatMap { it.findAllListeners(clazz) }
    }

    private fun registerRegisteredListener(listenerClass: KClass<out Listener>, listener: Listener) {
        val listeners = findAllListeners(listenerClass)
        out@ for (registered in listeners) {
            @Suppress("UNCHECKED_CAST")
            registered as RegisteredKFunctionListener<Dispatchable>
            var newConfiguration: EventConfiguration<Dispatchable>? = null

            if (!registered.kFunction.parameters[1].type.isMarkedNullable) {
                newConfiguration = EventConfiguration.default()
            } else {
                val arguments = mapOf(
                    registered.thisParameter to listener,
                    registered.eventParameter to null
                ) + registered.resolvers.mapValues { it.value.valueByConfiguration }
                val method = registered.kFunction
                try {
                    if (method.isSuspend) {
                        var exception: InvocationTargetException? = null
                        runBlocking {
                            withTimeout(2.milliseconds) {
                                try {
                                    method.callSuspendBy(arguments)
                                } catch (e: InvocationTargetException) {
                                    exception = e
                                }
                            }
                        }
                        if (exception != null) throw exception
                    } else {
                        method.callBy(arguments)
                    }
                } catch (e: InvocationTargetException) {
                    val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                    if (config !is EventConfiguration<*>) continue@out
                    @Suppress("UNCHECKED_CAST")
                    newConfiguration = config as EventConfiguration<Dispatchable>
                } catch (e: Throwable) {
                    exceptionHandler("onUnexpectedExceptionDuringRegistration") { onUnexpectedExceptionDuringRegistration(listener, method, e) }
                    continue@out
                }
            }
            if (newConfiguration == null) continue@out

            val new = RegisteredKFunctionListener(
                registered.type,
                listener,
                registered.kFunction,
                newConfiguration,
                registered.resolvers,
                this
            )
            getOrCreateHandlerBucket(registered.type).add(new)
        }
    }

    override fun register(listener: Listener) {
        checkClosed()
        val listenerClass = listener::class

        if (existListener(listenerClass)) {
            registerRegisteredListener(listenerClass, listener)
            return
        }

        out@ for (method in listenerClass.declaredMemberFunctions) {
            if (!method.hasAnnotation<Register>()) continue
            if (method.visibility != KVisibility.PUBLIC) {
                exceptionHandler("onMethodNotPublic") { onMethodNotPublic(listener, method, method.visibility) }
                continue@out
            }

            val parameters = method.parameters
            if (parameters.size < 2) {
                exceptionHandler("onMethodHasNoParameters") { onMethodHasNoParameters(listener, method) }
                continue@out
            }

            val resolvers = parameters.dropWhile { it.index < 2 }.associateWith { parameter ->
                parameterResolver.find {
                    it.name == (parameter.findAnnotation<InjectionName>()?.value
                        ?: parameter.name) && it.type == parameter.type.classifier
                } ?: continue@out
            }

            val eventClass = parameters[1].type.classifier as? KClass<*> ?: continue
            if (!Dispatchable::class.isSuperclassOf(eventClass)) {
                exceptionHandler("onMethodHasNoDispatchableParameter") { onMethodHasNoDispatchableParameter(listener, method, parameters[1].type) }
                continue@out
            }

            @Suppress("UNCHECKED_CAST")
            val typedEventClass = eventClass as KClass<Dispatchable>

            val arguments = mapOf(
                parameters[0] to listener,
                parameters[1] to null
            ) + resolvers.mapValues { it.value.valueByConfiguration }

            val defaultConfigOrCaptured: EventConfiguration<Dispatchable>? =
                if (!parameters[1].type.isMarkedNullable) {
                    EventConfiguration.default()
                } else {
                    try {
                        method.isAccessible = true
                        if (method.isSuspend) {
                            var exception: InvocationTargetException? = null
                            runBlocking {
                                withTimeout(2.milliseconds) {
                                    try {
                                        method.callSuspendBy(arguments)
                                        exceptionHandler("onMethodDidNotThrowConfiguredException") { onMethodDidNotThrowConfiguredException(listener, method) }
                                    } catch (e: InvocationTargetException) {
                                        exception = e
                                    }
                                }
                            }
                            if (exception != null) throw exception
                        } else {
                            method.callBy(arguments)
                            exceptionHandler("onMethodDidNotThrowConfiguredException") { onMethodDidNotThrowConfiguredException(listener, method) }
                        }
                        null
                    } catch (e: InvocationTargetException) {
                        val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                        if (config !is EventConfiguration<*>) {
                            exceptionHandler("onMethodThrewUnexpectedException") { onMethodThrewUnexpectedException(listener, method, e.targetException) }
                            null
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            config as EventConfiguration<Dispatchable>
                        }
                    } catch (t: Throwable) {
                        exceptionHandler("onUnexpectedExceptionDuringRegistration") { onUnexpectedExceptionDuringRegistration(listener, method, t) }
                        null
                    }
                }

            if (defaultConfigOrCaptured == null) continue@out

            try {
                method.isAccessible = true
            } catch (_: Throwable) {
                exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                continue@out
            }

            val handler = RegisteredKFunctionListener(
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

    override fun dispatch(event: Dispatchable) {
        checkClosed()
        if (handlers.isEmpty()) return
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: emptyList()

        for ((registeredClass, bucket) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or bucket.call(event, genericTypes)
        }

        if (dispatchDeadEvents && !called && eventClass != DeadEvent::class) {
            dispatch(DeadEvent(event))
        }
    }

    override suspend fun dispatchSuspend(event: Dispatchable) {
        checkClosed()
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: emptyList()

        for ((registeredClass, bucket) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or bucket.callSuspend(event, genericTypes)
        }

        if (dispatchDeadEvents && !called && eventClass != DeadEvent::class) {
            dispatchSuspend(DeadEvent(event))
        }
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        val listener = RegisteredFunctionListener(
            type = event,
            listener = null,
            method = handler,
            configuration = configuration,
            manager = this,
        )
        getOrCreateHandlerBucket(event).add(listener)
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
            listener = null,
            configuration = configuration,
            manager = this,
            suspendMethod = handler,
        )
        getOrCreateHandlerBucket(event).add(listener)
        return RegisteredLambdaHandler {
            @Suppress("UNCHECKED_CAST")
            (handlers[event] as? HandlerBucket<E>)?.remove(listener)
        }
    }

    override fun unregister(listener: Listener) {
        checkClosed()
        handlers.values.forEach { it.remove(listener) }
    }

    override fun close() {
        checkClosed()
        isClosed = true
        handlers.forEach { it.value.close() }
        handlers.clear()
        scope.cancel()
        sharedExclusiveExecution.clear()
    }

    private fun checkClosed() {
        if (isClosed) throw IllegalStateException("The event manager is already closed.")
    }

    private fun handleException(e: Throwable, listener: RegisteredListener<*>) {
        try {
            exceptionHandler.handle(
                e,
                listener.listener,
                (listener as? RegisteredKFunctionListener<*>)?.kFunction
            )
        } catch (handlerException: Throwable) {
            logger.log(
                Level.SEVERE,
                "Exception-Handler failed while handling an exception",
                handlerException
            )

            val listenerDescription = when (listener) {
                is RegisteredFunctionListener<*> -> "lambda: ${listener.method::class.jvmName}"
                is RegisteredKFunctionListener<*> -> "from: ${listener.listener::class.jvmName}#${listener.kFunction.name}"
                is RegisteredSuspendFunctionListener<*> -> "suspend lambda: ${listener.suspendMethod::class.jvmName}"
            }

            logger.log(Level.SEVERE, "Original exception was ($listenerDescription):", e)
        }
    }

    @Suppress("WRONG_INVOCATION_KIND")
    @OptIn(ExperimentalContracts::class)
    private inline fun exceptionHandler(methodName: String, block: ExceptionHandler.() -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        try {
            exceptionHandler.block()
        } catch (e: Throwable) {
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

    private inner class HandlerBucket<E : Dispatchable> {
        private val snapshot: AtomicReference<List<RegisteredListener<E>>> = AtomicReference(emptyList())

        fun add(listener: RegisteredListener<E>) {
            while (true) {
                val cur = snapshot.get()
                val next = (cur + listener).sortedByDescending { it.configuration.getOrDefault(Key.PRIORITY) }
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        fun remove(listener: RegisteredFunctionListener<E>) = removeByIdentity(listener)
        fun remove(listener: RegisteredSuspendFunctionListener<E>) = removeByIdentity(listener)

        fun remove(target: Listener) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { it.listener === target }
                if (next === cur) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        private fun removeByIdentity(target: RegisteredListener<E>) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { it === target }
                if (next === cur) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        fun existListener(clazz: KClass<out Listener>) = snapshot.get().any { it.listener?.let { l -> l::class == clazz } ?: false }

        @Suppress("UNCHECKED_CAST")
        fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
            var firstMatch: Listener? = null
            return snapshot.get().filter { registered ->
                registered.listener?.let {
                    if (it::class != clazz) return@filter true
                    if (firstMatch == null) firstMatch = it
                    it === firstMatch
                } ?: false
            } as List<RegisteredKFunctionListener<*>>
        }

        fun call(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            val list = snapshot.get()
            if (list.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            var called = false
            for (handler in list) {
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                    if (typedEvent::class != handler.type) continue
                }
                if (event::class == handler.type && genericTypes.isNotEmpty() && handler is RegisteredKFunctionListener<E> && !handler.allowGenericTypes(
                        genericTypes
                    )
                ) continue

                val silent = handler.configuration.getOrDefault(Key.SILENT)
                if (handler.isSuspend) {
                    scope.launch(Dispatchers.Unconfined) {
                        try {
                            handler.invokeSuspend(typedEvent, false)
                        } catch (e: Throwable) {
                            handleException(e, handler)
                        }
                    }
                    if (!silent) called = true
                } else {
                    try {
                        if (handler(typedEvent) && !silent) called = true
                    } catch (e: Throwable) {
                        handleException(e, handler)
                    }
                }
            }
            return called
        }

        suspend fun callSuspend(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            val list = snapshot.get()
            if (list.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            var called = false
            for (handler in list) {
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                    if (typedEvent::class != handler.type) continue
                }
                if (event::class == handler.type && genericTypes.isNotEmpty() && handler is RegisteredKFunctionListener<E> && !handler.allowGenericTypes(
                        genericTypes
                    )
                ) continue

                val silent = handler.configuration.getOrDefault(Key.SILENT)
                try {
                    val success = if (handler.isSuspend) {
                        handler.invokeSuspend(typedEvent, true)
                    } else {
                        handler.invoke(typedEvent)
                    }
                    if (success && !silent) called = true
                } catch (e: Throwable) {
                    handleException(e, handler)
                }
            }
            return called
        }

        fun close() { snapshot.set(emptyList()) }
    }

    private sealed class RegisteredListener<E : Dispatchable>(
        val type: KClass<E>,
        open val listener: Listener?,
        val configuration: EventConfiguration<E>,
        val manager: DefaultEventManager,
    ) {
        open val isSuspend: Boolean = false
        abstract val method: (E) -> Unit
        abstract val handlerId: String

        operator fun invoke(event: E): Boolean {
            if (isSuspend) throw UnsupportedOperationException("invoke is not supported for suspend functions.")
            if (configuration.getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)) {
                if (!manager.sharedExclusiveExecution.tryAcquire(handlerId)) return false
            }
            try {
                method(event)
            } finally {
                manager.sharedExclusiveExecution.release(handlerId)
            }
            return true
        }

        suspend fun invokeSuspend(event: E, isWaiting: Boolean): Boolean {
            if (configuration.getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)) {
                if (!manager.sharedExclusiveExecution.tryAcquire(handlerId)) return false
            }
            try {
                invokeSuspendInternal(event, isWaiting)
            } finally {
                manager.sharedExclusiveExecution.release(handlerId)
            }
            return true
        }

        protected open suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            throw UnsupportedOperationException("${this::class.jvmName} does not support suspend functions.")
        }
    }

    private class RegisteredFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        listener: Listener?,
        override val method: (E) -> Unit,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, listener, configuration, manager) {
        override val handlerId: String = "RegisteredFunctionListener@${type.jvmName}@${method.hashCode()}"
    }

    private class RegisteredSuspendFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        listener: Listener?,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
        val suspendMethod: suspend (E) -> Unit,
        override val method: (E) -> Unit = { error("Use RegisteredSuspendFunctionListener#suspendMethod instead") },
    ) : RegisteredListener<E>(type, listener, configuration, manager) {
        override val isSuspend: Boolean = true
        override val handlerId: String = "RegisteredSuspendFunctionListener@${type.jvmName}@${suspendMethod.hashCode()}"

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            suspendMethod(event)
        }
    }

    private class RegisteredKFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        override val listener: Listener,
        val kFunction: KFunction<*>,
        configuration: EventConfiguration<E>,
        val resolvers: Map<KParameter, ListenerParameterResolver<*>>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, listener, configuration, manager) {
        val thisParameter = kFunction.parameters[0]
        val eventParameter = kFunction.parameters[1]
        private val actualType = eventParameter.type
        private val typeArguments by lazy { actualType.arguments }
        private val hasTypeArguments: Boolean by lazy { typeArguments.any { it.type != null } }
        override val isSuspend: Boolean = kFunction.isSuspend
        override val handlerId: String = "RegisteredKFunctionListener@${type.jvmName}@${listener::class.jvmName}#${kFunction.hashCode()}"

        override val method: (E) -> Unit = { evt ->
            val args = buildArgs(evt, false)
            kFunction.callBy(args)
        }

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            val args = buildArgs(event, isWaiting)
            kFunction.callSuspendBy(args)
        }

        private fun buildArgs(event: E, isWaiting: Boolean): Map<KParameter, Any?> {
            return buildMap(resolvers.size + 2) {
                put(thisParameter, listener)
                put(eventParameter, event)
                for ((param, resolver) in resolvers) {
                    put(param,
                        if (resolver is InternalParameterResolver<*>) {
                            when (resolver) {
                                IsWaitingParameterResolver -> isWaiting
                                ConfigParameterResolver -> configuration
                            }
                        } else {
                            resolver.resolve(listener, kFunction, event)
                        }
                    )
                }
            }
        }

        fun allowGenericTypes(types: List<KClass<*>>): Boolean {
            if (!hasTypeArguments) return true
            if (typeArguments.size != types.size) return false
            return typeArguments.mapIndexed { index, projection ->
                if (projection.variance == null) return@mapIndexed true
                val type = projection.type?.classifier as? KClass<*> ?: return@mapIndexed false
                when (projection.variance) {
                    KVariance.INVARIANT -> type == types[index]
                    KVariance.IN -> types[index].isSuperclassOf(type)
                    KVariance.OUT -> types[index].isSubclassOf(type)
                    null -> true
                }
            }.all { it }
        }
    }

    companion object {
        private val logger = Logger.getLogger(DefaultEventManager::class.jvmName)
    }

    private sealed interface InternalParameterResolver<T : Any> : ListenerParameterResolver<T> {
        override fun resolve(listener: Listener?, methode: KFunction<*>?, event: Dispatchable): T {
            throw IllegalStateException("This method should not be called.")
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
        override val valueByConfiguration: EventConfiguration<*> = EventConfiguration.DEFAULT
    }
}