package com.fantamomo.kevent.manager.components

/**
 * Combines two [EventManagerComponent] instances into a single [ComponentSet].
 * If both components are [ComponentSet] instances, their contents are merged.
 * Otherwise, the components are grouped into a new [ComponentSet].
 *
 * @param component The [EventManagerComponent] to be added to this component.
 * @return A [ComponentSet] containing the combined [EventManagerComponent] instances.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
operator fun EventManagerComponent<*>.plus(component: EventManagerComponent<*>): EventManagerComponent<*> = when {
    this is ComponentSet && component is ComponentSet -> ComponentSet(this.components + component.components)
    this is ComponentSet -> ComponentSet(this.components + component)
    component is ComponentSet -> ComponentSet(component.components + this)
    else -> ComponentSet(setOf(this, component))
}

/**
 * Adds the given component to this [EventManagerComponent] if it is not already present.
 * If the component's key does not exist in this component, it combines them into a new
 * `ComponentSet`. Otherwise, returns the current component.
 *
 * @param component The [EventManagerComponent] to add if it is not already present.
 * @return A new [EventManagerComponent] that includes the given component if it was absent,
 * or the current [EventManagerComponent] if the component was already present.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun EventManagerComponent<*>.addIfAbsent(component: EventManagerComponent<*>): EventManagerComponent<*> {
    if (component.key !in this)
        return this + component
    return this
}

/**
 * Retrieves a specific component of type [C] from this [EventManagerComponent] instance or
 * from a [ComponentSet], if applicable, by matching its associated key.
 *
 * If the current instance is a [ComponentSet], it searches through the set for a component
 * whose key matches the provided `key`. Otherwise, it checks if the current component's
 * key matches the provided `key`.
 *
 * @param key The [EventManagerComponent.Key] associated with the component to be retrieved.
 * @return The component of type [C] if found and successfully cast, or `null` if no match is found.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
operator fun <C : EventManagerComponent<C>> EventManagerComponent<*>.get(key: EventManagerComponent.Key<C>): C? =
    (if (this is ComponentSet)
        this.components.find { it.key == key }
    else this.takeIf { it.key == key }) as? C

/**
 * Retrieves a component of type [C] from an [EventManagerComponent] using its associated [key].
 * If the component is not found, throws an [IllegalArgumentException] with a descriptive error message.
 *
 * @param key The [EventManagerComponent.Key] associated with the component to be retrieved.
 * @return The component of type [C] if found.
 * @throws IllegalArgumentException If no component of the specified type is found for the provided [key].
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun <C : EventManagerComponent<C>> EventManagerComponent<*>.getOrThrow(key: EventManagerComponent.Key<C>): C =
    this[key] ?: throw IllegalArgumentException(
        "EventManagerComponent with key ${key.clazz.simpleName} not found."
    )

/**
 * Checks whether the specified key is associated with a component in this [EventManagerComponent].
 *
 * This operation determines if the [EventManagerComponent] provided in the context or its children
 * (if the instance is a [ComponentSet]) contains a component corresponding to the given key.
 *
 * @param key The [EventManagerComponent.Key] representing the component to check for.
 * @return `true` if a component associated with the given key exists, `false` otherwise.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
operator fun <C : EventManagerComponent<C>> EventManagerComponent<*>.contains(key: EventManagerComponent.Key<C>): Boolean =
    this[key] != null

/**
 * Determines whether a given component is contained within this [EventManagerComponent].
 *
 * For a [ComponentSet], it checks if the given component exists in its components.
 * For other [EventManagerComponent] instances, it directly compares equality.
 *
 * @param component The [EventManagerComponent] to check for existence.
 * @return `true` if the component is found; otherwise, `false`.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
operator fun EventManagerComponent<*>.contains(component: EventManagerComponent<*>): Boolean = when (this) {
    is ComponentSet -> component in this.components
    else -> this == component
}

/**
 * Retrieves all components of the specified type from the current component or component set.
 *
 * If the current component is a [ComponentSet], this method filters its components by
 * matching their `key` with the given key. Otherwise, it checks if the current component's
 * key matches the given [key] and includes it in the result if it does.
 *
 * @param key The key representing the type of `EventManagerComponent` to retrieve.
 * @return A list of components of the specified type, or an empty list if no matching components are found.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
fun <C : EventManagerComponent<C>> EventManagerComponent<*>.getAll(key: EventManagerComponent.Key<C>): List<C> =
    (if (this is ComponentSet)
        this.components.filter { it.key == key }
    else listOfNotNull(this.takeIf { it.key == key })) as List<C>