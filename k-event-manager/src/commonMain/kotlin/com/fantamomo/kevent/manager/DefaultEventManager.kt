package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.*
import com.fantamomo.kevent.manager.settings.Settings
import com.fantamomo.kevent.manager.settings.getSetting
import com.fantamomo.kevent.utils.InjectionName
import kotlinx.coroutines.*
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
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

    private val handlers: ConcurrentHashMap<KClass<out Dispatchable>, HandlerList<out Dispatchable>> =
        ConcurrentHashMap()

    private val exceptionHandler = components.getOrThrow(ExceptionHandler)

    private val parameterResolver: List<ListenerParameterResolver<*>>

    private val sharedExclusiveExecution = components.getOrThrow(SharedExclusiveExecution)

    private val scope: CoroutineScope =
        components[EventCoroutineScope]?.scope ?: CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isClosed = false

    private val dispatchDeadEvents = components.getSetting(Settings.DISPATCH_DEAD_EVENTS)

    init {
        var components = components
        if (!components.getSetting(Settings.DISABLE_EVENTMANAGER_INJECTION)) components += ListenerParameterResolver.static(
            "manager",
            EventManager::class,
            this
        )

        if (!components.getSetting(Settings.DISABLE_LOGGER_INJECTION)) ListenerParameterResolver.static(
            "logger",
            Logger::class,
            logger
        )
        if (!components.getSetting(Settings.DISABLE_SCOPE_INJECTION)) ListenerParameterResolver.static(
            "scope",
            CoroutineScope::class,
            CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext.job))
        )

        if (!components.getSetting(Settings.DISABLE_IS_WAITING_INJECTION)) components += IsWaitingParameterResolver

        parameterResolver = components.getAll(ListenerParameterResolver.Key)
    }

    private fun existListener(clazz: KClass<out Listener>) = handlers.any { it.value.existListener(clazz) }

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
                } catch (_: Throwable) {
                    // Silent fail
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
            getOrCreateHandlerList(registered.type).add(new)
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
                }
                    ?: continue@out
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

            if (!parameters[1].type.isMarkedNullable) {
                try {
                    method.isAccessible = true
                } catch (_: Throwable) {
                    exceptionHandler("onMethodNotAccessible") { onMethodNotAccessible(listener, method) }
                    continue
                }
                val handler = RegisteredKFunctionListener(
                    type = typedEventClass,
                    listener = listener,
                    kFunction = method,
                    configuration = EventConfiguration.default(),
                    resolvers = resolvers,
                    manager = this,
                )

                getOrCreateHandlerList(typedEventClass).add(handler)
                continue
            }

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
            } catch (e: InvocationTargetException) {
                val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                if (config !is EventConfiguration<*>) {
                    exceptionHandler("onMethodThrewUnexpectedException") { onMethodThrewUnexpectedException(listener, method, e.targetException) }
                    continue@out
                }

                @Suppress("UNCHECKED_CAST")
                val handler = RegisteredKFunctionListener(
                    type = typedEventClass,
                    listener = listener,
                    kFunction = method,
                    configuration = config as EventConfiguration<Dispatchable>,
                    resolvers = resolvers,
                    manager = this,
                )

                getOrCreateHandlerList(typedEventClass).add(handler)
            } catch (e: Throwable) {
                exceptionHandler("onUnexpectedExceptionDuringRegistration") { onUnexpectedExceptionDuringRegistration(listener, method, e) }
            }
        }
    }

    override fun dispatch(event: Dispatchable) {
        checkClosed()
        if (handlers.isEmpty()) return
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: listOf()

        for ((registeredClass, handlerList) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or handlerList.call(event, genericTypes)
        }

        if (dispatchDeadEvents && !called && eventClass != DeadEvent::class) {
            dispatch(DeadEvent(event))
        }
    }

    override suspend fun dispatchSuspend(event: Dispatchable) {
        checkClosed()
        val eventClass = event::class
        var called = false
        val genericTypes = (event as? GenericTypedEvent)?.extractGenericTypes() ?: listOf()

        for ((registeredClass, handlerList) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or handlerList.callSuspend(event, genericTypes)
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
        getOrCreateHandlerList(event).add(listener)
        return RegisteredLambdaHandler {
            @Suppress("UNCHECKED_CAST")
            (handlers[event] as? HandlerList<E>)?.remove(listener)
        }
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

    override fun unregister(listener: Listener) {
        checkClosed()
        handlers.forEach {
            it.value.remove(listener)
        }
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
            }

            logger.log(Level.SEVERE, "Original exception was ($listenerDescription):", e)
        }
    }

    @Suppress("WRONG_INVOCATION_KIND")
    @OptIn(ExperimentalContracts::class)
    private fun exceptionHandler(methodName: String, block: ExceptionHandler.() -> Unit) {
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

    private fun <E : Dispatchable> getOrCreateHandlerList(type: KClass<E>): HandlerList<E> {
        @Suppress("UNCHECKED_CAST")
        return handlers.computeIfAbsent(type) { HandlerList<E>() } as HandlerList<E>
    }

    private inner class HandlerList<E : Dispatchable> {
        private val listeners: MutableList<RegisteredListener<E>> = mutableListOf()

        @Volatile
        private var sortedListeners: List<RegisteredListener<E>> = emptyList()

        @Volatile
        private var dirty: Boolean = true

        fun add(listener: RegisteredListener<E>) {
            synchronized(this) {
                listeners.add(listener)
                dirty = true
            }
        }

        fun remove(listener: Listener) {
            synchronized(this) {
                listeners.removeAll { it.listener === listener }
            }
        }

        fun call(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            if (listeners.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            val currentList = getSortedListeners()

            var called = false
            for (handler in currentList) {
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                    if (typedEvent::class != handler.type) continue
                }
                if (event::class == handler.type && genericTypes.isNotEmpty() && handler is RegisteredKFunctionListener<E> && !handler.allowGenericsTypes(
                        genericTypes
                    )
                ) continue
                called = called || !handler.configuration.getOrDefault(Key.SILENT)
                if (handler.isSuspend) {
                    scope.launch(Dispatchers.Unconfined) {
                        try {
                            handler.invokeSuspend(typedEvent, false)
                        } catch (e: Throwable) {
                            @Suppress("UNCHECKED_CAST")
                            handleException(e, handler)
                        }
                    }
                } else {
                    try {
                        handler(typedEvent)
                    } catch (e: Throwable) {
                        @Suppress("UNCHECKED_CAST")
                        handleException(e, handler)
                    }
                }
            }
            return called
        }

        suspend fun callSuspend(event: Dispatchable, genericTypes: List<KClass<*>>): Boolean {
            if (listeners.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            val currentList = getSortedListeners()

            var called = false
            for (handler in currentList) {
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                    if (typedEvent::class != handler.type) continue
                }
                if (event::class == handler.type && genericTypes.isNotEmpty() && handler is RegisteredKFunctionListener<E> && !handler.allowGenericsTypes(
                        genericTypes
                    )
                ) continue
                called = called || !handler.configuration.getOrDefault(Key.SILENT)
                try {
                    if (handler.isSuspend) {
                        handler.invokeSuspend(event, true)
                    } else {
                        handler.invoke(typedEvent)
                    }
                } catch (e: Throwable) {
                    @Suppress("UNCHECKED_CAST")
                    handleException(e, handler)
                }
            }
            return called
        }

        private fun getSortedListeners(): List<RegisteredListener<E>> {
            if (!dirty) return sortedListeners
            synchronized(this) {
                if (!dirty) return sortedListeners
                sortedListeners = listeners.sortedByDescending {
                    it.configuration.getOrDefault(Key.PRIORITY)
                }
                dirty = false
                return sortedListeners
            }
        }

        fun remove(listener: RegisteredFunctionListener<E>) {
            listeners.remove(listener)
        }

        fun close() {
            listeners.clear()
            sortedListeners = emptyList()
            dirty = false
        }

        fun existListener(clazz: KClass<out Listener>) =
            listeners.any { it.listener?.let { listener -> listener::class == clazz } ?: false }

        @Suppress("UNCHECKED_CAST")
        fun findAllListeners(clazz: KClass<out Listener>): List<RegisteredKFunctionListener<*>> {
            var listener: Listener? = null
            return listeners.filter { registeredListener ->
                registeredListener.listener?.let {
                    if (it::class != clazz) return@filter true
                    if (listener == null) listener = it
                    it === listener
                } ?: false
            } as List<RegisteredKFunctionListener<*>>
        }
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

        protected open suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {}
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
        val actualType = eventParameter.type
        val typeArguments by lazy { actualType.arguments }
        override val isSuspend: Boolean = kFunction.isSuspend
        override val handlerId: String = "RegisteredKFunctionListener@${type.jvmName}@${listener::class.jvmName}#${kFunction.hashCode()}"

        override val method: (E) -> Unit = { evt ->
            val args = mapOf(thisParameter to listener, eventParameter to evt) + resolvers.mapValues {
                if (it.value is InternalParameterResolver<*>) {
                    return@mapValues when (it.value) {
                        IsWaitingParameterResolver -> true
                        else -> throw IllegalStateException("This should not happen.")
                    }

                }
                it.value.resolve(
                    listener,
                    kFunction,
                    evt
                )
            }
            kFunction.callBy(args)
        }

        override suspend fun invokeSuspendInternal(event: E, isWaiting: Boolean) {
            val args = mapOf(thisParameter to listener, eventParameter to event) + resolvers.mapValues {
                if (it.value is InternalParameterResolver<*>) {
                    return@mapValues when (it.value) {
                        IsWaitingParameterResolver -> isWaiting
                        else -> throw IllegalStateException("This should not happen.")
                    }
                }
                it.value.resolve(
                    listener,
                    kFunction,
                    event
                )
            }
            kFunction.callSuspendBy(args)
        }

        fun allowGenericsTypes(types: List<KClass<*>>): Boolean {
            return typeArguments.size == types.size &&
                    typeArguments.mapIndexed { index, projection ->
                        if (projection.variance == null) return@mapIndexed true
                        val type = projection.type?.classifier as? KClass<*> ?: return@mapIndexed false
                        @Suppress("SENSELESS_NULL_IN_WHEN")
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
}