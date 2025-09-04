package com.fantamomo.kevent.manager.internal

/**
 * Checks if all elements in the iterable are `true`.
 *
 * The function will return:
 * - `true` if the iterable is empty.
 * - `true` if all elements in the iterable evaluate to `true`.
 * - `false` if any element in the iterable evaluates to `false`.
 *
 * @return `true` if all elements are `true` or if the iterable is empty, otherwise `false`.
 *
 * @author Fantamomo
 * @since 1.6-SNAPSHOT
 */
internal fun Iterable<Boolean>.all(): Boolean {
    if (this is Collection && isEmpty()) return true
    for (element in this) if (!element) return false
    return true
}