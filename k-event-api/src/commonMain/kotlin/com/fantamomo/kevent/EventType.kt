package com.fantamomo.kevent

/**
 * Marker interface for event interfaces that can be used as listener targets.
 *
 * By extending [EventType], an interface can be treated as an event category.
 * Any [Dispatchable] event class that implements such an interface will be
 * delivered to listeners registered for the interface.
 *
 * ### Example
 *
 * ```kotlin
 * // Define an event interface
 * interface UserEvent : EventType {
 *     val userId: String
 * }
 *
 * // Two concrete events
 * class UserJoinedEvent(override val userId: String, val username: String) : Event(), UserEvent
 * class UserLeftEvent(override val userId: String) : Event(), UserEvent
 *
 * // Listener that reacts to all UserEvents
 * class UserListener : Listener {
 *     @Register
 *     fun onAnyUserEvent(event: UserEvent) {
 *         println("User event: ${event.userId}")
 *     }
 * }
 * ```
 *
 * In this example, both `UserJoinedEvent` and `UserLeftEvent` trigger
 * `onAnyUserEvent`, because they implement `UserEvent`.
 *
 * ### Notes
 * - All events must still extend [Dispatchable] (typically via [Event]).
 * - Both the interface handler and the concrete event handler may be
 *   invoked for the same event.
 * - Useful for grouping related events without relying on inheritance.
 *
 * @author Fantamomo
 * @since 1.7-SNAPSHOT
 */
interface EventType : KEventElement