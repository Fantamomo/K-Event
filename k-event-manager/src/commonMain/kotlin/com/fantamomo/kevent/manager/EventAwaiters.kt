package com.fantamomo.kevent.manager

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventConfiguration
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
    val handler = register(event, configuration) { dispatchable ->
        if (cont.isActive) {
            cont.resume(dispatchable)
        }
    }

    cont.invokeOnCancellation {
        handler.unregister()
    }
}

/**
 * Waits for the dispatch and handling of an event of the specified type within the scope.
 *
 * This method suspends until an event of type [D] is dispatched and handled, optionally using a provided
 * configuration to control how the event is processed. The event type is resolved using the reified
 * generic parameter [D].
 *
 * @param D The specific type of the event to await, which must extend [Dispatchable].
 * @param configuration The configuration used to handle the event. If not provided, the default
 * configuration obtained from [EventConfiguration.default] will be used.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitEvent(configuration: EventConfiguration<D> = EventConfiguration.default()) =
    awaitEvent(D::class, configuration)

/**
 * Awaits the occurrence of a specific event within a given timeout period and optionally
 * applies a configuration to the event handler. If the event does not occur within the
 * specified timeout, the method returns null.
 *
 * @param D The type of event to await. This type must extend from [Dispatchable].
 * @param event The class of the event to await.
 * @param timeoutMillis The maximum time to wait for the event in milliseconds.
 * @param configuration The optional configuration to apply to the event handler. Defaults to the result of [EventConfiguration.default].
 * @return The awaited event of type [D] if it occurs within the timeout period, or null if the timeout expires.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitEventOrNull(
    event: KClass<D>,
    timeoutMillis: Long,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): D? = withTimeoutOrNull(timeoutMillis) { awaitEvent(event, configuration) }

/**
 * Suspends the current coroutine and waits for an event of type [D] to occur within the specified timeout.
 * If no event is received within the timeout period, the method returns null. The event is awaited using
 * the provided configuration or the default configuration if none is specified.
 *
 * @param timeoutMillis The maximum time, in milliseconds, to wait for the event before returning null.
 * @param configuration The configuration used for the event processing. Defaults to [EventConfiguration.default].
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitEventOrNull(
    timeoutMillis: Long,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = awaitEventOrNull(D::class, timeoutMillis, configuration)

/**
 * Suspends the current coroutine until a specific event is dispatched, then returns the event.
 * The event must match the provided type and satisfy the given filter.
 *
 * @param D The type of the event, which must extend [Dispatchable].
 * @param event The class type of the event to be awaited.
 * @param filter A lambda function used to filter the events of type [D]. The event is returned
 *               only if this lambda returns true.
 * @param configuration Optional configuration for the event, specifying additional handling
 *                      options. Defaults to the result of [EventConfiguration.default].
 * @return The first event of type [D] that meets the criteria specified by the filter.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitFilteredEvent(
    event: KClass<D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
    filter: (D) -> Boolean,
): D = suspendCancellableCoroutine { cont ->
    val handler = register(event, configuration) { dispatchable ->
        if (cont.isActive) {
            if (filter(dispatchable)) {
                cont.resume(dispatchable)
            }
        }
    }

    cont.invokeOnCancellation {
        handler.unregister()
    }
}

/**
 * Suspends until a specific event of type [D] is dispatched and passes the provided [filter].
 *
 * This function listens for events of the specified type [D] in the current [HandlerEventScope].
 * The event must satisfy the [filter] condition to be returned. A custom [EventConfiguration] can also
 * be provided to configure the behavior of the event handling.
 *
 * @param D The type of event to wait for. This must extend from `Dispatchable`.
 * @param filter A lambda function that filters the events to be handled. Only events for which the [filter]
 *               returns `true` will cause the suspension to end.
 * @param configuration The configuration for event handling. Defaults to [EventConfiguration.default].
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
suspend inline fun <reified D : Dispatchable> HandlerEventScope.awaitFilteredEvent(
    configuration: EventConfiguration<D> = EventConfiguration.default(),
    noinline filter: (D) -> Boolean,
) = awaitFilteredEvent(D::class, configuration, filter)

/**
 * Creates a flow of events of the specified type that are dispatched within the current handler scope.
 * The flow will emit events as they occur, respecting the provided configuration.
 *
 * @param D The type of event, which must extend [Dispatchable].
 * @param event The [KClass] of the event type to listen for.
 * @param configuration The configuration for the event handler. Defaults to [EventConfiguration.default].
 * @return A [Flow] of dispatched events of type [D].
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun <D : Dispatchable> HandlerEventScope.eventFlow(
    event: KClass<D>,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): Flow<D> = channelFlow {
    val handler = register(event, configuration) {
        trySend(it).onFailure { close() }
    }

    awaitClose { handler.unregister() }
}

/**
 * Provides a flow of events of the specified type [D] that are dispatched within the current handler scope.
 * The flow will emit events as they occur, using the provided configuration or the default configuration.
 *
 * @param D The type of event, which must extend [Dispatchable].
 * @param configuration The configuration for the event handler. Defaults to [EventConfiguration.default].
 * @return A [Flow] of dispatched events of type [D].
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
inline fun <reified D : Dispatchable> HandlerEventScope.eventFlow(
    configuration: EventConfiguration<D> = EventConfiguration.default(),
) = eventFlow(D::class, configuration)

/**
 * Creates a flow of events of type [Dispatchable] within the current [HandlerEventScope].
 *
 * This method provides a convenient way to observe and process events of type [Dispatchable]
 * in a reactive manner using Kotlin Flows. The flow emits events as they are dispatched
 * within the scope, following the default [EventConfiguration].
 *
 * The flow is useful for scenarios where event handling requires asynchronous processing
 * or when multiple listeners need to react to a specific event type without direct coupling.
 *
 * @return A [Flow] of events of type [Dispatchable] within the current scope.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun HandlerEventScope.eventFlow() = eventFlow<Dispatchable>(EventConfiguration.default())

/**
 * Awaits for a specified number of events of a given type to be dispatched, returning them in a list.
 *
 * This method suspends until the specified number of events matching the given type have been received.
 * An optional configuration can be provided to customize event handling behavior.
 *
 * @param D The type of events to await, extending from [Dispatchable].
 * @param type The class representing the type of the events to wait for.
 * @param count The number of events to wait for before resuming.
 * @param configuration The configuration for the event type. Defaults to [EventConfiguration.default].
 * @return A list of events of type [D] that were received.
 *
 * @author Fantamomo
 * @since 1.3-SNAPSHOT
 */
