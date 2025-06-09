package com.fantamomo.kevent

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Configures an event handler with custom options.
 * 
 * This function serves two purposes in the event system:
 * 
 * 1. **Initialization phase**: When called with `event = null` during registration,
 *    it creates an [EventConfigurationScope], applies the configuration [block],
 *    and throws an [EventConfigurationHolder] exception containing the configuration.
 *    This exception is caught by the registration system to extract the configuration.
 * 
 * 2. **Runtime phase**: When called with a non-null event during normal execution,
 *    it simply verifies the event is non-null and returns, allowing the rest of the
 *    handler method to execute with the guarantee that `event` is not null.
 * 
 * The function uses Kotlin contracts to inform the compiler that if this function
 * returns normally (without throwing an exception), then `event` is guaranteed to be
 * non-null in the subsequent code.
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
 *     // At this point, event is guaranteed to be non-null
 *     println(event.someProperty)
 * }
 * ```
 * 
 * @param event The event being handled, which may be null during initialization
 * @param block A configuration block that sets options for this event handler
 * @throws ConfigurationCapturedException during initialization to pass configuration data
 * 
 * @see EventConfigurationScope
 * @see ConfigurationCapturedException
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
fun <E : Event> configuration(event: E?, @EventDsl block: EventConfigurationScope<E>.() -> Unit) {
    contract {
        returns() implies (event != null)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (event == null) {
        val scope = EventConfigurationScope<E>()
        scope.block()
        throw ConfigurationCapturedException(EventConfiguration(scope))
    }
}

/**
 * Configures an event handler with default options.
 * 
 * This function is similar to [configuration] but doesn't require a configuration block.
 * It's used when the event handler doesn't need any custom configuration and can use
 * the default settings.
 * 
 * Like [configuration], this function serves two purposes:
 * 
 * 1. **Initialization phase**: When called with `event = null`, it throws an
 *    [EventConfigurationHolder] with default configuration.
 * 
 * 2. **Runtime phase**: When called with a non-null event, it simply verifies the
 *    event is non-null and returns.
 * 
 * Example usage:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     emptyConfiguration(event)
 *     
 *     // At this point, event is guaranteed to be non-null
 *     println(event.someProperty)
 * }
 * ```
 * 
 * @param event The event being handled, which may be null during initialization
 * @throws ConfigurationCapturedException during initialization to pass default configuration data
 * 
 * @see configuration
 * @see ConfigurationCapturedException
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
fun emptyConfiguration(event: Event?) {
    contract { returns() implies (event != null) }
    if (event == null) throw ConfigurationCapturedException(EventConfiguration.DEFAULT)
}