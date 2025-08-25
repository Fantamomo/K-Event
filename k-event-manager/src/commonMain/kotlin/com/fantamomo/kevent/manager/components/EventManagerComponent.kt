package com.fantamomo.kevent.manager.components

import kotlin.reflect.KClass

/**
 * Represents a component in an event manager system. Each component is uniquely identified
 * by its associated key, which is tied to its class type. Components are used to extend
 * the functionality of an event manager by providing modular and reusable features.
 * event management system, enabling them to be identified and retrieved using their `Key`.
 *
 * @param C The type of the implementing component. This ensures that each component has a unique key type.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface EventManagerComponent<C : EventManagerComponent<C>> {
    /**
     * Represents the unique identifier for a given `EventManagerComponent`.
     * The `key` allows the component to be distinctly identified and managed
     * within the event management system. It is associated with a specific
     * runtime class type (`Key.clazz`) of the component.
     *
     * @param C The type of the `EventManagerComponent` associated with this key.
     */
    val key: Key<C>

    /**
     * Represents a unique key associated with a specific `EventManagerComponent`.
     * This key is used to uniquely identify and retrieve components within the
     * event management system. The key is strongly typed to ensure that only
     * components of a specific type are associated with a corresponding key.
     *
     * @param C The type of the `EventManagerComponent` associated with this key.
     *
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
    interface Key<C : EventManagerComponent<C>> {
        val clazz: KClass<C>
    }
}