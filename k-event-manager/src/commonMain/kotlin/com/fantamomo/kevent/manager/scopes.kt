package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.internal.HandlerEventScopeImpl

/**
 * Creates a new instance of `HandlerEventScope` with a specified parent scope.
 *
 *
 * @param parent The parent `HandlerEventScope` from which the new scope should inherit behavior.
 * @return A new instance of `HandlerEventScope` that is tied to the specified parent.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun HandlerEventScope(parent: HandlerEventScope): HandlerEventScope = HandlerEventScopeImpl(parent)

/**
 * Creates a new child scope derived from the current `HandlerEventScope`.
 *
 * @return A new instance of `HandlerEventScope` representing the child scope.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun HandlerEventScope.newScope(): HandlerEventScope = HandlerEventScopeImpl(this)