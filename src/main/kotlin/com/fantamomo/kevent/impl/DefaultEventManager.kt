package com.fantamomo.kevent.impl

import com.fantamomo.kevent.*
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

class DefaultEventManager : EventManager {

    private val listenerMap: MutableMap<KClass<out Event>, MutableList<RegisteredListener<out Event>>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: Listener) {
        for (method in listener::class.memberFunctions) {
            if (method.findAnnotation<Register>() == null) continue
            if (method.returnType.classifier != Unit::class) continue

            val parameters = method.parameters.drop(1)
            if (parameters.size != 1) continue

            val param = parameters.first()
            val eventClass = (param.type.classifier as? KClass<*>)?.takeIf {
                Event::class.java.isAssignableFrom(it.java) && param.type.isMarkedNullable
            } as? KClass<Event> ?: continue

            method.isAccessible = true

            try {
                method.call(listener, null)
            } catch (ite: InvocationTargetException) {
                val config = (ite.cause as? ConfigurationCapturedException)?.configuration as? EventConfiguration<Event>
                    ?: throw ite

                val handler: (Event) -> Unit = { event -> method.call(listener, event) }

                val registeredListener = RegisteredListener(listener, eventClass, config, handler)

                listenerMap.computeIfAbsent(eventClass) { mutableListOf() }
                    .add(registeredListener)

                listenerMap[eventClass]!!.sortByDescending {
                    it.configuration.getOrDefault(Key.PRIORITY)
                }
            }
        }
    }

    override fun dispatch(event: Event) {
        val eventClass = event::class

        listenerMap.forEach { (registeredClass, listeners) ->
            if (eventClass.isSubclassOf(registeredClass)) {
                for (listener in listeners) {
                    @Suppress("UNCHECKED_CAST")
                    (listener as RegisteredListener<Event>).handler(event)
                }
            }
        }
    }

    private class RegisteredListener<E : Event>(
        val listener: Listener,
        val eventClass: KClass<E>,
        val configuration: EventConfiguration<E>,
        val handler: (E) -> Unit
    )
}