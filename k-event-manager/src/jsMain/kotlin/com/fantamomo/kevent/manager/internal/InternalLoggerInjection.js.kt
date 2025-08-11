package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ListenerParameterResolver

internal actual object InternalLoggerInjection {
    actual fun isActive(components: EventManagerComponent<*>): Boolean = false

    actual fun inject(): ListenerParameterResolver<*> = throw UnsupportedOperationException("JS does not support logger injection")
}