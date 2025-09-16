package com.fantamomo.kevent

/**
 * Root type for all entities that can participate in the event system.
 *
 * Both [Dispatchable] (concrete event classes) and [EventType] (marker interfaces
 * for event categories) inherit from this sealed interface.
 *
 * This allows the type hierarchy to be complete and closed, so that
 * all dispatchable elements can be recognized and handled consistently.
 *
 * @author Fantamomo
 * @since 1.7-SNAPSHOT
 */
sealed interface KEventElement