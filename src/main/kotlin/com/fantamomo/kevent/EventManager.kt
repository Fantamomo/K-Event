package com.fantamomo.kevent

import kotlin.reflect.KClass

interface EventManager {
    fun register(listener: Listener)

    fun dispatch(event: Event)

    fun <E : Event> register(
        event: KClass<E>,
        configuration: EventConfiguration<E> = EventConfiguration.default(),
        handler: (E) -> Unit
    )
}