package com.fantamomo.kevent

fun <E : Event, T> EventConfigurationScope<E>.getOrDefault(key: Key<T>): T = get(key) ?: key.defaultValue