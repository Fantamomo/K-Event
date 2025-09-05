package com.fantamomo.kevent.manager

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.manager.config.DispatchConfig
import kotlin.reflect.KClass

/**
 * Interface for managing event listeners and dispatching events in the event system.
 *
 * This interface provides methods for registering event listeners, dispatching events
 * to the registered listeners, and registering event-specific handlers with configuration.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface EventManager : HandlerEventScope {

    /**
     * Dispatches an event to all registered event listeners.
     *
     * This method delivers the given [event] to all listeners or handlers that are subscribed
     * to the event type or any of its supertypes. The event is processed synchronously,
     * ensuring that each listener is invoked before continuing execution.
     *
     * The optional [config] parameter allows fine-grained control over the dispatch process,
     * such as marking the event as sticky or disable firing a [com.fantamomo.kevent.DeadEvent] when no listeners are found.
     * If not provided, [DispatchConfig.EMPTY] is used as the default.
     *
     * @param event The event instance to be dispatched to the appropriate listeners.
     * @param config Configuration options for dispatching the event (e.g., sticky flag, DeadEvent behavior).
     */
    fun dispatch(event: Dispatchable, config: DispatchConfig = DispatchConfig.EMPTY)

    /**
     * Dispatches an event to all registered event listeners asynchronously.
     *
     * This suspend function delivers the given [event] to all applicable listeners
     * or handlers subscribed to the event type or its supertypes. Unlike its synchronous
     * counterpart, this method allows asynchronous processing within a coroutine
     * context, enabling non-blocking or delayed execution of event handling logic.
     *
     * The optional [config] parameter allows fine-grained control over the dispatch process,
     * such as marking the event as sticky or disable firing a [com.fantamomo.kevent.DeadEvent] when no listeners are found.
     * If not provided, [DispatchConfig.EMPTY] is used as the default.
     *
     * @param event The event instance to be dispatched to the appropriate listeners.
     * @param config Configuration options for dispatching the event (e.g., sticky flag, DeadEvent behavior).
     */
    suspend fun dispatchSuspend(event: Dispatchable, config: DispatchConfig = DispatchConfig.EMPTY)

    /**
     * Clears all sticky events stored in the event manager.
     */
    fun clearStickyEvents()

    /**
     * Removes a stored sticky event associated with the specified event type.
     *
     * @param clazz The class type of the sticky event to be removed. It must extend [Dispatchable].
     */
    fun removeStickyEvent(clazz: KClass<out Dispatchable>)
}