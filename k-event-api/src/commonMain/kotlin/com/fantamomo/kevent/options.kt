package com.fantamomo.kevent

/**
 * Internal property to cast the current [EventConfigurationScope] to an [EventConfiguration].
 *
 * This property is only visible within this file and is used to access implementation details
 * of [EventConfiguration] when working with an [EventConfigurationScope].
 *
 * @receiver The [EventConfigurationScope] instance to cast.
 * @return The [EventConfiguration] instance.
 * @param D The type of [Dispatchable] the configuration applies to.
 *
 * @since 1.0-SNAPSHOT
 */
private inline val <D : Dispatchable> EventConfigurationScope<D>.internal
    get() = this as EventConfiguration<D>

/**
 * Retrieves the configured [Priority] for the current [EventConfiguration].
 *
 * If no priority is explicitly set, this will return [Priority.Standard.NORMAL] by default.
 *
 * @receiver The [EventConfiguration] to read from.
 * @return The configured [Priority], or the default if none is set.
 *
 * @see Priority
 * @see Key.PRIORITY
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.priority: Priority
    get() = getOrDefault(Key.PRIORITY)

/**
 * Extension property for setting and getting the priority of an event handler.
 *
 * This property provides a convenient way to access the [Priority] configuration
 * option in the event configuration DSL. It uses the predefined [Key.PRIORITY] key
 * to store and retrieve the priority value.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     priority = Priority.Standard.HIGH
 * }
 * ```
 *
 * **Default:** [Priority.Standard.NORMAL]
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property priority The priority level for the event handler.
 *
 * @see Priority
 * @see Key.PRIORITY
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.priority: Priority
    get() = internal.priority
    set(value) = set(Key.PRIORITY, value)

/**
 * Retrieves whether subtypes of the event are disallowed in this [EventConfiguration].
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if subtypes are disallowed, `false` otherwise.
 *
 * @see Key.DISALLOW_SUBTYPES
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.disallowSubtypes: Boolean
    get() = getOrDefault(Key.DISALLOW_SUBTYPES)

/**
 * Determines whether subtypes of the event are disallowed for event handling.
 *
 * If set to `true`, handlers will only respond to the exact event type and will ignore
 * events derived from that type. By default, this property is set to `false`, allowing
 * handlers to process subtypes of the event.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     disallowSubtypes = true
 * }
 * ```
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property disallowSubtypes Whether subtypes of the event are disallowed.
 *
 * @see Key.DISALLOW_SUBTYPES
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.disallowSubtypes: Boolean
    get() = internal.disallowSubtypes
    set(value) = set(Key.DISALLOW_SUBTYPES, value)

/**
 * Retrieves whether the listener is configured for exclusive processing.
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if exclusive processing is enabled, otherwise `false`.
 *
 * @see Key.EXCLUSIVE_LISTENER_PROCESSING
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.exclusiveListenerProcessing: Boolean
    get() = getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)

/**
 * Specifies whether a listener should process events exclusively.
 *
 * When this property is set to `true`, the associated listener will not process
 * a new event if it is still actively handling a previous one. This ensures sequential
 * processing and prevents overlapping execution.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     exclusiveListenerProcessing = true
 * }
 * ```
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property exclusiveListenerProcessing Whether exclusive listener processing is enabled.
 *
 * @see Key.EXCLUSIVE_LISTENER_PROCESSING
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.exclusiveListenerProcessing: Boolean
    get() = internal.exclusiveListenerProcessing
    set(value) = set(Key.EXCLUSIVE_LISTENER_PROCESSING, value)

/**
 * Retrieves whether the listener is marked as silent.
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if the listener is silent, otherwise `false`.
 *
 * @see Key.SILENT
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.silent: Boolean
    get() = getOrDefault(Key.SILENT)

/**
 * Marks whether a listener is considered "silent".
 *
 * When `true`, the listener will not prevent a [DeadEvent] from being dispatched
 * if it is the only listener for a given event. This allows passive observers,
 * loggers, or debug tools to listen to all events without affecting
 * [DeadEvent] suppression.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     silent = true
 * }
 * ```
 *
 * **Default:** `false`
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property silent Whether the listener is considered silent.
 *
 * @see Key.SILENT
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.silent: Boolean
    get() = internal.silent
    set(value) = set(Key.SILENT, value)

/**
 * Retrieves whether sticky events should be ignored.
 *
 * **Default:** Determined by [Key.IGNORE_STICKY_EVENTS].
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if sticky events are ignored, otherwise `false`.
 *
 * @see Key.IGNORE_STICKY_EVENTS
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.ignoreStickyEvents: Boolean
    get() = getOrDefault(Key.IGNORE_STICKY_EVENTS)

/**
 * Configures whether sticky events should be ignored for the current event handler configuration.
 *
 * Sticky events are events that have been dispatched before the handler was registered
 * and are typically replayed to new handlers. When `true`, the event handler will not
 * process any previously dispatched sticky events.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     ignoreStickyEvents = true
 * }
 * ```
 *
 * **Default:** See [Key.IGNORE_STICKY_EVENTS]
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property ignoreStickyEvents Whether sticky events should be ignored.
 *
 * @see Key.IGNORE_STICKY_EVENTS
 * @since 1.3-SNAPSHOT
 */
var EventConfigurationScope<*>.ignoreStickyEvents: Boolean
    get() = internal.ignoreStickyEvents
    set(value) = set(Key.IGNORE_STICKY_EVENTS, value)

/**
 * Retrieves the configured name for the listener.
 *
 * **Default:** `null`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return The configured name, or `null` if none is set.
 *
 * @see Key.NAME
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.name: String?
    get() = get(Key.NAME)

/**
 * Sets or retrieves the name of a listener within the event configuration.
 *
 * This property allows assigning a human-readable identifier to a listener,
 * which can be helpful for debugging, logging, or integration with other systems.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     name = "PlayerJoinListener"
 * }
 * ```
 *
 * **Default:** `null`
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property name The human-readable identifier for the listener.
 *
 * @see Key.NAME
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.name: String?
    get() = internal.name
    set(value) = set(Key.NAME, value)