suspend fun <D : Dispatchable> HandlerEventScope.awaitEvents(
    type: KClass<D>,
    count: Int,
    configuration: EventConfiguration<D> = EventConfiguration.default(),
): List<D> = suspendCancellableCoroutine { cont ->
    val events = mutableListOf<D>()
    val handler = register(type, configuration) {
        if (cont.isActive) {
            events.add(it)
            if (events.size >= count) {
                cont.resume(events)
            }
        }
    }
    cont.invokeOnCancellation { handler.unregister() }
}

/**
 * Awaits the occurrence of a specified number of events of a certain type
 * within the current [HandlerEventScope].
 *
 * This suspend function blocks the coroutine execution until the specified
 * number of matching events has been received. The event handling can be
 * customized by providing an optional [EventConfiguration] for the event type.
 *
 * @param D The type of events to await, extending from [Dispatchable].
 * @param count The number of events to wait for, must be greater than zero.
 * @param configuration The configuration to apply when awaiting events of type [D].
 *                       Defaults to [EventConfiguration.default].
 *
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
 * This method listens for dispatchable events of the provided types and returns the first event
 * that matches one of the specified types.
 * If no types are provided, the method awaits for any dispatchable event.
 * If multiple types are provided, the method awaits for one of the specified types (or one of its subtypes).
 *
 * @param D The type of the dispatchable event to await. It must extend [Dispatchable].
 * @param types Vararg parameter specifying the classes of event types to listen for. If no types
 *              are provided, all dispatchable events are considered.
 * @param configuration An optional [EventConfiguration] to customize how the awaited event is handled.
 *                      Defaults to [EventConfiguration.default].
 * @return The first dispatched event of the specified types that matches the criteria.
 *
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
        types.isEmpty() -> awaitEvent<Dispatchable>(configuration)
        types.size == 1 -> awaitEvent(types[0] as KClass<Dispatchable>, configuration)
        else -> suspendCancellableCoroutine { cont ->
            val events = types.toSet()
            val handler = register(Dispatchable::class, configuration) {
                if (cont.isActive) {
                    if (events.contains(it::class)) {
                        cont.resume(it)
                    }
                }
            }

            cont.invokeOnCancellation { handler.unregister() }
        }
    } as D
}