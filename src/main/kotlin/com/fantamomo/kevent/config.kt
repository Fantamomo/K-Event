package com.fantamomo.kevent

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun configuration(event: Event?, @EventDsl block: EventConfigurationScope.() -> Unit) {
    contract {
        returns() implies (event != null)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (event == null) {
        val scope = EventConfigurationScope()
        scope.block()
        throw EventConfigurationHolder(EventConfiguration(scope))
    }
}

@OptIn(ExperimentalContracts::class)
fun emptyConfiguration(event: Event?) {
    contract { returns() implies (event != null) }
    if (event == null) throw EventConfigurationHolder(EventConfiguration.DEFAULT)
}