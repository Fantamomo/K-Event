package com.fantamomo.kevent.manager.internal

import com.fantamomo.kevent.manager.DefaultEventManager
import com.fantamomo.kevent.manager.components.EventManagerComponent
import com.fantamomo.kevent.manager.components.ListenerParameterResolver
import com.fantamomo.kevent.manager.settings.DISABLE_LOGGER_INJECTION
import com.fantamomo.kevent.manager.settings.Settings
import com.fantamomo.kevent.manager.settings.getSetting
import java.util.logging.Logger

internal actual object InternalLoggerInjection {
    val logger = Logger.getLogger(DefaultEventManager::class.java.name)
    private val injection = ListenerParameterResolver.static("logger", Logger::class, logger)

    actual fun isActive(components: EventManagerComponent<*>): Boolean = !components.getSetting(Settings.DISABLE_LOGGER_INJECTION)

    actual fun inject(): ListenerParameterResolver<*> = injection
}