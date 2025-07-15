package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Marker interface for events that expose their generic type arguments at runtime.
 *
 * This allows the event system to perform more specific type matching
 * for generic event types, enabling listeners to react only to certain
 * parameterizations of a generic class.
 *
 * For example, an event like `DeadEvent<T>` can implement this interface
 * to provide the actual runtime type of `T`, enabling listeners like:
 *
 * ```
 * @Register
 * fun onDead(event: DeadEvent<PlayerJoinedEvent>) { ... }
 * ```
 *
 * If an event implements this interface, the event system will use the
 * returned list of `KClass` objects to perform type-based matching.
 *
 * @see SingleGenericTypedEvent for the single-type variant.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface GenericTypedEvent {
    /**
     * Returns the list of runtime generic types associated with this event.
     *
     * The types should be returned in declaration order. For example,
     * for a class `ResultEvent<T, R>`, this method should return:
     *
     * ```
     * listOf(T::class, R::class)
     * ```
     *
     * If any of the types are unknown (e.g., null), the implementation may
     * substitute `Nothing::class`.
     *
     * @return List of KClass objects representing the runtime types of the generics.
     */
    fun extractGenericTypes(): List<KClass<*>>
}