package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ListenerParameterResolver

internal expect object InternalLoggerInjection {
    fun isActive(components: EventManagerComponent<*>): Boolean

    fun inject(): ListenerParameterResolver<*>
}