package com.fantamomo.kevent.manager

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventConfiguration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
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
): D {
    val deferred = CompletableDeferred<D>()
    val handler = register(event, configuration) {
        deferred.complete(it)
    }
    return deferred.await().also { handler.unregister() }
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
): D? {
    val deferred = CompletableDeferred<D>()
    val handler = register(event, configuration) {
        deferred.complete(it)
    }
    val result = withTimeoutOrNull(timeoutMillis) { deferred.await() }
    handler.unregister()
    return result
}

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
): D {
    val deferred = CompletableDeferred<D>()
    val handler = register(event, configuration) {
        if (filter(it)) {
            deferred.complete(it)
        }
    }
    return deferred.await().also { handler.unregister() }
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
): Flow<D> {
    val channel = Channel<D>(Channel.BUFFERED)
    val handler = register(event, configuration) {
        channel.trySend(it).isSuccess
    }
    return flow {
        try {
            for (item in channel) {
                emit(item)
            }
        } finally {
            handler.unregister()
            channel.close()
        }
    }
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
