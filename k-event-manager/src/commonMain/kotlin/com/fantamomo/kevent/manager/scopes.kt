package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.internal.HandlerEventScopeImpl

fun HandlerEventScope(eventManager: EventManager): HandlerEventScope = HandlerEventScopeImpl(eventManager)

fun EventManager.newScope(): HandlerEventScope = HandlerEventScopeImpl(this)