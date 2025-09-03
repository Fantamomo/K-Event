package com.fantamomo.kevent.manager.components

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A component that supports shared-exclusive execution control for handlers.
 *
 * This class provides mechanisms to manage the concurrent execution of handlers
 * using unique handler identifiers. Only one execution for a given handler ID
 * can occur at a time, ensuring that tasks associated with the same handler ID
 * do not overlap.
 *
 * @author Fantamomo
 * @since 1.1-SNAPSHOT
 */
class SharedExclusiveExecution : EventManagerComponent<SharedExclusiveExecution> {
    private val runningHandlers = ConcurrentHashMap<String, AtomicBoolean>()

    /**
     * Attempts to acquire the execution permission for the specified handler ID.
     * Ensures that only one execution for the given handler ID can occur at a time.
     *
     * @param handlerId The unique identifier of the handler for which execution permission is requested.
     * @return `true` if the permission is successfully acquired; `false` if it is already in use.
     */
    fun tryAcquire(handlerId: String): Boolean {
        val flag = runningHandlers.computeIfAbsent(handlerId) { AtomicBoolean(false) }
        return flag.compareAndSet(false, true)
    }

    /**
     * Releases the execution lock for the specified handler ID, allowing it to be acquired again.
     *
     * This method sets the internal flag associated with the provided handler ID to `false`,
     * indicating that the handler is no longer in use. It should be called after a handler has
     * finished its execution to ensure other operations associated with the same handler ID
     * can proceed.
     *
     * @param handlerId The unique identifier of the handler whose execution lock is to be released.
     */
    fun release(handlerId: String) {
        runningHandlers[handlerId]?.set(false)
    }

    /**
     * Clears references to handlers that are no longer valid.
     */
    fun clear() = runningHandlers.values.removeIf { !it.get() }

    override val key = Key

    companion object Key : EventManagerComponent.Key<SharedExclusiveExecution> {
        override val clazz = SharedExclusiveExecution::class
    }
}
