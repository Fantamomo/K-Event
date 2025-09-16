package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.*
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
    private val listeners: MutableList<Any> = mutableListOf()

    override fun register(listener: Listener) {
        checkClosed()
        listeners.add(listener)
        parent.register(listener)
    }

    override fun register(listener: SimpleConfiguration<*>) {
        checkClosed()
        listeners.add(listener)
        parent.register(listener)
    }

    override fun unregister(listener: Listener) {
        checkClosed()
        listeners.remove(listener)
        parent.unregister(listener)
    }

    override fun unregister(listener: SimpleConfiguration<*>) {
        checkClosed()
        listeners.remove(listener)
        parent.unregister(listener)
    }

    override fun <E : KEventElement> register(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        return parent.register(event, configuration, handler).also { listeners.add(it) }
    }

    override fun <E : KEventElement> registerSuspend(
        event: KClass<E>,
        configuration: EventConfiguration<E>,
        handler: suspend (E) -> Unit,
    ): RegisteredLambdaHandler {
        checkClosed()
        return parent.registerSuspend(event, configuration, handler).also { listeners.add(it) }
    }

    override fun close() {
        checkClosed()
        isClosed = true
        listeners.forEach(::unregister)
        listeners.clear()
    }

    private fun unregister(listener: Any): Unit = when (listener) {
        is RegisteredLambdaHandler -> listener.unregister()
        is Listener -> parent.unregister(listener)
        is SimpleListener<*> -> parent.unregister(listener)
        is SimpleSuspendListener<*> -> parent.unregister(listener)
        else -> {}
    }

    private fun checkClosed() {
        if (isClosed)
            throw IllegalStateException("HandlerEventScope is closed")
    }
}