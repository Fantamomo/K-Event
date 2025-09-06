package com.fantamomo.kevent.manager.config

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.manager.config.DispatchConfig.DispatchConfigImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Modifies the current [DispatchConfig] using the provided DSL [block].
 *
 * This function creates a mutable scope from the current configuration data,
 * applies the modifications specified in the [block], and returns a new [DispatchConfig] instance
 * reflecting those changes.
 *
 * @param block A lambda with [DispatchConfigScope] as the receiver, used to define configuration modifications.
 * @return A new `DispatchConfig` instance with the updates applied.
 * @author Fantamomo
 * @since 1.10-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
inline fun <E : Dispatchable> DispatchConfig<E>.modify(block: DispatchConfigScope<E>.() -> Unit): DispatchConfig<E> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    if (this !is DispatchConfigImpl) return createDispatchConfig(block)
    val scope = DispatchConfigScope<E>(this.dataAsMutable())
    scope.block()
    return DispatchConfig(scope)
}

/**
 * Creates a new dispatch configuration using the provided configuration block.
 *
 * This function initializes a `DispatchConfigScope` for the generic type `E` and applies
 * the user-defined block to configure the dispatch settings. It uses Kotlin contracts to
 * ensure the block is called exactly once during execution.
 *
 * @param E The type of the dispatchable entity for which the dispatch configuration is defined.
 * This must extend from `Dispatchable`.
 * @param block A lambda with receiver of type `DispatchConfigScope<E>` that defines
 * the configuration logic for the dispatch settings.
 * @return A `DispatchConfig<E>` instance containing the configured dispatch settings.
 * @author Fantamomo
 * @since 1.10-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
inline fun <E : Dispatchable> createDispatchConfig(block: DispatchConfigScope<E>.() -> Unit): DispatchConfig<E> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return DispatchConfig(DispatchConfigScope<E>().apply(block))
}