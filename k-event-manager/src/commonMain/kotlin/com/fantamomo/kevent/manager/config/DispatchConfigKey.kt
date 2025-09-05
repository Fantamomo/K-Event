package com.fantamomo.kevent.manager.config

data class DispatchConfigKey<T>(val name: String, val defaultValue: T) {
    companion object {
        val DISPATCH_DEAD_EVENT = key("dispatch_dead_event", true)
        val STICKY = key("sticky", false)

        private fun <T> key(name: String, defaultValue: T) = DispatchConfigKey(name, defaultValue)
    }
}