@file:JvmName("EventManagerFactory")

package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.components.ComponentSet
import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ExceptionHandler
import com.fantamomo.kevent.manager.components.addIfAbsent

fun EventManager(defaultParameterInjection: Boolean = true): EventManager = EventManager(ComponentSet.of(), defaultParameterInjection)

fun EventManager(components: EventManagerComponent<*>, defaultParameterInjection: Boolean = true): EventManager {
    val component = components.addIfAbsent(ExceptionHandler.Empty)
    return DefaultEventManager(component, defaultParameterInjection)
}