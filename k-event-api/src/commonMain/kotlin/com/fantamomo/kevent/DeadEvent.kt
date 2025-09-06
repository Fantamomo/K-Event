package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents an event that could not be delivered to any *active* registered listeners.
 *
 * A listener is considered *active* unless its [silent][EventConfigurationScope.silent] property
 * is set to `true`. Silent listeners are ignored when determining whether an event has been handled.
 * This allows passive observers, loggers, or debugging tools to listen to events without preventing
 * a `DeadEvent` from being triggered.
 *
 * A `DeadEvent` is used by the event system to indicate that a given event instance
 * had no active listeners or handlers capable of processing it. This is particularly
 * useful for debugging and logging purposes, as it makes unhandled events visible.
 *
 * The `DeadEvent` wraps the original event object, allowing the system or application
 * to inspect its properties or take further action.
 *
 * Important note: If no listeners subscribe to `DeadEvent` itself, no `DeadEvent` will be fired.
 * As a result, the wrapped [event] is never of type `DeadEvent` unless a user explicitly fires it.
 *
 * A `DeadEvent` is itself a valid event and can be handled by listeners subscribed to it —
 * including silent listeners, if desired.
 *
 * @param event The original event instance that could not be delivered to any *active* listeners.
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