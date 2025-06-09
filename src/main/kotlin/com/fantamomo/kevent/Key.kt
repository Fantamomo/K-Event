package com.fantamomo.kevent

import kotlin.reflect.KClass

data class Key<T>(val key: String, val type: KClass<T & Any>, val defaultValue: T) {
    companion object {
        val PRIORITY = Key<Priority>("priority", Priority.Standard.NORMAL)

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(key: String, defaultValue: T): Key<T> =
            Key(key, T::class as KClass<T & Any>, defaultValue)
    }
}