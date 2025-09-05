package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Functional interface for handling events within the dispatchable event system.
 *
 * This interface defines the contract for managing a single type of event that extends
 * the [Dispatchable] class. Implementing or instantiating this functional interface
 * allows the handling and optional configuration of events during their lifecycle.
 *
 * @param E The type of the event that this listener handles, extending [Dispatchable].
 *
 * @author Fantamomo
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleListener<E : Dispatchable> {

    /**
     * Represents the type of event handled by this listener.
     *
     * This property determines the specific event type that the listener is configured to handle.
     * If this is null, the listener will attempt to deduce the type from the event parameter passed
     * to the `handle` method. This fallback mechanism relies on the event's runtime type to ensure
     * compatible processing.
     *
     * It is intended to be used in scenarios where the event type needs to be explicitly defined
     * or derived dynamically during listener execution.
     *
     * @return The Kotlin class representing of the event type [E], or null if it should be deduced.
     */
    val type: KClass<E>?
        get() = null
    /**
     * Handles an event of type [E].
     *
     * This method is intended to be called only by the `handle(E, Map<String, Any?>)` method.
     * Implementations that require additional arguments should throw an exception
     * when this method is called directly without them.
     *
     * @param event The event to be handled.
     */
    fun handle(event: E)

    /**
     * Handles an event of type [E] with additional arguments.
     *
     * This method allows the processing of an event, supplying an optional map
     * of arguments that might influence the handling behavior. The arguments
     * map can be used to pass configuration or data specific to the event
     * being handled. In the default implementation, this method delegates
     * to the simpler `handle(E)` method for processing.
     *
     * @param event The event to be handled.
     * @param args A map of additional arguments that may influence event handling.
     */
    fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)

    /**
     * Configures and returns an immutable [EventConfiguration] for the given event type.
     *
     * This method is invoked during the registration of an event listener to define the
     * configuration for handling events of type [E]. It creates a mutable configuration
     * scope, allows customization through the scope's [configure] function, and returns
     * an immutable [EventConfiguration] instance.
     *
     * @return An immutable [EventConfiguration] instance containing the finalized configuration
     *         for handling events of type [E].
     */
    fun configuration(): EventConfiguration<E> {
        val scope = EventConfigurationScope<E>()
        scope.configure()
        return EventConfiguration(scope)
    }

    /**
     * Configures the event handling behavior within the given [EventConfigurationScope].
     *
     * This method is used internally during the creation of an event configuration
     * to define specific settings or properties for handling events of type [E].
     * It operates within the scope provided by [EventConfigurationScope], allowing
     * customization of the event handling logic.
     *
     * This method should only be invoked by the [configuration] function. Direct invocation
     * of this method is not supported by the default event system implementation.
     *
     * @receiver The [EventConfigurationScope] in which the event handling configuration is defined.
     */
    fun EventConfigurationScope<E>.configure() {}

    /**
     * Provides a map of argument names and their corresponding types required for event handling.
     *
     * This method returns an empty map by default, serving as a base for specific implementations
     * where arguments for event processing need to be defined.
     *
     * @return A map where the keys are argument names as strings, and the values are the corresponding argument types as [KClass].
     */
    fun args(): Map<String, KClass<*>> = emptyMap()
}