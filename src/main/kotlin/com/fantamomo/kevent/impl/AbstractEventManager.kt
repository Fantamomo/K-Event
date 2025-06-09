package com.fantamomo.kevent.impl

import com.fantamomo.kevent.ConfigurationCapturedException
import com.fantamomo.kevent.Event
import com.fantamomo.kevent.EventConfiguration
import com.fantamomo.kevent.EventManager
import com.fantamomo.kevent.Key
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.Register
import java.lang.reflect.InvocationTargetException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.sortByDescending
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

abstract class AbstractEventManager : EventManager {

    protected val listenerMap: MutableMap<KClass<out Event>, MutableList<RegisteredListener<out Event>>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: Listener) {
        for (method in listener::class.memberFunctions) {
            if (method.findAnnotation<Register>() == null) continue
            if (method.returnType.classifier != Unit::class) {
                errorNotUnitReturnType(listener, method, method.returnType.classifier)
                continue
            }

            val parameters = method.parameters.drop(1)
            if (parameters.size != 1) {
                errorNotOneParameter(listener, method, parameters.size)
                continue
            }

            val param = parameters.first()
            val paramType = param.type.classifier as? KClass<*> ?: continue
            if (!paramType.isSubclassOf(Event::class)) {
                errorNotEventParameter(listener, method, param)
                continue
            }
            if (!param.type.isMarkedNullable) {
                errorNotNullableParameter(listener, method, param)
                continue
            }
            val eventClass = paramType as KClass<Event>

            method.isAccessible = true

            try {
                method.call(listener, null)
            } catch (ite: InvocationTargetException) {
                val cause = ite.cause ?: continue
                if (cause !is ConfigurationCapturedException) {
                    errorUnrecognizedException(listener, method, cause)
                    continue
                }

                val config = cause.configuration as EventConfiguration<Event>

                val handler: (Event) -> Unit = { event -> method.call(listener, event) }

                val registeredListener = RegisteredListener(listener, eventClass, config, handler)

                listenerMap.computeIfAbsent(eventClass) { mutableListOf() }
                    .add(registeredListener)

                listenerMap[eventClass]!!.sortByDescending {
                    it.configuration.getOrDefault(Key.PRIORITY)
                }
                continue
            } catch (e: Exception) {
                errorUnrecognizedException(listener, method, e)
            }
            errorNoExceptionCaught(listener, method)
        }
    }

    protected open fun errorNotUnitReturnType(listener: Listener, methode: KFunction<*>, real: KClassifier?) {}

    protected open fun errorNotOneParameter(listener: Listener, methode: KFunction<*>, count: Int) {}

    protected open fun errorNotEventParameter(listener: Listener, methode: KFunction<*>, real: KParameter) {}

    protected open fun errorNotNullableParameter(listener: Listener, methode: KFunction<*>, param: KParameter) {}

    protected open fun errorNoExceptionCaught(listener: Listener, methode: KFunction<*>) {}

    protected open fun errorUnrecognizedException(listener: Listener, methode: KFunction<*>, e: Throwable) {}

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

    protected class RegisteredListener<E : Event>(
        val listener: Listener,
        val eventClass: KClass<E>,
        val configuration: EventConfiguration<E>,
        val handler: (E) -> Unit
    )
}