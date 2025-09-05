package com.fantamomo.kevent.manager.config

/**
 * Specifies whether to dispatch [com.fantamomo.kevent.DeadEvent].
 *
 * If set to `true`, dead events will be dispatched and handled accordingly.
 * If set to `false`, they will not be dispatched.
 *
 * This property interacts with the underlying configuration within the [DispatchConfigScope],
 * leveraging [DispatchConfigKey.DISPATCH_DEAD_EVENT] to store and retrieve its value.
 *
 * The default value is determined by the configuration key's predefined default.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
var DispatchConfigScope.dispatchDeadEvent: Boolean
    get() = getOrDefault(DispatchConfigKey.DISPATCH_DEAD_EVENT)
    set(value) = set(DispatchConfigKey.DISPATCH_DEAD_EVENT, value)

/**
 * Represents a configuration property within the [DispatchConfigScope] that determines
 * if event dispatch behavior is persistent or non-persistent.
 *
 * When `true`, the "sticky" behavior for event dispatching is enabled, allowing events
 * to be retained and dispatched to listeners that subscribe after the event occurs.
 * When `false`, only currently active listeners will receive dispatched events.
 *
 * This property uses the [DispatchConfigKey.STICKY] key to store and retrieve its value.
 * The default value is determined by the dispatch configuration setup.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
var DispatchConfigScope.sticky: Boolean
    get() = getOrDefault(DispatchConfigKey.STICKY)
    set(value) = set(DispatchConfigKey.STICKY, value)