package com.fantamomo.kevent

/**
 * Internal property to cast an [EventConfigurationScope] to an [EventConfiguration].
 *
 * Only visible within this file, used to access implementation details of [EventConfiguration].
 *
 * @receiver The [EventConfigurationScope] instance to cast.
 * @return The [EventConfiguration] instance.
 * @param D The type of [Dispatchable] the configuration applies to.
 * @since 1.0-SNAPSHOT
 */
private inline val <D : Dispatchable> EventConfigurationScope<D>.internal
    get() = this as EventConfiguration<D>

/**
 * Retrieves the configured [Priority] for an [EventConfiguration].
 *
 * Defaults to [Priority.Standard.NORMAL] if not explicitly set.
 *
 * @receiver The [EventConfiguration] to read from.
 * @return The configured [Priority], or the default if none is set.
 * @see Priority
 * @see Key.PRIORITY
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.priority: Priority
    get() = getOrDefault(Key.PRIORITY)

/**
 * Extension property to get or set the priority of an event handler.
 *
 * Uses [Key.PRIORITY] to store and retrieve the value within the configuration DSL.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     priority = Priority.Standard.HIGH
 * }
 * ```
 *
 * Default: [Priority.Standard.NORMAL]
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property priority The priority level for the event handler.
 * @see Key.PRIORITY
 * @see Priority
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.priority: Priority
    get() = internal.priority
    set(value) = set(Key.PRIORITY, value)

/**
 * Retrieves whether subtypes of the event are disallowed.
 *
 * Default: `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if subtypes are disallowed, `false` otherwise.
 * @see Key.DISALLOW_SUBTYPES
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.disallowSubtypes: Boolean
    get() = getOrDefault(Key.DISALLOW_SUBTYPES)

/**
 * Specifies whether subtypes of the event are disallowed for handling.
 *
 * If `true`, handlers will only respond to the exact event type.
 * Default: `false`, allowing subtypes to be handled.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     disallowSubtypes = true
 * }
 * ```
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property disallowSubtypes Whether subtypes are disallowed.
 * @see Key.DISALLOW_SUBTYPES
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.disallowSubtypes: Boolean
    get() = internal.disallowSubtypes
    set(value) = set(Key.DISALLOW_SUBTYPES, value)

/**
 * Retrieves whether the listener is configured for exclusive processing.
 *
 * Default: `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if exclusive processing is enabled, otherwise `false`.
 * @see Key.EXCLUSIVE_LISTENER_PROCESSING
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.exclusiveListenerProcessing: Boolean
    get() = getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)

/**
 * Specifies whether a listener should process events exclusively.
 *
 * When `true`, the listener will not handle a new event while actively processing another.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     exclusiveListenerProcessing = true
 * }
 * ```
 *
 * Default: `false`
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property exclusiveListenerProcessing Whether exclusive listener processing is enabled.
 * @see Key.EXCLUSIVE_LISTENER_PROCESSING
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.exclusiveListenerProcessing: Boolean
    get() = internal.exclusiveListenerProcessing
    set(value) = set(Key.EXCLUSIVE_LISTENER_PROCESSING, value)

/**
 * Retrieves whether the listener is marked as silent.
 *
 * Default: `false`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if the listener is silent, otherwise `false`.
 * @see Key.SILENT
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.silent: Boolean
    get() = getOrDefault(Key.SILENT)

/**
 * Marks whether a listener is considered silent.
 *
 * When `true`, the listener does not prevent [DeadEvent] dispatch,
 * allowing observers, loggers, or debug tools to listen without affecting processing.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     silent = true
 * }
 * ```
 *
 * Default: `false`
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property silent Whether the listener is silent.
 * @see Key.SILENT
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.silent: Boolean
    get() = internal.silent
    set(value) = set(Key.SILENT, value)

/**
 * Retrieves whether sticky events should be ignored.
 *
 * Default: determined by [Key.IGNORE_STICKY_EVENTS].
 *
 * @receiver The [EventConfiguration] to read from.
 * @return `true` if sticky events are ignored, otherwise `false`.
 * @see Key.IGNORE_STICKY_EVENTS
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.ignoreStickyEvents: Boolean
    get() = getOrDefault(Key.IGNORE_STICKY_EVENTS)

/**
 * Configures whether sticky events should be ignored for this event handler.
 *
 * Sticky events are previously dispatched events replayed to new handlers.
 * When `true`, these events are ignored.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     ignoreStickyEvents = true
 * }
 * ```
 *
 * Default: See [Key.IGNORE_STICKY_EVENTS]
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property ignoreStickyEvents Whether sticky events are ignored.
 * @see Key.IGNORE_STICKY_EVENTS
 * @since 1.3-SNAPSHOT
 */
var EventConfigurationScope<*>.ignoreStickyEvents: Boolean
    get() = internal.ignoreStickyEvents
    set(value) = set(Key.IGNORE_STICKY_EVENTS, value)

/**
 * Retrieves the configured name for the listener.
 *
 * Default: `null`
 *
 * @receiver The [EventConfiguration] to read from.
 * @return The configured name, or `null` if none.
 * @see Key.NAME
 * @since 1.4-SNAPSHOT
 */
val EventConfiguration<*>.name: String?
    get() = get(Key.NAME)

/**
 * Sets or retrieves the name of a listener in the configuration.
 *
 * Assigns a human-readable identifier for debugging, logging, or integration purposes.
 *
 * Example:
 * ```
 * configuration(myEvent) {
 *     name = "PlayerJoinListener"
 * }
 * ```
 *
 * Default: `null`
 *
 * @receiver The [EventConfigurationScope] this property belongs to.
 * @property name The human-readable identifier.
 * @see Key.NAME
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.name: String?
    get() = internal.name
    set(value) = set(Key.NAME, value)
