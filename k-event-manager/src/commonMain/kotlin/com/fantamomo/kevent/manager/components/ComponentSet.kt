package com.fantamomo.kevent.manager.components

class ComponentSet internal constructor(internal val components: Set<EventManagerComponent<*>>) : EventManagerComponent<ComponentSet> {
    override val key: EventManagerComponent.Key<ComponentSet> = Key

    internal object Key : EventManagerComponent.Key<ComponentSet> {
        override val clazz = ComponentSet::class
    }

    companion object {
        private val EMPTY = ComponentSet(emptySet())
        fun of(vararg components: EventManagerComponent<*>): ComponentSet {
            if (components.isEmpty()) return EMPTY
            return ComponentSet(components.toSet())
        }
    }
}