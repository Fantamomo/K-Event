package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents a simple configuration interface for event listeners.
 *
 * The `SimpleConfiguration` interface allows defining configurations for handling
 * events of type [E], which extend [Dispatchable]. It provides mechanisms to
 * specify event types, configure event handling behavior, and map listener arguments.
 *
 * @param E The type of event this configuration applies to, extending [Dispatchable].
 * @author Fantamomo
 * @since 1.6-SNAPSHOT
 */
sealed interface SimpleConfiguration<E : KEventElement> {

    /**
     * The type of event handled by this listener.
     *
     * Determines the specific event class that the listener is configured to handle.
     * If null, the listener will attempt to infer the type from the event instance
     * passed to the handler, using the event's runtime type.
     *
     * This is useful when the event type should be explicitly defined or
     * dynamically determined during execution.
     *
     * @return The Kotlin class of the event type [E], or null to infer it at runtime.
     */
    val type: KClass<E>?
        get() = null

    /**
     * Configures and returns an immutable [EventConfiguration] for the listener.
     *
     * This method is invoked during listener registration. It creates a mutable
     * [EventConfigurationScope], applies the configuration via [configure], and
     * then returns a finalized, immutable [EventConfiguration].
     *
     * @return An immutable [EventConfiguration] representing the listener's configuration.
     */
    fun configuration(): EventConfiguration<E> {
        val scope = EventConfigurationScope<E>()
        scope.configure()
        return EventConfiguration(scope)
    }

    /**
     * Defines the event handling configuration within a given [EventConfigurationScope].
     *
     * This function is intended to be called internally by [configuration] to set
     * up listener-specific options and properties for events of type [E].
     * Direct invocation outside of this process is not supported.
     *
     * @receiver The [EventConfigurationScope] used to define event handling options.
     */
    fun EventConfigurationScope<E>.configure() {}

    /**
     * Provides a mapping of listener argument names to their respective types.
     *
     * By default, this returns an empty map. Implementations can override this
     * to specify required argument names and types for event processing.
     *
     * @return A map where keys are argument names (String) and values are their types ([KClass]).
     */
    fun args(): Map<String, KClass<*>> = emptyMap()
}