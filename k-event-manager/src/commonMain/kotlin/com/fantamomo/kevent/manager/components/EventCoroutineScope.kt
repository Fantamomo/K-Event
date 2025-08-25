package com.fantamomo.kevent.manager.components

import kotlinx.coroutines.CoroutineScope

/**
 * A data class that represents a coroutine scope as a component within the event manager system.
 * This component allows integration of [CoroutineScope] into the event manager for managing
 * coroutine-based tasks and operations.
 *
 * @property scope The [CoroutineScope] instance managed by this component.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
data class EventCoroutineScope(val scope: CoroutineScope) : EventManagerComponent<EventCoroutineScope>{
    override val key: EventManagerComponent.Key<EventCoroutineScope> = Key

    companion object Key : EventManagerComponent.Key<EventCoroutineScope> {
        override val clazz = EventCoroutineScope::class
    }
}