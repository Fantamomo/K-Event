package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.internal.HandlerEventScopeImpl

fun HandlerEventScope(parent: HandlerEventScope): HandlerEventScope = HandlerEventScopeImpl(parent)

fun HandlerEventScope.newScope(): HandlerEventScope = HandlerEventScopeImpl(this)