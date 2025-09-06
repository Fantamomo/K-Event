package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Base class for events within the event dispatch system.
 *
 * `Event` is an abstract subclass of `Dispatchable` that provides a foundation
 * for defining specific event types. Normally, developers should inherit from
 * `Event` rather than directly from `Dispatchable`, unless there is a special
 * reason to bypass the event-specific functionality.
 *
 * It extends `Dispatchable` to support event handling and dispatching within the framework.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
abstract class Event : Dispatchable() {
    companion object : Listenable<Event> {
        override val eventType: KClass<Event> = Event::class
    }
}