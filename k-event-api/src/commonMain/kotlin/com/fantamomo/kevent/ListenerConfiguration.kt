package com.fantamomo.kevent

/**
 * Represents a configuration provider for a listener.
 *
 * Listeners that implement this interface expose a default [EventConfiguration] that can be
 * used during event handler registration. This default configuration serves as a starting
 * point which can be further customized using the [L.configuration] DSL.
 *
 * ### Usage
 * ```kotlin
 * class MyListener : Listener, ListenerConfiguration {
 *     override val defaultConfiguration: EventConfiguration<Dispatchable> = createConfigurationScope {
 *         priority = Priority.LOWEST
 *     }
 * }
 * ```
 *
 * The [defaultConfiguration] is typically converted to an [EventConfigurationScope] during
 * registration to allow applying customizations via DSL blocks.
 *
 * @property defaultConfiguration The listener’s default [EventConfiguration] for events of type [Dispatchable].
 *
 * @see EventConfiguration
 * @see EventConfigurationScope
 * @see configuration
 * @author Fantamomo
 * @since 1.7-SNAPSHOT
 */
interface ListenerConfiguration {
    /**
     * Provides the default [EventConfiguration] for events of type [Dispatchable].
     *
     * This configuration acts as a baseline for setting up event handlers and can be further
     * customized when registering listeners. It is typically used to define initial
     * configuration properties such as priority and other metadata related to event handling.
     *
     * The [defaultConfiguration] serves as a standardized starting point and can be converted
     * into a mutable configuration scope to apply additional customizations.
     *
     * It will be also used when a `@Register` methode has no [configuration] or [emptyConfiguration].
     *
     * @see EventConfiguration
     * @see ListenerConfiguration
     */
    val defaultConfiguration: EventConfiguration<Dispatchable>
}
