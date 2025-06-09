package com.fantamomo.kevent

/**
 * Extension property for setting and getting the priority of an event handler.
 * 
 * This property provides a convenient way to access the [Priority] configuration
 * option in the event configuration DSL. It uses the predefined [Key.PRIORITY] key
 * to store and retrieve the priority value.
 * 
 * Example usage:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *     }
 * }
 * ```
 * 
 * The default priority is [Priority.Standard.NORMAL] if not explicitly set.
 * 
 * @see Priority
 * @see Key.PRIORITY
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
var <E : Event> EventConfigurationScope<E>.priority: Priority
    get() = getOrDefault(Key.PRIORITY)
    set(value) = set(Key.PRIORITY, value)