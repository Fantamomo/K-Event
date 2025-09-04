package com.fantamomo.kevent

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmSynthetic

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
 * non-null in the following code.
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
@Throws(ConfigurationCapturedException::class)
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
inline fun <E : Dispatchable> configuration(event: E?, @EventDsl block: EventConfigurationScope<E>.() -> Unit) {
    contract {
        returns() implies (event != null)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (event == null) {
        throwConfigurationScope(block)
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
 * The next example does the same as above:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *
 *     println(event.someProperty)
 * }
 * ```
 *
 * You can remove the `emptyConfiguration` call when you make the parameter non-null.
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
@Suppress("NOTHING_TO_INLINE")
@Throws(ConfigurationCapturedException::class)
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
inline fun emptyConfiguration(event: Dispatchable?) {
    contract { returns() implies (event != null) }
    if (event == null) throw ConfigurationCapturedException(EventConfiguration.DEFAULT)
}

/**
 * Throws a [ConfigurationCapturedException] encapsulating the event configuration defined
 * in the provided DSL block.
 *
 * This method is primarily used in the event system for capturing the event configuration
 * during the initialization process. The configuration is specified within the scope of
 * the [EventConfigurationScope], using a DSL syntax. When invoked, this method executes
 * the given block, encapsulates the resulting configuration in a [ConfigurationCapturedException],
 * and throws it. This exception is later caught by the event registration system to extract
 * and associate the configuration with the respective event handler.
 *
 * @param block The DSL block defining the event configuration. The block is executed
 *              within an instance of [EventConfigurationScope] specific to the event type [E].
 * @return This method does not return a value as it always throws a [ConfigurationCapturedException].
 * @throws ConfigurationCapturedException This exception is thrown carrying the configuration data
 *                                         created within the provided [block].
 *
 * @author Fantamomo
 * @since 1.1-SNAPSHOT
 */
@Throws(ConfigurationCapturedException::class)
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
inline fun <E : Dispatchable> throwConfigurationScope(block: @EventDsl EventConfigurationScope<E>.() -> Unit): Nothing {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    throw ConfigurationCapturedException(createConfigurationScope(block))
}

/**
 * Creates a configuration scope for an event and applies the given configuration block.
 *
 * This function initializes an [EventConfigurationScope], executes the provided
 * configuration [block] within that scope, and then returns an [EventConfiguration]
 * containing the configured data.
 *
 * @param block A lambda that defines the configuration for the event. The block
 *              is executed with the newly created [EventConfigurationScope] as the receiver.
 * @return An [EventConfiguration] containing the configuration data set within the scope.
 *
 * @see configuration
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
inline fun <E : Dispatchable> createConfigurationScope(block: @EventDsl EventConfigurationScope<E>.() -> Unit): EventConfiguration<E> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val scope = EventConfigurationScope<E>()
    scope.block()
    return EventConfiguration(scope)
}