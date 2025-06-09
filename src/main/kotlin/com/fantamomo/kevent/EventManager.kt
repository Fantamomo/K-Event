package com.fantamomo.kevent

interface EventManager {
    fun register(listener: Listener)

    fun dispatch(event: Event)
}