package com.fantamomo.kevent

/**
 * Functional interface for asynchronously handling events in the dispatchable event system.
 *
 * This interface defines a contract for managing a single type of event that extends
 * [Dispatchable], using `suspend` functions to support asynchronous processing.
 * Implementing this interface allows asynchronous handling and optional configuration
 * of events during their lifecycle.
 *
 * @param E The type of event this listener handles, extending [Dispatchable].
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleSuspendListener<E : Dispatchable> : SimpleConfiguration<E> {

    /**
     * Asynchronously handles an event of type [E].
     *
     * This method is intended to be called only by [handleArgs]. Implementations
     * that require additional arguments should throw an exception if this method
     * is called directly without them.
     *
     * @param event The event instance to handle.
     */
    suspend fun handle(event: E)

    /**
     * Asynchronously handles an event of type [E] with additional arguments.
     *
     * This method allows processing the event with a map of optional arguments
     * that may influence handling behavior. By default, it delegates to [handle(event)].
     *
     * @param event The event instance to handle.
     * @param args A map of additional arguments for event handling.
     */
    suspend fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)
}