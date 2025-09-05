package com.fantamomo.kevent.manager.config

var DispatchConfigScope.dispatchDeadEvent: Boolean
    get() = getOrDefault(DispatchConfigKey.DISPATCH_DEAD_EVENT)
    set(value) = set(DispatchConfigKey.DISPATCH_DEAD_EVENT, value)

var DispatchConfigScope.sticky: Boolean
    get() = getOrDefault(DispatchConfigKey.STICKY)
    set(value) = set(DispatchConfigKey.STICKY, value)