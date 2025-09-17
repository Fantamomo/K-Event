package com.fantamomo.kevent.manager.internal

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

/**
 * Determines the most specific common superclass of a collection of classes.
 *
 * This function finds the most specific class that is a superclass or equal to all input classes.
 * If the collection is empty, it returns null. If the collection contains a single class,
 * that class is returned. If all input classes are the same, that class is returned.
 * If no common superclass is found, the `Any` class is returned.
 *
 * @param classes A collection of classes to determine the common superclass for.
 * @return The most specific common superclass, or null if the collection is empty.
 * @author Fantamomo
 * @since 1.15-SNAPSHOT
 */
fun commonSuperClass(classes: Collection<KClass<*>>): KClass<*>? {
    if (classes.isEmpty()) return null

    val ref = classes.first()

    if (classes.size == 1) return ref

    if (classes.all { it == ref }) return ref

    val refChain = generateSequence(ref) { it.superclass }.toList()

    for (candidate in refChain) {
        if (classes.all { it.isSubclassOf(candidate) }) {
            return candidate
        }
    }

    return Any::class
}

/**
 * Extension property to retrieve the primary superclass of a Kotlin class.
 *
 * This property computes the direct superclass of a Kotlin class while skipping
 * interfaces. It inspects the supertypes of the class and retrieves the first
 * non-interface type in the supertypes list.
 *
 * @receiver A Kotlin class whose superclass is to be determined.
 * @return The primary superclass of the class, or `null` if there is none.
 * @author Fantamomo
 * @since 1.15-SNAPSHOT
 */
private val <T : Any> KClass<T>.superclass: KClass<*>?
    get() = this.supertypes.asSequence()
        .mapNotNull { it.classifier as? KClass<*> }
        .firstOrNull { !it.java.isInterface }