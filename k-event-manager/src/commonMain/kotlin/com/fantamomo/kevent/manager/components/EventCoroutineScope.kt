package com.fantamomo.kevent.manager.components

import kotlinx.coroutines.CoroutineScope

data class EventCoroutineScope(val scope: CoroutineScope) : EventManagerComponent<EventCoroutineScope>{
    override val key: EventManagerComponent.Key<EventCoroutineScope> = Key

    companion object Key : EventManagerComponent.Key<EventCoroutineScope> {
        override val clazz = EventCoroutineScope::class
    }
}