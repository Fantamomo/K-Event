package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Marker interface for events that expose their generic type arguments at runtime.
 *
 * This enables the event system to perform precise type matching for generic events,
 * allowing listeners to react only to specific parameterizations of a generic class.
 *
 * For example, an event like `DeadEvent<T>` can implement this interface to provide
 * the actual runtime type of `T`, allowing listeners such as:
 *
 * ```
 * @Register
 * fun onDead(event: DeadEvent<PlayerJoinedEvent>) { ... }
 * ```
 *
 * If implemented, the event system will use the returned list of `KClass` objects
 * for type-based matching.
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
     * for a class `ResultEvent<T, R>`, this should return:
     *
     * ```
     * listOf(T::class, R::class)
     * ```
     *
     * Unknown types can be represented with `Nothing::class`.
     *
     * @return List of KClass objects representing the runtime generic types.
     */
    fun extractGenericTypes(): List<KClass<*>>
}