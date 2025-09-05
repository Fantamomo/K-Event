package com.fantamomo.kevent.manager

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.manager.config.DispatchConfig
import com.fantamomo.kevent.manager.config.DispatchConfigScope

/**
 * Dispatches an event to all registered listeners with a configurable dispatch scope.
 *
 * This inline function provides a way to dispatch an event of type [E], along with a configurable
 * [DispatchConfigScope]. Use the [config] lambda to define custom behavior or configuration
 * for the event dispatch process.
 *
 * @param event The event instance to be dispatched. Must be a subtype of [Dispatchable].
 * @param config A lambda function used to configure the [DispatchConfigScope] for the event dispatch.
 * @since 1.9-SNAPSHOT
 * @author Fantamomo
 */
inline fun <E : Dispatchable> EventManager.dispatch(event: E, config: DispatchConfigScope.() -> Unit) {
    val scope = DispatchConfigScope()
    scope.config()
    dispatch(event, DispatchConfig(scope))
}

/**
 * Asynchronously dispatches a specified event to all registered listeners with a custom configuration.
 *
 * This inline function provides a way to dispatch an event of type [E], along with a configurable
 * [DispatchConfigScope]. Use the [config] lambda to define custom behavior or configuration
 * for the event dispatch process.
 *
 * @param E The type of the event, which must extend [Dispatchable].
 * @param event The event instance to be dispatched to all applicable listeners.
 * @param config A lambda function to configure the dispatch behavior using a [DispatchConfigScope].
 * @since 1.9-SNAPSHOT
 * @author Fantamomo
 */
suspend inline fun <E : Dispatchable> EventManager.dispatchSuspend(event: E, config: DispatchConfigScope.() -> Unit) {
    val scope = DispatchConfigScope()
    scope.config()
    dispatchSuspend(event, DispatchConfig(scope))
}