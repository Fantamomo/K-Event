package com.fantamomo.kevent.manager.settings

import kotlin.reflect.KClass

/**
 * Singleton object containing configurable settings for event dispatching behavior.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
object Settings {
    /**
     * A setting that disables the injection of the `isWaiting:` [Boolean] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `isWaiting` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_IS_WAITING_INJECTION by setting(false)
    /**
     * A setting that disables the injection of the `manager:` [com.fantamomo.kevent.manager.EventManager] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `manager` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_EVENTMANAGER_INJECTION by setting(false)
    /**
     * A setting that disables the injection of the `scope:` [kotlinx.coroutines.CoroutineScope] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `scope` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_SCOPE_INJECTION by setting(false)
    /**
     * A setting that disables the injection of the `logger:` [java.util.logging.Logger] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `scope` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_LOGGER_INJECTION by setting(false)

    /**
     * Configures whether dead events should be dispatched by the event manager.
     *
     * A dead event occurs when an event is posted to the event manager but no
     * listener is registered to handle it. When this setting is enabled (`true`),
     * the event manager dispatches such events as dead events, allowing specific
     * listeners for dead events to handle them. If disabled (`false`), dead events
     * are ignored and not dispatched.
     *
     * By default, this setting is enabled.
     */
    val DISPATCH_DEAD_EVENTS by setting(true)


    private inline fun <reified T> setting(name: String, defaultValue: T) = SettingsEntry<T>(name, defaultValue)
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> setting(defaultValue: T) = LazySettingsEntry(T::class as KClass<T & Any>, defaultValue)
}