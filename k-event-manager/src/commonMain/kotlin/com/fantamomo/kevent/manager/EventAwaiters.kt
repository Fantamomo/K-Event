package com.fantamomo.kevent.manager

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventConfiguration
import com.fantamomo.kevent.Key
import com.fantamomo.kevent.setIfAbsent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.reflect.KClass

/**
 * Awaits the dispatch of a specified event type within the event handling system.
 *
 * The method suspends until an event of the specified type is dispatched, then returns the event.
 * An optional configuration can be provided to customize the behavior of the event handler.
 *
 * By default, this function ignores sticky events because it calls
 * `configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)`.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration (even though the default is false, this helper overrides it).
 *
 * @param D The specific type of event to wait for, which must extend [Dispatchable].
 * @param event The [KClass] of the event to wait for.
 * @param configuration The configuration for the event handler. Defaults to [EventConfiguration.default].
 * @return The instance of the dispatched event.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitEvent(
    event: KClass<D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): D = suspendCancellableCoroutine { cont ->
    // Register a handler for the event type
    val handler = register(event, configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)) { dispatchable ->
        // Resume coroutine with the event when it is dispatched
        if (cont.isActive) {
            cont.resume(dispatchable)
        }
    }

    // Ensure handler is unregistered when coroutine is cancelled
    cont.invokeOnCancellation {
        handler.unregister()
    }
}

/**
 * Waits for the dispatch and handling of an event of the specified type within the scope.
 *
 * This method suspends until an event of type [D] is dispatched and handled, optionally using a provided
 * configuration to control how the event is processed.
 *
 * This function implicitly calls [awaitEvent], which ignores sticky events by default.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The specific type of the event to await, which must extend [Dispatchable].
 * @param configuration The configuration used to handle the event.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitEvent(
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = awaitEvent(D::class, configuration)

/**
 * Awaits the occurrence of a specific event within a given timeout period.
 *
 * This function implicitly calls [awaitEvent], which ignores sticky events by default.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of event to await.
 * @param event The class of the event to await.
 * @param timeoutMillis The maximum time to wait in milliseconds.
 * @param configuration The optional configuration to apply.
 * @return The awaited event if it occurs within the timeout period, or null.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitEventOrNull(
    event: KClass<D>,
    timeoutMillis: Long,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): D? = withTimeoutOrNull(timeoutMillis) {
    // Use awaitEvent but cancel if timeout is reached
    awaitEvent(event, configuration)
}

/**
 * Suspends until a specific event of type [D] occurs within the given timeout.
 *
 * This function implicitly calls [awaitEventOrNull], which ignores sticky events by default.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitEventOrNull(
    timeoutMillis: Long,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = awaitEventOrNull(D::class, timeoutMillis, configuration)

/**
 * Suspends the current coroutine until a specific event is dispatched and passes the given filter.
 *
 * By default, this function ignores sticky events because it calls
 * `configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)`.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of the event.
 * @param event The class type of the event to be awaited.
 * @param filter A lambda to filter the events.
 * @param configuration Optional configuration for the event.
 * @return The first event that meets the filter criteria.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitFilteredEvent(
    event: KClass<D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
    filter: (D) -> Boolean,
): D = suspendCancellableCoroutine { cont ->
    // Register handler that applies filter to events
    val handler = register(event, configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)) { dispatchable ->
        if (cont.isActive) {
            // Resume only if event matches filter
            if (filter(dispatchable)) {
                cont.resume(dispatchable)
            }
        }
    }

    // Clean up handler when coroutine is cancelled
    cont.invokeOnCancellation {
        handler.unregister()
    }
}

/**
 * Suspends until a specific event of type [D] passes the given filter.
 *
 * This function implicitly calls [awaitFilteredEvent], which ignores sticky events by default.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param configuration The configuration for event handling.
 * @param filter A lambda that filters the events.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitFilteredEvent(
    configuration: EventConfiguration<D> = EventConfiguration.default(),
    noinline filter: (D) -> Boolean,
) = awaitFilteredEvent(D::class, configuration, filter)

/**
 * Creates a flow of events of the specified type within the current handler scope.
 *
 * By default, this function ignores sticky events because it calls
 * `configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)`.
 * If you want sticky events to be included in the flow, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of event.
 * @param event The [KClass] of the event type.
 * @param configuration The configuration for the event handler.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun <D : Dispatchable> HandlerEventScope.eventFlow(
    event: KClass<D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): Flow<D> = channelFlow {
    // Register event handler and emit events into the channel
    val handler = register(event, configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)) {
        trySend(it).onFailure { close() } // Try to send event, close if failed
    }

    // Unregister handler when flow collection is cancelled
    awaitClose { handler.unregister() }
}

/**
 * Provides a flow of events of the specified type.
 *
 * This function implicitly calls [eventFlow], which ignores sticky events by default.
 * If you want sticky events to be included, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param configuration The configuration for the event handler.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
inline fun <reified D : Dispatchable> HandlerEventScope.eventFlow(
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = eventFlow(D::class, configuration)

/**
 * Creates a flow of events of type [Dispatchable] within the current [HandlerEventScope].
 *
 * This function implicitly calls [eventFlow], which ignores sticky events by default.
 * If you want sticky events to be included, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @return A [Flow] of events of type [Dispatchable].
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun HandlerEventScope.eventFlow() = eventFlow<Dispatchable>(EventConfiguration.default())

/**
 * Awaits for a specified number of events of a given type to be dispatched, returning them in a list.
 *
 * By default, this function ignores sticky events because it calls
 * `configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)`.
 * If you want sticky events to be counted, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of events to await.
 * @param type The class representing the type of the events to wait for.
 * @param count The number of events to wait for.
 * @param configuration The configuration for the event type.
 * @author Fantamomo
 * @since 1.3-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitEvents(
    type: KClass<D>,
    count: Int,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): List<D> = suspendCancellableCoroutine { cont ->
    val events = mutableListOf<D>() // Store collected events
    val handler = register(type, configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)) {
        if (cont.isActive) {
            events.add(it) // Add each event to list
            if (events.size >= count) {
                // Resume coroutine once enough events are collected
                cont.resume(events)
            }
        }
    }
    // Cleanup when coroutine is cancelled
    cont.invokeOnCancellation { handler.unregister() }
}

/**
 * Awaits the occurrence of a specified number of events of a certain type.
 *
 * This function implicitly calls [awaitEvents], which ignores sticky events by default.
 * If you want sticky events to be counted, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of events to await.
 * @param count The number of events to wait for.
 * @param configuration The configuration to apply.
 * @author Fantamomo
 * @since 1.3-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitEvents(
    count: Int,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = awaitEvents(D::class, count, configuration)

/**
 * Awaits and suspends until any event of the specified types is dispatched within the current event scope.
 *
 * By default, this function ignores sticky events because it calls
 * `configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)`.
 * If you want sticky events to be listened to, explicitly set `Key.IGNORE_STICKY_EVENTS` to `false`
 * in the provided configuration.
 *
 * @param D The type of the dispatchable event to await.
 * @param types Vararg parameter specifying event types to listen for.
 * @param configuration An optional [EventConfiguration] to customize event handling.
 * @return The first dispatched event that matches the criteria.
 * @author Fantamomo
 * @since 1.3-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
suspend fun <D : Dispatchable> HandlerEventScope.awaitAnyEvent(
    vararg types: KClass<out D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): D {
    configuration as EventConfiguration<Dispatchable>
    return when {
        // No types provided → wait for any Dispatchable
        types.isEmpty() -> awaitEvent<Dispatchable>(configuration)

        // Single type → delegate to awaitEvent
        types.size == 1 -> awaitEvent(types[0] as KClass<Dispatchable>, configuration)

        // Multiple types → listen for Dispatchable and check if class matches
        else -> suspendCancellableCoroutine { cont ->
            val events = types.toSet() // Allowed event classes
            val handler = register(Dispatchable::class, configuration.setIfAbsent(Key.IGNORE_STICKY_EVENTS, true)) {
                if (cont.isActive) {
                    if (events.contains(it::class)) {
                        cont.resume(it) // Resume once matching event is found
                    }
                }
            }

            // Cleanup on cancellation
            cont.invokeOnCancellation { handler.unregister() }
        }
    } as D
}