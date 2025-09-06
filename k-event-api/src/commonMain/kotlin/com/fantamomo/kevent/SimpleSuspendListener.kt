package com.fantamomo.kevent

/**
 * Functional interface for asynchronously handling events within the dispatchable event system.
 *
 * This interface defines the contract for managing a single type of event that extends
 * [Dispatchable], using `suspend` functions to support asynchronous processing.
 * Implementing or instantiating this functional interface allows asynchronous handling
 * and optional configuration of events during their lifecycle.
 *
 * @param E The type of the event that this listener handles, extending [Dispatchable].
 *
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleSuspendListener<E : Dispatchable> : SimpleConfiguration<E> {

    /**
     * Asynchronously handles an event of type [E].
     *
     * This method is intended to be called only by the `handle(E, Map<String, Any?>)` method.
     * Implementations that require additional arguments should throw an exception
     * when this method is called directly without them.
     *
     * @param event The event to be handled.
     */
    suspend fun handle(event: E)

    /**
     * Asynchronously handles an event of type [E] with additional arguments.
     *
     * This method allows the processing of an event while providing an optional map
     * of arguments that may influence the handling behavior. The default implementation
     * delegates to the simpler `handle(E)` method.
     *
     * @param event The event to be handled.
     * @param args A map of additional arguments that may influence event handling.
     */
    suspend fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)
}