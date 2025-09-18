package com.fantamomo.kevent

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmSynthetic

/**
 * Configures an event handler with custom options.
 *
 * This function plays two distinct roles in the event system:
 *
 * 1. **Initialization phase**
 *    When called with `event = null` (during handler registration), it creates an
 *    [EventConfigurationScope], executes the given [block], and throws a
 *    [ConfigurationCapturedException] containing the resulting [EventConfiguration].
 *    The registration system catches this exception to extract and store the configuration.
 *
 * 2. **Runtime phase**
 *    When called with a non-null event during normal execution, the function simply verifies
 *    that the event is non-null and then returns. From this point forward, the compiler
 *    and the developer can treat `event` as non-null.
 *
 * ### Example
 * ```kotlin
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *         // other configuration options
 *     }
 *
 *     // At this point, `event` is guaranteed to be non-null
 *     println(event.someProperty)
 * }
 * ```
 *
 * Kotlin contracts are used so the compiler knows:
 * if this function returns normally (without throwing), then `event` is guaranteed non-null.
 *
 * @param event The event being handled. May be `null` during registration, never `null` at runtime.
 * @param block A DSL block for customizing the event handler’s configuration.
 * @throws ConfigurationCapturedException During registration, carrying the captured configuration.
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
 * Configures a listener’s event handler with custom options using its default configuration.
 *
 * This extension function is intended for listeners that implement both [Listener] and
 * [ListenerConfiguration]. It provides a DSL to customize the handler’s configuration
 * and behaves differently depending on the phase of execution:
 *
 * 1. **Initialization phase (event = null)**
 *    - Converts the listener’s default configuration into an [EventConfigurationScope].
 *    - Executes the given [block] in the context of that scope.
 *    - Throws a [ConfigurationCapturedException] containing the resulting [EventConfiguration].
 *      The registration system intercepts this exception to store the configuration.
 *
 * 2. **Runtime phase (event != null)**
 *    - Simply returns without executing the block.
 *    - The Kotlin contract guarantees that after this call, `event` is non-null.
 *
 * ### Example
 * ```kotlin
 * class MyListener : Listener, ListenerConfiguration {
 *     override val defaultConfiguration: EventConfiguration<Dispatchable> = createConfigurationScope {
 *         ignoreStickyEvents = true
 *         exclusiveListenerProcessing = true
 *     }
 *
 *     @Register
 *     fun onMyEvent(event: MyEvent?) {
 *         configuration(event) {
 *             priority = Priority.LOWEST
 *             exclusiveListenerProcessing = false
 *         }
 *
 *         // At this point, `event` is guaranteed to be non-null
 *         println(event.someProperty)
 *     }
 * }
 * ```
 *
 * ### Kotlin Contracts
 * Contracts are used to inform the compiler that if the function returns normally,
 * `event` is non-null and that [block] is called at most once during initialization.
 *
 * @receiver The listener instance, which must implement both [Listener] and [ListenerConfiguration].
 * @param event The event being handled. May be `null` during registration, never `null` at runtime.
 * @param block A DSL block applied to the listener’s configuration scope for customizing options.
 * @throws ConfigurationCapturedException During initialization, carrying the captured configuration.
 *
 * @see EventConfigurationScope
 * @see ConfigurationCapturedException
 * @see configuration
 * @author Fantamomo
 * @since 1.7-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
@Throws(ConfigurationCapturedException::class)
@OptIn(ExperimentalContracts::class)
@JvmSynthetic
inline fun <E : Dispatchable, L> L.configuration(event: E?, @EventDsl block: EventConfigurationScope<E>.() -> Unit) where L : Listener, L : ListenerConfiguration {
    contract {
        returns() implies (event != null)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (event == null) {
        val scope = this.defaultConfiguration.toScope() as EventConfigurationScope<E>
        scope.block()
        throw ConfigurationCapturedException(EventConfiguration(scope))
    }
}
/**
 * Applies the listener’s default configuration to an event.
 *
 * This function is intended for listeners that implement both [Listener] and [ListenerConfiguration].
 * It ensures that the event can be safely used at runtime while providing the default
 * [EventConfiguration] during registration.
 *
 * The behavior depends on the phase:
 *
 * 1. **Initialization phase (event = null)**
 *    - Throws a [ConfigurationCapturedException] containing the listener’s [defaultConfiguration].
 *      This allows the registration system to capture and store the configuration.
 *
 * 2. **Runtime phase (event != null)**
 *    - Simply returns, guaranteeing via Kotlin contracts that the `event` is non-null
 *      and safe to use.
 *
 * ### Example
 * ```kotlin
 * class MyListener : Listener, ListenerConfiguration {
 *     override val defaultConfiguration: EventConfiguration<Dispatchable> = createConfigurationScope {
 *         priority = Priority.LOWEST
 *     }
 *
 *     @Register
 *     fun onMyEvent(event: MyEvent?) {
 *         this.defaultConfiguration(event)
 *         println(event.someProperty) // event is guaranteed non-null
 *     }
 *
 *     @Register
 *     fun thisMethodDoesTheSame(event: MyEvent) {
 *         println(event.someProperty)
 *     }
 * }
 * ```
 *
 * ### Kotlin Contracts
 * The contract declares that if this function returns normally, `event` is non-null.
 *
 * @receiver The listener instance implementing both [Listener] and [ListenerConfiguration].
 * @param event The event being handled. May be `null` during registration, never `null` at runtime.
 * @throws ConfigurationCapturedException During initialization, carrying the default configuration.
 *
 * @see ListenerConfiguration
 * @see EventConfiguration
 * @see ConfigurationCapturedException
 * @since 1.7-SNAPSHOT
 */
