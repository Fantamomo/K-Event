@file:JvmName("EventManagerFactory")

package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.components.ComponentSet
import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ExceptionHandler
import com.fantamomo.kevent.manager.components.addIfAbsent

fun EventManager(): EventManager = EventManager(ComponentSet.of())

fun EventManager(components: EventManagerComponent<*>): EventManager {
    val component = components.addIfAbsent(ExceptionHandler.Empty)
    return DefaultEventManager(component)
}