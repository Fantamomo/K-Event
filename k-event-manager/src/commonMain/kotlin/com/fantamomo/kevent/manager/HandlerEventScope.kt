package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import kotlin.reflect.KClass

/**
 * Defines a scope for managing event handlers and listeners within the event system.
 *
 * The `HandlerEventScope` interface provides methods to register and unregister listeners
 * and event handlers, as well as manage the lifecycle of the scope. This scope is designed
 * to simplify the management of event handlers, allowing for their encapsulation and controlled
 * unbinding when no longer needed.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface HandlerEventScope {
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
     * Registers a simple event listener with the event system.
     *
     * This method adds an instance of a [SimpleListener] or [SimpleSuspendListener] to the event handling mechanism.
     * The provided listener will be notified of events it is configured to handle.
     *
     * @param listener The simple listener to register, responsible for handling a specific type of event.
     * @since 1.14-SNAPSHOT
     */
    fun register(listener: SimpleConfiguration<*>)

    /**
     * Unregisters an event listener from the event system.
     *
     * This method removes a previously registered [Listener] instance from the event system.
     * Once unregistered, the listener will no longer receive or handle dispatched events.
     *
     * @param listener The listener to be unregistered, previously registered with the event system.
     */
    fun unregister(listener: Listener)

    /**
     * Unregisters a simple event listener from the event system.
     *
     * This method removes a previously registered instance of [SimpleListener] or [SimpleSuspendListener] from the event system.
     * Once unregistered, the listener will no longer be notified or handle any dispatched events.
     *
     * @param listener The simple listener to be unregistered, previously registered with the event system.
     * @since 1.14-SNAPSHOT
     */
    fun unregister(listener: SimpleConfiguration<*>)

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
    ): RegisteredLambdaHandler

    /**
     * Registers a suspendable event handler with the event system.
     *
     * This method allows for asynchronous handler registration for a specific type of event.
     * The handler will be invoked whenever an instance of the specified event class is dispatched.
     * Additionally, a custom event configuration can be provided to modify event-specific behavior.
     *
     * @param E The type of event the handler will process. It must extend from [Dispatchable].
     * @param event The class of the event to register the handler for.
     * @param configuration The configuration for the event handler. Defaults to the result of [EventConfiguration.default].
     * @param handler The suspending function to be invoked when the event is dispatched.
     * @return An instance of [RegisteredLambdaHandler] that can be used to unregister the handler.
     *
     * @since 1.3-SNAPSHOT
     */
    fun <E : Dispatchable> registerSuspend(
        event: KClass<E>,
        configuration: EventConfiguration<E> = EventConfiguration.default(),
        handler: suspend (E) -> Unit,
    ): RegisteredLambdaHandler

    /**
     * Closes the `HandlerEventScope` and unregister its handlers.
     *
     * Once closed, the scope cannot be reused, and all
     * resources associated with it will be released.
     */
    fun close()
}