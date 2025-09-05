package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.*
import com.fantamomo.kevent.manager.internal.all
import com.fantamomo.kevent.manager.internal.rethrowIfFatal
import com.fantamomo.kevent.manager.settings.Settings
import com.fantamomo.kevent.manager.settings.getSetting
import com.fantamomo.kevent.utils.InjectionName
import kotlinx.coroutines.*
import java.lang.reflect.InaccessibleObjectException
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
                val args = registered.kFunction.parameters.map { param ->
                    when (param.index) {
                        0 -> listener
                        1 -> null
                        else -> {
                            val resolver = registered.resolvers[param]
                            resolver?.valueByConfiguration
                        }
                    }
                }
                val method = registered.kFunction
                try {
                    if (method.isSuspend) {
                        var exception: InvocationTargetException? = null
                        runBlocking {
                            withTimeout(2.milliseconds) {
                                try {
                                    method.callSuspend(*args.toTypedArray())
                                } catch (e: InvocationTargetException) {
                                    exception = e
                                }
                            }
                        }
                        if (exception != null) throw exception
                    } else {
                        method.call(*args.toTypedArray())
                    }
                } catch (e: InvocationTargetException) {
                    val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                    if (config !is EventConfiguration<*>) continue@out
                    @Suppress("UNCHECKED_CAST")
                    newConfiguration = config as EventConfiguration<Dispatchable>
                } catch (e: Throwable) {
                    e.rethrowIfFatal()
                    exceptionHandler("onUnexpectedExceptionDuringRegistration") {
                        onUnexpectedExceptionDuringRegistration(
                            listener,
                            method,
                            e
                        )
                    }
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
                val name = (parameter.findAnnotation<InjectionName>()?.value
                    ?: parameter.name)
                parameterResolver.find {
                    it.name == name && it.type == parameter.type.classifier
                } ?: run {
                    if (name == null) {
                        logger.severe(
                            "The name of a Parameter which should have a name, has none. " +
                                    "(Parameter index: ${parameter.index}) (Type: ${parameter.type}) " +
                                    "(Kind: ${parameter.kind} (Method: ${listenerClass.jvmName}#${method.name})"
                        )
                    } else {
                        exceptionHandler("onParameterHasNoResolver") {
                            onParameterHasNoResolver(
                                listener, method, parameter,
                                name, parameter.type
                            )
                        }
                    }
                    continue@out
                }
            }

            val eventClass = parameters[1].type.classifier as? KClass<*> ?: continue
            if (!Dispatchable::class.isSuperclassOf(eventClass)) {
                exceptionHandler("onMethodHasNoDispatchableParameter") {
                    onMethodHasNoDispatchableParameter(
                        listener,
                        method,
                        parameters[1].type
                    )
                }
                continue@out
            }

            @Suppress("UNCHECKED_CAST")
            val typedEventClass = eventClass as KClass<Dispatchable>

            val args = method.parameters.map { param ->
                when (param.index) {
                    0 -> listener
                    1 -> null
                    else -> {
                        val resolver = resolvers[param]
                        resolver?.valueByConfiguration
                    }
                }
            }

            val defaultConfigOrCaptured: EventConfiguration<Dispatchable>? =
                if (!parameters[1].type.isMarkedNullable) {
                    EventConfiguration.default()
                } else {
                    try {
                        method.isAccessible = true
                        if (method.isSuspend) {
                            var exception: InvocationTargetException? = null
                            val job = scope.launch(Dispatchers.Unconfined) {
                                try {
                                    method.callSuspend(*args.toTypedArray())
                                    exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                        onMethodDidNotThrowConfiguredException(
                                            listener,
                                            method
                                        )
                                    }
                                } catch (e: InvocationTargetException) {
                                    exception = e
                                }
                            }
                            job.cancel()
                            if (exception != null) throw exception
                        } else {
                            method.call(*args.toTypedArray())
                            exceptionHandler("onMethodDidNotThrowConfiguredException") {
                                onMethodDidNotThrowConfiguredException(
                                    listener,
                                    method
                                )
                            }
                        }
                        null
                    } catch (_: InaccessibleObjectException) {
                        exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                        null
                    } catch (e: InvocationTargetException) {
                        val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                        if (config !is EventConfiguration<*>) {
                            exceptionHandler("onMethodThrewUnexpectedException") {
                                onMethodThrewUnexpectedException(
                                    listener,
                                    method,
                                    e.targetException
                                )
                            }
                            null
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            config as EventConfiguration<Dispatchable>
                        }
                    } catch (t: Throwable) {
                        t.rethrowIfFatal()
                        exceptionHandler("onUnexpectedExceptionDuringRegistration") {
                            onUnexpectedExceptionDuringRegistration(
                                listener,
                                method,
                                t
                            )
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

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: SimpleListener<*>) {
        checkClosed()
        val registered = RegisteredSimpleListener(this, listener) as RegisteredListener<Dispatchable>
        getOrCreateHandlerBucket(registered.type).add(registered)
    }

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: SimpleSuspendListener<*>) {
        checkClosed()
        val registered = RegisteredSimpleSuspendListener(this, listener) as RegisteredListener<Dispatchable>
        getOrCreateHandlerBucket(registered.type).add(registered)
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

    override fun unregister(listener: SimpleListener<*>) {
        checkClosed()
        handlers.values.forEach { it.remove(listener) }
    }

    override fun unregister(listener: SimpleSuspendListener<*>) {
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
                (listener as? RegisteredKFunctionListener)?.listener,
                (listener as? RegisteredKFunctionListener<*>)?.kFunction
            )
        } catch (handlerException: Throwable) {
            handlerException.rethrowIfFatal()
            logger.log(
                Level.SEVERE,
                "Exception-Handler failed while handling an exception",
                handlerException
            )

            val listenerDescription = when (listener) {
                is RegisteredFunctionListener<*> -> "lambda: ${listener.method::class.jvmName}"
                is RegisteredKFunctionListener<*> -> "from: ${listener.listener::class.jvmName}#${listener.kFunction.name}"
                is RegisteredSuspendFunctionListener<*> -> "suspend lambda: ${listener.suspendMethod::class.jvmName}"
                is RegisteredSimpleListener<*> -> "simple listener: ${listener.simpleListener::class.jvmName}"
                is RegisteredSimpleSuspendListener<*> -> "simple suspend listener: ${listener.simpleListener::class.jvmName}"
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
            e.rethrowIfFatal()
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

        fun remove(listener: RegisteredListener<E>) = removeByIdentity(listener)

        fun remove(target: SimpleListener<*>) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { (it as? RegisteredSimpleListener<E>)?.simpleListener === target }
                if (next.size == cur.size) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        fun remove(target: SimpleSuspendListener<*>) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { (it as? RegisteredSimpleSuspendListener<E>)?.simpleListener === target }
                if (next.size == cur.size) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        fun remove(target: Listener) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { (it as? RegisteredKFunctionListener<E>)?.listener === target }
                if (next.size == cur.size) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        private fun removeByIdentity(target: RegisteredListener<E>) {
            while (true) {
                val cur = snapshot.get()
                val next = cur.filterNot { it === target }
                if (next.size == cur.size) return
                if (snapshot.compareAndSet(cur, next)) return
            }
        }

        fun existListener(clazz: KClass<out Listener>) =
            snapshot.get().any { (it as? RegisteredKFunctionListener<E>)?.listener?.let { l -> l::class == clazz } ?: false }

        @Suppress("UNCHECKED_CAST")
        fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
            var firstMatch: Listener? = null
            return snapshot.get().filter { registered ->
                (registered as? RegisteredKFunctionListener<E>)?.listener?.let {
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
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES) && typedEvent::class != handler.type) continue
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

        fun close() {
            snapshot.set(emptyList())
        }
    }

    private sealed class RegisteredListener<E : Dispatchable>(
        val type: KClass<E>,
        val configuration: EventConfiguration<E>,
        val manager: DefaultEventManager,
    ) {
        open val isSuspend: Boolean = false
        abstract val handlerId: String

        operator fun invoke(event: E): Boolean {
            if (isSuspend) throw UnsupportedOperationException("invoke is not supported for suspend functions.")
            if (configuration.getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)) {
                if (!manager.sharedExclusiveExecution.tryAcquire(handlerId)) return false
            }
            try {
                invokeInternal(event)
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

        protected abstract fun invokeInternal(event: E)
    }

    private class RegisteredFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        val method: (E) -> Unit,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, configuration, manager) {
        override val handlerId: String = "RegisteredFunctionListener@${type.jvmName}@${method.hashCode()}"

        override fun invokeInternal(event: E) {
            method(event)
        }
    }

    private class RegisteredSuspendFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        configuration: EventConfiguration<E>,
        manager: DefaultEventManager,
        val suspendMethod: suspend (E) -> Unit
    ) : RegisteredListener<E>(type, configuration, manager) {
        override val isSuspend: Boolean = true
        override val handlerId: String = "RegisteredSuspendFunctionListener@${type.jvmName}@${suspendMethod.hashCode()}"

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            suspendMethod(event)
        }

        override fun invokeInternal(event: E) {
            throw UnsupportedOperationException("${this::class.jvmName} does not support none suspend listeners.")
        }
    }

    private class RegisteredKFunctionListener<E : Dispatchable>(
        type: KClass<E>,
        val listener: Listener,
        val kFunction: KFunction<*>,
        configuration: EventConfiguration<E>,
        val resolvers: Map<KParameter, ListenerParameterResolver<*>>,
        manager: DefaultEventManager,
    ) : RegisteredListener<E>(type, configuration, manager) {
        val eventParameter = kFunction.parameters[1]
        private val actualType = eventParameter.type
        private val typeArguments by lazy { actualType.arguments }
        private val hasTypeArguments: Boolean by lazy { typeArguments.any { it.type != null } }
        override val isSuspend: Boolean = kFunction.isSuspend
        override val handlerId: String =
            "RegisteredKFunctionListener@${type.jvmName}@${listener::class.jvmName}#${kFunction.hashCode()}"

        @Suppress("UNCHECKED_CAST")
        private val extraStrategies: Array<ArgStrategy<E>> by lazy {
            kFunction.parameters.drop(2).map { param ->
                resolvers[param].toStrategy(this)
            }.toTypedArray()
        }

        override fun invokeInternal(event: E) {
            if (extraStrategies.isEmpty()) {
                kFunction.call(listener, event)
            } else {
                val args = buildArgs(event, true)
                kFunction.call(*args)
            }
        }

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            if (extraStrategies.isEmpty()) {
                kFunction.callSuspend(listener, event)
            } else {
                val args = buildArgs(event, isWaiting)
                kFunction.callSuspend(*args)
            }
        }

        private fun buildArgs(event: E, isWaiting: Boolean): Array<Any?> {
            val args = arrayOfNulls<Any?>(2 + extraStrategies.size)
            args[0] = listener
            args[1] = event
            for (i in extraStrategies.indices) {
                args[i + 2] = extraStrategies[i].resolve(event, isWaiting)
            }
            return args
        }

        fun allowGenericTypes(types: List<KClass<*>>): Boolean {
            if (!hasTypeArguments) return true
            if (typeArguments.size != types.size) return false
            return typeArguments.mapIndexed { index, projection ->
                if (projection.variance == null) return@mapIndexed true
                val type = projection.type?.classifier as? KClass<*> ?: return@mapIndexed false
                @Suppress("SENSELESS_NULL_IN_WHEN") // false positive inspection
                when (projection.variance) {
                    KVariance.INVARIANT -> type == types[index]
                    KVariance.IN -> types[index].isSuperclassOf(type)
                    KVariance.OUT -> types[index].isSubclassOf(type)
                    null -> true
                }
            }.all()
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
                ?: throw IllegalStateException(
                    "No resolver found for parameter ${arg.key} with type ${arg.value.jvmName}.",
                    NO_RESOLVER
                )).toStrategy(this)
        }.toMap()

        override fun invokeInternal(event: E) {
            simpleListener.handleArgs(event, resolvers.mapValues { it.value.resolve(event, true) })
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
                ?: throw IllegalStateException(
                    "No resolver found for parameter ${arg.key} with type ${arg.value.jvmName}.",
                    NO_RESOLVER
                )).toStrategy(this)
        }.toMap()

        override fun invokeInternal(event: E) {
            throw UnsupportedOperationException("${this::class.jvmName} does not support none suspend listeners.")
        }
        override val handlerId: String = "RegisteredSimpleSuspendListener@${type.jvmName}@${simpleListener.hashCode()}"

        override val isSuspend: Boolean = true

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            simpleListener.handleArgs(event, resolvers.mapValues { it.value.resolve(event, isWaiting) })
        }
    }

    private sealed interface ArgStrategy<E : Dispatchable> {
        fun resolve(event: E, isWaiting: Boolean): Any?
    }

    private class WaitingStrategy<E : Dispatchable> : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean) = isWaiting
    }

    private class ConfigStrategy<E : Dispatchable>(
        private val configuration: EventConfiguration<E>,
    ) : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean) = configuration
    }

    private class ResolverStrategy<E : Dispatchable>(
        private val listener: Listener?,
        private val kFunction: KFunction<*>?,
        private val resolver: ListenerParameterResolver<*>,
    ) : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean): Any =
            resolver.resolve(listener, kFunction, event)
    }

    private class NullStrategy<E : Dispatchable> : ArgStrategy<E> {
        override fun resolve(event: E, isWaiting: Boolean) = null
    }

    companion object {
        internal val NO_RESOLVER = Throwable(null, null).apply { stackTrace = emptyArray() }

        private val logger = Logger.getLogger(DefaultEventManager::class.jvmName)
            .apply {
                level = Level.SEVERE
            }

        private fun <E : Dispatchable> ListenerParameterResolver<*>?.toStrategy(registered: RegisteredListener<E>): ArgStrategy<E> =
            when (this) {
                IsWaitingParameterResolver -> WaitingStrategy()
                ConfigParameterResolver -> ConfigStrategy(registered.configuration)
                is ListenerParameterResolver<*> -> ResolverStrategy(
                    (registered as? RegisteredKFunctionListener)?.listener,
                    (registered as? RegisteredKFunctionListener)?.kFunction,
                    this
                )

                else -> NullStrategy()
            }
    }

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
        override val valueByConfiguration: EventConfiguration<*> = EventConfiguration.DEFAULT
    }
}