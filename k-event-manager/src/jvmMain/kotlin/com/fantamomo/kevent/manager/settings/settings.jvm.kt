package com.fantamomo.kevent.manager.settings


/**
 * A setting that disables the injection of the `logger:` [java.util.logging.Logger] state into parameter resolution during
 * event dispatching.
 *
 * By default, the value is `false`, meaning the `scope` injection is enabled. Setting this to
 * `true` will turn off this behavior.
 */
val Settings.DISABLE_LOGGER_INJECTION get() = SettingsEntry("DISABLE_LOGGER_INJECTION", false)