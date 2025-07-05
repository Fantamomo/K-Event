package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ExceptionHandler
import com.fantamomo.kevent.manager.components.getOrThrow
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.isAccessible

class DefaultEventManager internal constructor(components: EventManagerComponent<*>) : EventManager {
    private val handlers: MutableMap<KClass<out Dispatchable>, MutableList<RegisteredListener<Dispatchable>>> = mutableMapOf()
    private val exceptionHandler = components.getOrThrow(ExceptionHandler)

    override fun register(listener: Listener) {
        val listenerClass = listener::class
        for (methode in listenerClass.declaredMemberFunctions) {
            if (!methode.hasAnnotation<Register>()) continue

            if (methode.visibility != KVisibility.PUBLIC) continue

            val parameter = methode.parameters
            if (parameter.size != 2) continue

            @Suppress("UNCHECKED_CAST")
            val eventClass = parameter[1].type.classifier as? KClass<Dispatchable> ?: continue
            if (!Dispatchable::class.isSuperclassOf(eventClass)) continue

            try {
                methode.isAccessible = true
                methode.call(listener, null)
            } catch (e: InvocationTargetException) {
                @Suppress("UNCHECKED_CAST")
                val configuration =
                    (e.targetException
                            as? ConfigurationCapturedException)?.configuration
                            as? EventConfiguration<Dispatchable>
                        ?: continue

                handlers.getOrPut(eventClass) { mutableListOf() }.add(
                    RegisteredListener(
                        eventClass, listener, { methode.call(listener, it) },
                        configuration
                    )
                )
            } catch (e: Throwable) {
                continue
            }
        }
    }

    override fun dispatch(event: Dispatchable) {
        val eventClass = event::class
        var called = false

        for ((target, listener) in handlers) {
            if (listener.isEmpty()) continue
            if (!target.isSuperclassOf(eventClass)) continue

            called = called || call(event, listener)
        }
        if (!called && eventClass != DeadEvent::class) dispatch(DeadEvent(event))
    }

    private fun call(event: Dispatchable, listener: List<RegisteredListener<Dispatchable>>): Boolean {
        var called = false
        listener.sortedBy { it.configuration.getOrDefault(Key.PRIORITY) }.forEach { handler ->
            if (handler.configuration.getOrDefault(Key.DISALLOW_SUBTYPES)) {
                if (event::class != handler.type) return@forEach
            }
            called = true
            handler(event)
        }
        return called
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(event) { mutableListOf() }.add(RegisteredListener(event, null, handler, configuration) as RegisteredListener<Dispatchable>)
    }

    private inner class RegisteredListener<D : Dispatchable>(
        val type: KClass<D>,
        val listener: Listener?,
        val methode: (D) -> Unit,
        val configuration: EventConfiguration<D>,
    ) {
        operator fun invoke(event: D) {
            @Suppress("UNCHECKED_CAST")
            try {
                methode(event)
            } catch (e: InvocationTargetException) {
                exceptionHandler.handle(e.cause ?: e, listener, methode as (Dispatchable) -> Unit)
            } catch (e: Throwable) {
                exceptionHandler.handle(e, listener, methode as (Dispatchable) -> Unit)
            }
        }
    }
}