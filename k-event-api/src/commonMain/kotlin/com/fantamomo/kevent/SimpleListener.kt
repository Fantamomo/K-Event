package com.fantamomo.kevent

/**
 * Functional interface for handling events within the dispatchable event system.
 *
 * This interface defines the contract for managing a single type of event that extends
 * the [Dispatchable] class. Implementing or instantiating this functional interface
 * allows the handling and optional configuration of events during their lifecycle.
 *
 * @param E The type of the event that this listener handles, extending [Dispatchable].
 *
 * @author Fantamomo
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleListener<E : Dispatchable> : SimpleConfiguration<E> {

    /**
     * Handles an event of type [E].
     *
     * This method is intended to be called only by the `handle(E, Map<String, Any?>)` method.
     * Implementations that require additional arguments should throw an exception
     * when this method is called directly without them.
     *
     * @param event The event to be handled.
     */
    fun handle(event: E)

    /**
     * Handles an event of type [E] with additional arguments.
     *
     * This method allows the processing of an event, supplying an optional map
     * of arguments that might influence the handling behavior. The arguments
     * map can be used to pass configuration or data specific to the event
     * being handled. In the default implementation, this method delegates
     * to the simpler `handle(E)` method for processing.
     *
     * @param event The event to be handled.
     * @param args A map of additional arguments that may influence event handling.
     */
    fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)
}