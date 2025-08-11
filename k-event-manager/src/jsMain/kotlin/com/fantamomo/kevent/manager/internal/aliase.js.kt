package com.fantamomo.kevent.manager.internal

actual fun <K, V> concurrentMap(): MutableMap<K, V> = mutableMapOf()