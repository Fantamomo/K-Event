package com.fantamomo.kevent.manager.internal

import java.lang.reflect.InvocationTargetException

actual fun Throwable.unbox(): Throwable = when (this) {
    is InvocationTargetException -> targetException
    else -> this
}