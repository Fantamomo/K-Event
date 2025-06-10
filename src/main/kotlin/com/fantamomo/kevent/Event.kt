package com.fantamomo.kevent

/**
 * Base class for all events in the event system.
 * 
 * Events are objects that are passed to event handlers when they are triggered.
 * Custom events should extend this class to be compatible with the event system.
 * 
 * Example of a custom event:
 * ```
 * class MyEvent(val data: String) : Event() {
 *     // Additional properties and methods specific to this event
 * }
 * ```
 * 
 * When an event is dispatched, it is passed to all registered event handlers
 * that accept its type or a supertype. The event handlers can then access the
 * event's properties and methods to handle the event appropriately.
 * 
 * @see Listener
 * @see Register
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
open class Event {

    /**
     * The name of the event class derived from its simple class name.
     *
     * This property holds the simple name of the event class and is used as an identifier
     * for event instances. If the class does not have a name (e.g., anonymous classes),
     * an [IllegalArgumentException] is thrown. Ensures all events have a meaningful name
     * for identification within the event system.
     *
     * @throws IllegalArgumentException if the event class does not have a name
     */
    val name: String = this::class.simpleName ?: throw IllegalArgumentException("Event class must have a name")

    /**
     * Interface to be implemented by `companion object`s of subclasses inheriting from [Event].
     *
     * The primary purpose of this interface is to enable developers to define `companion object`s
     * that can be extended with utility functions through extension methods. These extensions
     * allow for easier registration and interaction with the events within the event system.
     *
     * It is recommended that all `companion object`s of event subclasses implement this interface,
     * as it provides a structured and type-safe way to handle event-specific operations.
     *
     * @param E The type of event associated with this listener.
     */
    interface Listening<E : Event>

    companion object : Listening<Event>
}