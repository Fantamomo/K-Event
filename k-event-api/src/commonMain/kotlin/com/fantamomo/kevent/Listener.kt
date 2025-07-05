package com.fantamomo.kevent

/**
 * A marker interface used to identify classes that contain event listener methods.
 * 
 * Classes implementing this interface can define methods annotated with [Register]
 * to handle events. The event system will scan these classes for methods with the
 * [Register] annotation and register them as event handlers.
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
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface Listener