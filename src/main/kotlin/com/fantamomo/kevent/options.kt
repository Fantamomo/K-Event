package com.fantamomo.kevent

var <E : Event> EventConfigurationScope<E>.priority: Priority
    get() = getOrDefault(Key.PRIORITY)
    set(value) = set(Key.PRIORITY, value)