package com.fantamomo.kevent

/**
 * Special exception used to transport event configuration data during the initialization process.
 * 
 * This class is a key part of the event system's initialization mechanism:
 * 
 * 1. When a method annotated with [Register] is being registered, it is invoked with `null`
 *    as the event parameter.
 * 
 * 2. The method calls either [configuration] or [emptyConfiguration], which detects the
 *    null event and throws this exception containing the configuration data.
 * 
 * 3. The registration system catches this exception and extracts the [configuration] to
 *    associate it with the event handler.
 * 
 * This approach allows event handlers to define their configuration in a natural, DSL-style
 * syntax while still making the configuration data available to the registration system.
 * 
 * @property configuration The configuration data for the event handler
 * 
 * @see Register
 * @see configuration
 * @see emptyConfiguration
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class EventConfigurationHolder(val configuration: EventConfiguration<*>) : RuntimeException()