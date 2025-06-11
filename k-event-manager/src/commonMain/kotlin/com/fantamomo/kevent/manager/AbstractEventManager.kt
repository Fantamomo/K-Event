package com.fantamomo.kevent.manager

import com.fantamomo.kevent.ConfigurationCapturedException
import com.fantamomo.kevent.Event
import com.fantamomo.kevent.EventConfiguration
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

    protected val listenerMap = mutableMapOf<KClass<out Event>, MutableList<RegisteredListener<out Event>>>()

    @Suppress("UNCHECKED_CAST")
    override fun register(listener: Listener) {
        for (method in listener::class.memberFunctions) {
            if (method.findAnnotation<Register>() == null) continue
            if (method.returnType.classifier != Unit::class) {
                onInvalidReturnType(listener, method, method.returnType.classifier)
                continue
            }

            val parameters = method.parameters.drop(1)
            if (parameters.size != 1) {
                onInvalidParameterCount(listener, method, parameters.size)
                continue
            }

            val param = parameters.first()
            val paramType = param.type.classifier as? KClass<*> ?: continue
            if (!paramType.isSubclassOf(Event::class)) {
                onInvalidParameterType(listener, method, param)
                continue
            }
            if (!param.type.isMarkedNullable) {
                onNonNullableParameter(listener, method, param)
                continue
            }

            val eventClass = paramType as KClass<Event>
            method.isAccessible = true

            val config = try {
                method.call(listener, null)
                onMissingConfigurationException(listener, method)
                continue
            } catch (e: InvocationTargetException) {
                val cause = e.cause
                if (cause !is ConfigurationCapturedException) {
                    onUnexpectedException(listener, method, cause ?: e)
                    continue
                }
                cause.configuration as EventConfiguration<Event>
            } catch (e: Exception) {
                onUnexpectedException(listener, method, e)
                continue
            }

            val handler: (Event) -> Unit = { event -> method.call(listener, event) }

            val registeredListener = buildRegisteredListener(listener, eventClass, config, handler)
            val list = listenerMap.computeIfAbsent(eventClass) { mutableListOf() }
            list.add(registeredListener)
            list.sortByDescending { it.configuration.getOrDefault(Key.PRIORITY) }
        }
    }

    protected open fun <E : Event> buildRegisteredListener(
        listener: Listener?,
        eventClass: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit
    ) = RegisteredListener(listener, eventClass, configuration, handler)

    protected open fun onInvalidReturnType(listener: Listener, method: KFunction<*>, actual: KClassifier?) {}
    protected open fun onInvalidParameterCount(listener: Listener, method: KFunction<*>, count: Int) {}
    protected open fun onInvalidParameterType(listener: Listener, method: KFunction<*>, param: KParameter) {}
    protected open fun onNonNullableParameter(listener: Listener, method: KFunction<*>, param: KParameter) {}
    protected open fun onMissingConfigurationException(listener: Listener, method: KFunction<*>) {}
    protected open fun onUnexpectedException(listener: Listener, method: KFunction<*>, e: Throwable) {}

    protected open fun <E : Event> onHandlerException(
        registeredListener: RegisteredListener<E>,
        event: E,
        e: Throwable
    ) {}

    override fun dispatch(event: Event) {
        val eventClass = event::class
        for ((registeredClass, listeners) in listenerMap) {
            if (!eventClass.isSubclassOf(registeredClass)) continue

            for (listener in listeners) {
                @Suppress("UNCHECKED_CAST")
                listener as RegisteredListener<Event>
                if (listener.disallowSubtypes && eventClass != listener.eventClass) continue
                if (listener.exclusiveListenerProcessing && listener.isCurrentlyCalled) continue
                listener.isCurrentlyCalled = true
                listener.callCount++
                try {
                    listener.handler(event)
                    listener.successfulCallCount++
                } catch (e: InvocationTargetException) {
                    val cause = if (e.cause != null) e.cause!! else {
                        onHandlerException(listener, event, e)
                        continue
                    }
                    onHandlerException(listener, event, cause)
                } catch (e: Exception) {
                    onHandlerException(listener, event, e)
                } finally {
                    listener.isCurrentlyCalled = false
                }
            }
        }
    }

    override fun <E : Event> register(event: KClass<E>, configuration: EventConfiguration<E>, handler: (E) -> Unit) {
        listenerMap.computeIfAbsent(event) { mutableListOf() }.add(buildRegisteredListener(null, event, configuration, handler))
    }

    protected open class RegisteredListener<E : Event>(
        val listener: Listener?,
        val eventClass: KClass<E>,
        val configuration: EventConfiguration<E>,
        val handler: (E) -> Unit
    ) {
        val disallowSubtypes = configuration.getOrDefault(Key.DISALLOW_SUBTYPES)
        val exclusiveListenerProcessing = configuration.getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)
        var isCurrentlyCalled = false
        var callCount: Int = 0
        var successfulCallCount: Int = 0
    }
}