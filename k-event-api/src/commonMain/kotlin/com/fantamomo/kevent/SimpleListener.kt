package com.fantamomo.kevent

/**
 * Functional interface for handling events in the dispatchable event system.
 *
 * This interface defines a contract for managing a single type of event that extends
 * [Dispatchable]. Implementing or instantiating this functional interface allows
 * handling events and optionally providing configuration for them during their lifecycle.
 *
 * @param E The type of event this listener handles, extending [Dispatchable].
 * @author Fantamomo
 * @since 1.2-SNAPSHOT
 */
fun interface SimpleListener<E : Dispatchable> : SimpleConfiguration<E> {

    /**
     * Handles an event of type [E].
     *
     * This method is intended to be called only by [handleArgs]. Implementations
     * that require additional arguments should throw an exception if this method
     * is called directly without them.
     *
     * @param event The event instance to be handled.
     */
    fun handle(event: E)

    /**
     * Handles an event of type [E] with additional arguments.
     *
     * This method allows processing the event with a map of optional arguments
     * that can influence handling behavior. The arguments may contain configuration
     * data or other runtime information relevant to the event.
     *
     * By default, this method delegates to [handle(event)].
     *
     * @param event The event instance to be handled.
     * @param args A map of additional arguments for event handling.
     */
    fun handleArgs(event: E, args: Map<String, Any?>) = handle(event)
}