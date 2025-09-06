package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Interface for events that contain a single generic type parameter.
 *
 * This is a convenience interface extending [GenericTypedEvent], simplifying
 * the declaration of events like:
 *
 * ```
 * class DeadEvent<T : Dispatchable>(val event: T) : SingleGenericTypedEvent {
 *     override fun extractGenericType(): KClass<*> = event::class
 * }
 * ```
 *
 * The event system automatically wraps this single type into a list for matching
 * logic via the inherited [extractGenericTypes] method.
 *
 * Use this interface when your event only needs to expose one type parameter.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface SingleGenericTypedEvent : GenericTypedEvent {
    /**
     * Returns the single runtime generic type associated with this event.
     *
     * Should return the actual [KClass] representing the generic type `T`.
     * If the type is unknown or `null`, `Nothing::class` can be returned.
     *
     * @return The generic type of this event instance.
     */
    fun extractGenericType(): KClass<*>

    override fun extractGenericTypes(): List<KClass<*>> = listOf(extractGenericType())
}