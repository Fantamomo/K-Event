package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Specialized interface for events that contain only a single generic type parameter.
 *
 * This is a convenience interface that extends [GenericTypedEvent], simplifying
 * the declaration of events like:
 *
 * ```
 * class DeadEvent<T : Dispatchable>(val event: T) : SingleGenericTypedEvent {
 *     override fun extractGenericType(): KClass<*> = event::class
 * }
 * ```
 *
 * The event system will automatically wrap this single type into a list for use
 * in matching logic via the inherited [extractGenericTypes] method.
 *
 * Use this when your event only needs to expose one type parameter.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface SingleGenericTypedEvent : GenericTypedEvent {
    /**
     * Returns the single runtime generic type associated with this event.
     *
     * Should return the actual [KClass] instance representing the generic type `T`.
     *
     * It may return `Nothing::class` when the type is `null`.
     *
     * @return The generic type of this event instance.
     */
    fun extractGenericType(): KClass<*>

    override fun extractGenericTypes(): List<KClass<*>> = listOf(extractGenericType())
}