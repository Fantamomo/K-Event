package com.fantamomo.kevent

/**
 * Marker interface for classes that define event listener methods.
 *
 * Classes implementing this interface can provide methods annotated with [Register]
 * to handle events. The event system scans these classes for such methods and
 * registers them as event handlers.
 *
 * Example usage:
 * ```
 * class MyListener : Listener {
 *     @Register
 *     fun onMyEvent(event: MyEvent?) {
 *         configuration(event) {
 *             // Configure event handling options
 *             priority = Priority.HIGH
 *         }
 *
 *         // Handle the event
 *     }
 * }
 * ```
 *
 * This interface does not define any methods itself; it simply marks a class
 * as containing event handlers.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface Listener