package com.fantamomo.kevent

/**
 * Exception used internally to capture and transfer event configuration data
 * during the initialization process.
 *
 * This class plays a central role in the event system's setup mechanism:
 *
 * 1. When a method annotated with [Register] is registered, it is first invoked
 *    with `null` as the event parameter.
 *
 * 2. Inside the method, calling [configuration] or [emptyConfiguration] detects
 *    the null event and throws this exception, embedding the configuration data.
 *
 * 3. The registration system catches this exception and retrieves the
 *    [configuration], linking it to the event handler.
 *
 * This mechanism allows event handlers to declare their configuration in a
 * natural, DSL-like style, while still making the configuration data accessible
 * to the registration process.
 *
 * @property configuration The captured configuration data for the event handler.
 *
 * @see Register
 * @see configuration
 * @see emptyConfiguration
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class ConfigurationCapturedException(configuration: EventConfiguration<*>) : Throwable {
    val configuration: EventConfiguration<*>
}