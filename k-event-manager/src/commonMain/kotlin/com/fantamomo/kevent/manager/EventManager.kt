package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import kotlin.reflect.KClass

/**
 * Interface for managing event listeners and dispatching events in the event system.
 *
 * This interface provides methods for registering event listeners, dispatching events
 * to the registered listeners, and registering event-specific handlers with configuration.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface EventManager {
    /**
     * Registers an event listener with the event system.
     *
     * This method allows adding a class implementing the [Listener] interface
     * to the event handling system. The event system will scan the listener
     * for annotated methods (with [Register]) and register them as event handlers.
     *
     * @param listener The listener to be registered, containing methods annotated
     *                 to handle specific events.
     *
     * @see Register
     * @see Listener
     */
    fun register(listener: Listener)

    /**
     * Dispatches an event to all registered event listeners.
     *
     * This method delivers the given [event] to all listeners or handlers that are subscribed
     * to the event type or any of its supertypes. The event is processed synchronously,
     * ensuring that each listener is invoked before continuing execution.
     *
     * @param event The event instance to be dispatched to the appropriate listeners.
     */
    fun dispatch(event: Dispatchable)

    /**
     * Dispatches an event to all registered event listeners asynchronously.
     *
     * This suspend function delivers the given [event] to all applicable listeners
     * or handlers subscribed to the event type or its supertypes. Unlike its synchronous
     * counterpart, this method allows asynchronous processing within a coroutine
     * context, enabling non-blocking or delayed execution of event handling logic.
     *
     * @param event The event instance to be dispatched to the appropriate listeners.
     */
    suspend fun dispatchSuspend(event: Dispatchable)

    /**
     * Registers an event-specific handler with its configuration.
     *
     * This method allows for registering a handler for a specific type of event. The handler
     * will be invoked whenever an instance of the specified event class is dispatched. Additionally,
     * a custom event configuration can be provided to modify event-specific behavior.
     *
     * @param E The type of event the handler will process.
     * @param event The class of the event to register the handler for.
     * @param configuration The configuration for the event handler. Defaults to the result of [EventConfiguration.default].
     * @param handler The function to be invoked when the event is dispatched.
     *
     * @see createConfigurationScope
     * @see EventConfigurationScope
     */
    fun <E : Dispatchable> register(
        event: KClass<E>,
        configuration: EventConfiguration<E> = EventConfiguration.default(),
        handler: (E) -> Unit,
    )

    fun unregister(listener: Listener)
}