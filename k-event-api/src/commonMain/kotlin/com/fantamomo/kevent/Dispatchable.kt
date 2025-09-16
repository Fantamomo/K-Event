package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents a dispatchable entity within the event system.
 *
 * `Dispatchable` serves as the base class for all events in this framework.
 * It provides a common type that can be extended to define custom events.
 * Subclasses of `Dispatchable` can implement specific event-related functionality
 * and be dispatched within the system.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
abstract class Dispatchable : KEventElement {
    /**
     * Interface for companion objects of classes inheriting from `Dispatchable`.
     *
     * Implementing this interface allows developers to use extension functions
     * directly on the event type, simplifying listener registration and management.
     *
     * @param E The event type represented by the implementing class. Must extend
     *          `Dispatchable` to be compatible with the event system.
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
    interface Listenable<E : Dispatchable> {
        /**
         * The Kotlin class corresponding to the event type `E`.
         *
         * This property is used by the event system to identify
         * events of the specified type.
         *
         * @property eventType The class type of the event `E`, which extends `Dispatchable`.
         */
        val eventType: KClass<E>
    }

    companion object : Listenable<Dispatchable> {
        override val eventType: KClass<Dispatchable> = Dispatchable::class
    }
}