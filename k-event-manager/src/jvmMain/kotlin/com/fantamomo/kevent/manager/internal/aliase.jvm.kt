package com.fantamomo.kevent.manager.internal

import java.util.concurrent.ConcurrentHashMap

actual fun <K, V> concurrentMap(): MutableMap<K, V> = ConcurrentHashMap()