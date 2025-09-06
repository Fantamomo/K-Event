package com.fantamomo.kevent

/**
 * Marks a method in a [Listener] class as an event handler.
 *
 * ## Requirements
 * Methods annotated with [Register] must adhere to these rules:
 * 1. Must return [Unit] (void).
 * 2. Must accept **at least one parameter** that inherits from [Event].
 *    - The first parameter is the event itself.
 *    - If the event parameter is **nullable**, the method should start with either `configuration(event) { ... }` or `emptyConfiguration(event)`.
 *      During registration, the handler is invoked with `null` to capture the configuration.
 *    - If the event parameter is **non-nullable**, it is treated as if `emptyConfiguration(event)` is used automatically.
 * 3. Additional parameters **are allowed** and can be injected automatically via Parameter Injection.
 * 4. Must include a configuration block at the start (for nullable parameters) or rely on the default configuration (for non-nullable parameters).
 *
 * ## How It Works
 * During registration:
 * - Nullable event parameters trigger a call with `null` to capture configuration.
 * - Non-nullable event parameters skip configuration capture and act like `emptyConfiguration`.
 * - Additional injected parameters are ignored during registration but are provided at runtime when the event is dispatched.
 *
 * ## Examples
 * Example with nullable parameter (custom configuration):
 * ```kotlin
 * @Register
 * fun onMyEvent(event: MyEvent?, manager: EventManager) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *     }
 *     // At this point, `event` is guaranteed to be non-null
 *     println("Event received: ${event.id}")
 * }
 * ```
 *
 * Example with non-nullable parameter (default configuration):
 * ```kotlin
 * @Register
 * fun onOtherEvent(event: OtherEvent, logger: Logger) {
 *     // Behaves as if using `emptyConfiguration(event)`
 *     logger.info("Event received: $event")
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
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Register
