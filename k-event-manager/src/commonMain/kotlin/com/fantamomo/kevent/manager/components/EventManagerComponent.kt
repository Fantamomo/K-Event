package com.fantamomo.kevent.manager.components

import kotlin.reflect.KClass

interface EventManagerComponent<C : EventManagerComponent<C>> {
    val key: Key<C>

    interface Key<C : EventManagerComponent<C>> {
        val clazz: KClass<C>
    }
}