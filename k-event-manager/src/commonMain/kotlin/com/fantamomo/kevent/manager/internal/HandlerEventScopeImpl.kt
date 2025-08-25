package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventConfiguration
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.manager.HandlerEventScope
import com.fantamomo.kevent.manager.RegisteredLambdaHandler
import kotlin.reflect.KClass

/**
 * Implementation of the [HandlerEventScope] interface for managing event handlers and listeners.
 *
 * This class provides a concrete implementation of [HandlerEventScope], allowing
 * secure registration, unregistration, and lifecycle management of event listeners
 * and handlers. A `HandlerEventScopeImpl` instance enforces proper scoping and cleanup
 * to ensure no leftover associations or leaks after the scope is closed.
 *
 * @constructor Creates a new `HandlerEventScopeImpl` instance with a specified parent scope.
 * @param parent The parent [HandlerEventScope] to coordinate listener and handler registration.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class HandlerEventScopeImpl(val parent: HandlerEventScope) : HandlerEventScope {
    private var isClosed = false
    private val listeners: MutableList<Listener> = mutableListOf()
    private val lambdas: MutableList<RegisteredLambdaHandler> = mutableListOf()

    override fun register(listener: Listener) {
        checkClosed()
        listeners.add(listener)
        parent.register(listener)
    }

    override fun unregister(listener: Listener) {
        checkClosed()
        listeners.remove(listener)
        parent.unregister(listener)
    }

    override fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        return parent.register(event, configuration, handler).also { lambdas.add(it) }
    }

    override fun close() {
        checkClosed()
        isClosed = true
        listeners.forEach(parent::unregister)
        listeners.clear()
        lambdas.forEach(RegisteredLambdaHandler::unregister)
        lambdas.clear()
    }

    private fun checkClosed() {
        if(isClosed)
            throw IllegalStateException("HandlerEventScope is closed")
    }
}