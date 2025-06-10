package com.fantamomo.kevent

/**
 * Extension property for setting and getting the priority of an event handler.
 *
 * This property provides a convenient way to access the [Priority] configuration
 * option in the event configuration DSL. It uses the predefined [Key.PRIORITY] key
 * to store and retrieve the priority value.
 *
 * The default priority is [Priority.Standard.NORMAL] if not explicitly set.
 *
 * @see Priority
 * @see Key.PRIORITY
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
var EventConfigurationScope<*>.priority: Priority
    get() = getOrDefault(Key.PRIORITY)
    set(value) = set(Key.PRIORITY, value)

/**
 * Property to determine whether subtypes of the event are disallowed for event handling.
 *
 * This property can be used to configure event handlers to restrict them from handling
 * subtypes of a specific event. If set to `true`, handlers will only respond to the exact
 * event type and will ignore events derived from that type. By default, this property is
 * set to `false`, allowing handlers to process subtypes of the event.
 *
 * @receiver The [EventConfigurationScope] that this property belongs to.
 * @property disallowSubtypes Determines whether subtypes of the event are disallowed.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 *
 * @see Priority
 *
 * @see Key.DISALLOW_SUBTYPES
 */
var EventConfigurationScope<*>.disallowSubtypes: Boolean
    get() = getOrDefault(Key.DISALLOW_SUBTYPES)
    set(value) = set(Key.DISALLOW_SUBTYPES, value)

/**
 * Specifies whether a listener should process events exclusively.
 *
 * When this property is set to `true`, the associated listener will not process
 * a new event if it is still actively handling a previous one. This ensures that
 * the listener processes events sequentially, preventing overlapping or concurrent
 * execution. If a new event occurs while the listener is busy, that event will be
 * ignored for the duration of the listener's processing, and it will not be
 * processed later once the listener becomes available.
 *
 * This option is useful for listeners that may call this Event, due API or other.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 *
 * @see Key.EXCLUSIVE_LISTENER_PROCESSING
 */
var EventConfigurationScope<*>.exclusiveListenerProcessing: Boolean
    get() = getOrDefault(Key.EXCLUSIVE_LISTENER_PROCESSING)
    set(value) = set(Key.EXCLUSIVE_LISTENER_PROCESSING, value)