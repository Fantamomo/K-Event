package com.fantamomo.kevent

import kotlin.reflect.KClass

data class Key<T>(val key: String, val type: KClass<T & Any>, val defaultValue: T)