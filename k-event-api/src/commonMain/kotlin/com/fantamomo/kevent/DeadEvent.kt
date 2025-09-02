package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents an event that could not be delivered to any *active* registered listeners.
 *
 * A listener is considered *active* unless its [silent][EventConfigurationScope.silent] property
 * is set to `true`. Silent listeners are ignored when determining whether an event has been handled,
 * which allows passive observers, loggers, or debug tools to receive events without suppressing
 * `DeadEvent` dispatch.
 *
 * The `DeadEvent` is used within the event system to signify that a particular event
 * instance has no active listeners or handlers capable of processing it. This is helpful in
 * debugging or logging scenarios where it is desirable to track unhandled events.
 *
 * A `DeadEvent` wraps the original event object that failed to be delivered, allowing
 * the event system or application to inspect the original event properties or take
 * other necessary actions.
 *
 * It is important to note that a `DeadEvent` is itself a valid event and can be processed
 * by listeners that subscribe to it — including silent listeners, if desired.
 *
 * @param event The original event instance that could not be delivered to any *active* listeners
 * @see EventConfigurationScope.silent
 * @see Key.SILENT
 * @since 1.0-SNAPSHOT
 */
data class DeadEvent<D : Dispatchable>(val event: D) : Dispatchable(), SingleGenericTypedEvent {

    override fun extractGenericType() = event::class

    companion object : Listenable<DeadEvent<Nothing>> {
        @Suppress("UNCHECKED_CAST")
        override val eventType: KClass<DeadEvent<Nothing>> = DeadEvent::class as KClass<DeadEvent<Nothing>>
    }
}