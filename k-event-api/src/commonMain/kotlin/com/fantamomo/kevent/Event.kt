package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents a base class for events in the event dispatch system.
 *
 * The `Event` class is an abstract subclass of `Dispatchable` that enables the creation
 * of specific event types. Any event in the system should inherit from this class.
 * It extends the functionality provided by the `Dispatchable` class to allow event handling
 * and dispatching within the system.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
abstract class Event : Dispatchable() {
    companion object : Listenable<Event> {
        override val eventType: KClass<Event> = Event::class
    }
}