package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents an event that could not be delivered to any registered listeners.
 *
 * The `DeadEvent` is used within the event system to signify that a particular event
 * instance has no listeners or handlers capable of processing it. This helps in debugging
 * or logging scenarios where it is desirable to track unhandled events in the system.
 *
 * A `DeadEvent` wraps the original event object that failed to be delivered, allowing
 * the event system or application to inspect the original event properties or take
 * other necessary actions.
 *
 * It is important to note that a `DeadEvent` is itself a valid event and can be processed
 * by listeners that subscribe to it, if desired.
 *
 * @param event The original event instance that could not be delivered to any listeners
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
data class DeadEvent(val event: Dispatchable) : Dispatchable() {
    companion object : Listenable<DeadEvent> {
        override val eventType: KClass<DeadEvent> = DeadEvent::class
    }
}