package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventConfiguration
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.manager.EventManager
import com.fantamomo.kevent.manager.HandlerEventScope
import com.fantamomo.kevent.manager.RegisteredLambdaHandler
import kotlin.reflect.KClass

class HandlerEventScopeImpl(val eventManager: EventManager) : HandlerEventScope {
    private var isClosed = false
    private val listeners: MutableList<Listener> = mutableListOf()
    private val lambdas: MutableList<RegisteredLambdaHandler> = mutableListOf()

    override fun register(listener: Listener) {
        checkClosed()
        listeners.add(listener)
        eventManager.register(listener)
    }

    override fun unregister(listener: Listener) {
        checkClosed()
        listeners.remove(listener)
        eventManager.unregister(listener)
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        return eventManager.register(event, configuration, handler).also { lambdas.add(it) }
    }

    override fun close() {
        checkClosed()
        isClosed = true
        listeners.forEach(eventManager::unregister)
        listeners.clear()
        lambdas.forEach(RegisteredLambdaHandler::unregister)
        lambdas.clear()
    }

    private fun checkClosed() {
        if(isClosed)
            throw IllegalStateException("HandlerEventScope is closed")
    }
}