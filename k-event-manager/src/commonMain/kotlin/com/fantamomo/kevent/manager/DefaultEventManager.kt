package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ExceptionHandler
import com.fantamomo.kevent.manager.components.getOrThrow
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmName

class DefaultEventManager internal constructor(
    components: EventManagerComponent<*>
) : EventManager {

    private val handlers: ConcurrentHashMap<KClass<out Dispatchable>, HandlerList<out Dispatchable>> =
        ConcurrentHashMap()

    private val exceptionHandler = components.getOrThrow(ExceptionHandler)

    override fun register(listener: Listener) {
        val listenerClass = listener::class
        for (method in listenerClass.declaredMemberFunctions) {
            if (!method.hasAnnotation<Register>()) continue
            if (method.visibility != KVisibility.PUBLIC) continue

            val parameters = method.parameters
            if (parameters.size != 2) continue

            val eventClass = parameters[1].type.classifier as? KClass<*> ?: continue
            if (!Dispatchable::class.isSuperclassOf(eventClass)) continue

            @Suppress("UNCHECKED_CAST")
            val typedEventClass = eventClass as KClass<Dispatchable>

            try {
                method.isAccessible = true
                method.call(listener, null)
            } catch (e: InvocationTargetException) {
                val config = (e.targetException as? ConfigurationCapturedException)?.configuration
                if (config !is EventConfiguration<*>) continue

                @Suppress("UNCHECKED_CAST")
                val handler = RegisteredListener(
                    type = typedEventClass,
                    listener = listener,
                    method = { evt -> method.call(listener, evt) },
                    configuration = config as EventConfiguration<Dispatchable>
                )

                getOrCreateHandlerList(typedEventClass).add(handler)
            } catch (_: Throwable) {
                // Silent fail
            }
        }
    }

    override fun dispatch(event: Dispatchable) {
        val eventClass = event::class
        var called = false

        for ((registeredClass, handlerList) in handlers) {
            if (!registeredClass.isSuperclassOf(eventClass)) continue
            called = called or handlerList.call(event)
        }

        if (!called && eventClass != DeadEvent::class) {
            dispatch(DeadEvent(event))
        }
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit
    ) {
        val listener = RegisteredListener(
            type = event,
            listener = null,
            method = handler,
            configuration = configuration
        )
        getOrCreateHandlerList(event).add(listener)
    }

    private fun handleException(e: Throwable, listener: Listener?, method: (Dispatchable) -> Unit) {
        try {
            exceptionHandler.handle(
                e,
                listener,
                method
            )
        } catch (e: Throwable) {
            // if the handler threw an exception... well, log it
            logger.log(Level.SEVERE, "The handler which should handle a exception threw an exception", e)
            logger.log(Level.SEVERE, "Original exception (from ${listener?.let { it::class.jvmName }}", e)
        }
    }

    private fun <E : Dispatchable> getOrCreateHandlerList(type: KClass<E>): HandlerList<E> {
        @Suppress("UNCHECKED_CAST")
        return handlers.computeIfAbsent(type) { HandlerList<E>() } as HandlerList<E>
    }

    private inner class HandlerList<E : Dispatchable> {
        private val listeners: MutableList<RegisteredListener<E>> = mutableListOf()
        @Volatile private var sortedListeners: List<RegisteredListener<E>> = emptyList()
        @Volatile private var dirty: Boolean = true

        fun add(listener: RegisteredListener<E>) {
            synchronized(this) {
                listeners.add(listener)
                dirty = true
            }
        }

        fun call(event: Dispatchable): Boolean {
            if (listeners.isEmpty()) return false

            @Suppress("UNCHECKED_CAST")
            val typedEvent = event as E
            val currentList = getSortedListeners()

            var called = false
            for (handler in currentList) {
                if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                    if (typedEvent::class != handler.type) continue
                }
                try {
                    handler(typedEvent)
                    called = true
                } catch (e: Throwable) {
                    @Suppress("UNCHECKED_CAST")
                    handleException(e, handler.listener, handler.method as (Dispatchable) -> Unit)
                }
            }
            return called
        }

        private fun getSortedListeners(): List<RegisteredListener<E>> {
            if (!dirty) return sortedListeners
            synchronized(this) {
                if (!dirty) return sortedListeners
                sortedListeners = listeners.sortedBy {
                    it.configuration.getOrDefault(Key.PRIORITY)
                }
                dirty = false
                return sortedListeners
            }
        }
    }

    private inner class RegisteredListener<E : Dispatchable>(
        val type: KClass<E>,
        val listener: Listener?,
        val method: (E) -> Unit,
        val configuration: EventConfiguration<E>
    ) {
        operator fun invoke(event: E) = method(event)
    }

    companion object {
        private val logger = Logger.getLogger(DefaultEventManager::class.jvmName)
    }
}
