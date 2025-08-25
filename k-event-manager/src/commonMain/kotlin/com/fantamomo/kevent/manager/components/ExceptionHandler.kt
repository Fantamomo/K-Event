package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Listener
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KVisibility

abstract class ExceptionHandler : EventManagerComponent<ExceptionHandler> {
    override val key: EventManagerComponent.Key<ExceptionHandler>
        get() = Key

    companion object Key : EventManagerComponent.Key<ExceptionHandler> {
        override val clazz = ExceptionHandler::class
    }

    abstract fun handle(exception: Throwable, listener: Listener?, methode: KFunction<*>?)

    fun onMethodNotPublic(listener: Listener, method: KFunction<*>, visibility: KVisibility?) {}

    fun onMethodHasNoParameters(listener: Listener, method: KFunction<*>) {}

    fun onMethodHasNoDispatchableParameter(listener: Listener, method: KFunction<*>, type: KType) {}

    fun onMethodNotAccessible(listener: Listener, method: KFunction<*>) {}

    fun onMethodThrewUnexpectedException(listener: Listener, method: KFunction<*>, exception: Throwable) {}

    fun onUnexpectedExceptionDuringRegistration(listener: Listener, method: KFunction<*>, exception: Throwable) {}

    fun onMethodDidNotThrowConfiguredException(listener: Listener, method: KFunction<*>) {}

    object Empty : ExceptionHandler() {
        override fun handle(
            exception: Throwable,
            listener: Listener?,
            methode: KFunction<*>?,
        ) {}
    }
}