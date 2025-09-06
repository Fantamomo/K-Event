package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents a simple configuration interface for event listeners in an event system.
 *
 * The `SimpleConfiguration` interface is designed to define and provide configurations
 * for handling events of type [E], which extend [Dispatchable]. It offers functionality
 * for setting up event-specific configurations, retrieving argument mappings, and defining
 * the type of events handled by the listener.
 *
 * @param E The type of event that this configuration applies to, extending [Dispatchable].
 * @author Fantamomo
 * @since 1.6-SNAPSHOT
 */
sealed interface SimpleConfiguration<E : Dispatchable> {
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