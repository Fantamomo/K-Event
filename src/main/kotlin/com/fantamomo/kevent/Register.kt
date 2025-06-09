package com.fantamomo.kevent

/**
 * Annotation used to mark methods in [Listener] classes as event handlers.
 * 
 * Methods annotated with [Register] must adhere to the following strict requirements:
 * 1. Must return [Unit] (void).
 * 2. Must accept exactly one nullable parameter of a type that inherits from [Event].
 * 3. Must begin with either `configuration(event) { ... }` or `emptyConfiguration(event)`.
 * 
 * During the registration process, each annotated method is invoked with `null` as the event parameter.
 * This triggers a special [ConfigurationCapturedException] exception that carries the configuration data
 * defined in the method's configuration block.
 * 
 * Example usage:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *         // Other configuration options
 *     }
 *     
 *     // Event handling code (only executed when event is not null)
 * }
 * ```
 * 
 * @see Listener
 * @see Event
 * @see configuration
 * @see emptyConfiguration
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Register