@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
@Throws(ConfigurationCapturedException::class)
@JvmSynthetic
inline fun <E : Dispatchable, L> L.defaultConfiguration(event: E?) where L : Listener, L : ListenerConfiguration {
    contract { returns() implies (event != null) }
    if (event == null) throw ConfigurationCapturedException(defaultConfiguration)
}

/**
 * Configures an event handler with default options.
 *
 * This function is a shorthand alternative to [configuration] when no custom
 * options are required. It applies the default [EventConfiguration].
 *
 * Like [configuration], it behaves differently depending on the phase:
 *
 * 1. **Initialization phase**
 *    If `event = null`, it throws a [ConfigurationCapturedException] containing
 *    the default configuration.
 *
 * 2. **Runtime phase**
 *    If `event` is non-null, the function simply returns, guaranteeing the event is safe to use.
 *
 * ### Example
 * ```kotlin
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     emptyConfiguration(event)
 *     println(event.someProperty) // event is guaranteed non-null
 * }
 * ```
 *
 * Equivalent and more idiomatic alternative:
 * ```kotlin
 * @Register
 * fun onMyEvent(event: MyEvent) {
 *     println(event.someProperty)
 * }
 * ```
 *
 * ### Recommendation
 * Prefer non-nullable event parameters in handler signatures whenever possible.
 * Use [emptyConfiguration] when you want explicit configuration, but in most cases
 * a non-null parameter makes the code cleaner.
 *
 * @param event The event being handled. May be `null` during registration.
 * @throws ConfigurationCapturedException During registration, carrying the default configuration.
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
    if (event == null) throw ConfigurationCapturedException(EventConfiguration)
}

/**
 * Throws a [ConfigurationCapturedException] encapsulating the event configuration
 * defined in the provided DSL block.
 *
 * This function is used during the initialization phase to capture a handler’s
 * configuration. The [block] is executed inside an [EventConfigurationScope],
 * producing an [EventConfiguration]. That configuration is then wrapped in a
 * [ConfigurationCapturedException] and thrown, so the registration system can
 * intercept and store it.
 *
 * @param block A DSL block executed inside an [EventConfigurationScope] that defines
 *              the handler’s configuration.
 * @throws ConfigurationCapturedException Always thrown with the configuration result.
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
 * Creates an [EventConfiguration] by applying a DSL block to a new [EventConfigurationScope].
 *
 * This function is the core builder for configurations. It instantiates a fresh
 * [EventConfigurationScope], runs the given [block] against it, and then produces
 * a finalized [EventConfiguration].
 *
 * @param block A DSL block executed with a new [EventConfigurationScope] as its receiver.
 * @return The resulting [EventConfiguration] containing the applied settings.
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