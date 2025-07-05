package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Represents a dispatchable entity in the event system.
 *
 * A dispatchable is the base class for all events within this event handling framework.
 * It serves as the foundational type that can be extended to define custom events.
 * Subclasses of `Dispatchable` are designed to provide specific event-related functionality
 * and can be dispatched within the system.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
abstract class Dispatchable {
    /**
     * Classes inheriting from `Dispatchable` should implement this interface in their companion
     * objects, allowing developers to use extension functions directly on the event type
     * for easier listener registration and management.
     *
     * @param E The type of event that the implementing class represents. This must extend
     *          from `Dispatchable` to ensure compatibility with the event system.
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
    interface Listenable<E : Dispatchable> {
        /**
         * The type of event represented by this listenable instance.
         *
         * This property provides the Kotlin class corresponding to the event type `E`.
         * It is used to identify and dispatch events of the specified type within
         * the event-handling system.
         *
         * @property eventType The class type of the event `E`, which extends `Dispatchable`.
         */
        val eventType: KClass<E>
    }

    companion object : Listenable<Dispatchable> {
        override val eventType: KClass<Dispatchable> = Dispatchable::class
    }
}