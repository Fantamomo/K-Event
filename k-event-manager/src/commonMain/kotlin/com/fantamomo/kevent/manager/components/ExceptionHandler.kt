package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener

abstract class ExceptionHandler : EventManagerComponent<ExceptionHandler> {
    override val key: EventManagerComponent.Key<ExceptionHandler>
        get() = Key

    companion object Key : EventManagerComponent.Key<ExceptionHandler> {
        override val clazz = ExceptionHandler::class
    }

    abstract fun handle(exception: Throwable, listener: Listener?, methode: (Dispatchable) -> Unit)

    object Empty : ExceptionHandler() {
        override fun handle(
            exception: Throwable,
            listener: Listener?,
            methode: (Dispatchable) -> Unit,
        ) {}
    }
}