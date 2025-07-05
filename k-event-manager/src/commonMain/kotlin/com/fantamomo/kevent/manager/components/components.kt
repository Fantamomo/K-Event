package com.fantamomo.kevent.manager.components

operator fun EventManagerComponent<*>.plus(component: EventManagerComponent<*>): EventManagerComponent<*> = when {
    this is ComponentSet && component is ComponentSet -> ComponentSet(this.components + component.components)
    this is ComponentSet -> ComponentSet(this.components + component)
    component is ComponentSet -> ComponentSet(component.components + this)
    else -> ComponentSet(setOf(this, component))
}

fun EventManagerComponent<*>.addIfAbsent(component: EventManagerComponent<*>): EventManagerComponent<*> {
    if (component.key !in this)
        return this + component
    return this
}

@Suppress("UNCHECKED_CAST")
operator fun <C : EventManagerComponent<C>> EventManagerComponent<*>.get(key: EventManagerComponent.Key<C>): C? =
    (if (this is ComponentSet)
        this.components.find { it.key == key }
    else this.takeIf { it.key == key }) as? C

fun <C : EventManagerComponent<C>> EventManagerComponent<*>.getOrThrow(key: EventManagerComponent.Key<C>): C =
    this[key] ?: throw IllegalArgumentException(
        "EventManagerComponent with key ${key.clazz.simpleName} not found."
    )

operator fun <C : EventManagerComponent<C>> EventManagerComponent<*>.contains(key: EventManagerComponent.Key<C>): Boolean =
    this[key] != null

operator fun EventManagerComponent<*>.contains(component: EventManagerComponent<*>): Boolean = when (this) {
    is ComponentSet -> component in this.components
    else -> this == component
}

@Suppress("UNCHECKED_CAST")
fun <C : EventManagerComponent<C>> EventManagerComponent<*>.getAll(key: EventManagerComponent.Key<C>): List<C> =
    (if (this is ComponentSet)
        this.components.filter { it.key == key }
    else listOfNotNull(this.takeIf { it.key == key })) as List<C>