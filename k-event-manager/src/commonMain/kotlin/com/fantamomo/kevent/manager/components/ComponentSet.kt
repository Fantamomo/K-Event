package com.fantamomo.kevent.manager.components

/**
 * Represents a set of [EventManagerComponent] instances. This class allows grouping
 * multiple components into a single entity, enabling efficient management within
 * an event management system.
 *
 * This class provides an efficient way to combine and manage multiple components
 * using utility functions such as [plus], [addIfAbsent], and [get] extensions.
 *
 * @constructor Internal constructor that initializes the [ComponentSet] with a set of components.
 * The class ensures immutability of the component set.
 *
 * @property components A [Set] containing the [EventManagerComponent] instances that belong to this set.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class ComponentSet internal constructor(internal val components: Set<EventManagerComponent<*>>) : EventManagerComponent<ComponentSet> {
    override val key: EventManagerComponent.Key<ComponentSet> = Key

    internal object Key : EventManagerComponent.Key<ComponentSet> {
        override val clazz = ComponentSet::class
    }

    companion object {
        /**
         * Represents an empty immutable instance of [ComponentSet], containing no components.
         *
         * This is a predefined constant used as a convenience to avoid creating multiple instances
         * of an empty [ComponentSet]. It ensures that all empty sets of components share a single
         * immutable instance, optimizing memory usage and providing a consistent reference for
         * operations requiring an empty component set.
         */
        val EMPTY = ComponentSet(emptySet())
        /**
         * Creates a new [ComponentSet] instance containing the given [EventManagerComponent] elements.
         *
         * This function allows combining multiple [EventManagerComponent] instances into a single
         * immutable [ComponentSet], facilitating efficient management and retrieval of components
         * in an event manager system. If no components are provided, an empty [ComponentSet] is returned.
         *
         * @param components An array of [EventManagerComponent] instances to be included in the resulting [ComponentSet].
         * @return A [ComponentSet] containing the provided [EventManagerComponent] instances,
         * or an empty [ComponentSet] if no components are supplied.
         */
        fun of(vararg components: EventManagerComponent<*>): ComponentSet {
            if (components.isEmpty()) return EMPTY
            return ComponentSet(components.toSet())
        }
    }
}