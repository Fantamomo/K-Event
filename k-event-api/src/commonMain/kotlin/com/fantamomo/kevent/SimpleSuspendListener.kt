package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Functional interface for asynchronously handling events within the dispatchable event system.
 *
 * This interface defines the contract for managing a single type of event that extends
 * [Dispatchable], using `suspend` functions to support asynchronous processing.
 * Implementing or instantiating this functional interface allows asynchronous handling
 * and optional configuration of events during their lifecycle.
 *
 * @param E The type of the event that this listener handles, extending [Dispatchable].
 *
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleSuspendListener<E : Dispatchable> {

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
     * Asynchronously handles an event of type [E].
     *
     * This method is intended to be called only by the `handle(E, Map<String, Any?>)` method.
     * Implementations that require additional arguments should throw an exception
     * when this method is called directly without them.
     *
     * @param event The event to be handled.
     */
    suspend fun handle(event: E)

    /**
     * Asynchronously handles an event of type [E] with additional arguments.
     *
     * This method allows the processing of an event while providing an optional map
     * of arguments that may influence the handling behavior. The default implementation
     * delegates to the simpler `handle(E)` method.
     *
     * @param event The event to be handled.
     * @param args A map of additional arguments that may influence event handling.
     */
    suspend fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)

    /**
     * Configures and returns an immutable [EventConfiguration] for the given event type.
     *
     * This method is invoked during the registration of an event listener to define the
     * configuration for handling events of type [E]. It creates a mutable configuration
     * scope, calls the [configure] function within that scope, and then returns an
     * immutable [EventConfiguration] instance.
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
     * is not supported by the default event system implementation.
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