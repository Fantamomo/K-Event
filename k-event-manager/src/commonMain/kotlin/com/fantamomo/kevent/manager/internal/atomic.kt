package com.fantamomo.kevent.manager.internal

import java.util.concurrent.atomic.AtomicReference
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Atomically updates the value of an [AtomicReference] by applying the provided transformation block.
 * The block is invoked at least once. The update operation will retry until successful, or until
 * the transformation results in no change (i.e., the new value is the same as the current value).
 *
 * @param V The type of the value held by this [AtomicReference].
 * @param block A function that computes a new value based on the current value.
 * @author Fantamomo
 * @since 1.17-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <V> AtomicReference<V>.update(block: (V) -> V) {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    while (true) {
        val cur = get() // Get the current value
        // update the value
        val upd = block(cur)
        // If nothing changed, stop
        if (cur === upd) return
        // Otherwise, update atomically
        if (compareAndSet(cur, upd)) return
    }
}

/**
 * Atomically modifies the value of an [AtomicReference] that references a collection.
 *
 * This function applies a provided transformation to the current value and attempts to update the reference
 * value atomically. The modification loop continues until the update successfully completes or the size of
 * the collection remains unchanged after applying the transformation.
 *
 * @param block A function that takes the current collection and produces a new collection to replace it.
 * @author Fantamomo
 * @since 1.17-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <V, C : Collection<V>> AtomicReference<C>.atomicModify(block: (C) -> C) {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    while (true) {
        val cur = get() // Get the current collection
        // update the collection
        val upd = block(cur)
        // If nothing changed, stop
        if (cur.size == upd.size) return
        // Otherwise, update atomically
        if (compareAndSet(cur, upd)) return
    }
}