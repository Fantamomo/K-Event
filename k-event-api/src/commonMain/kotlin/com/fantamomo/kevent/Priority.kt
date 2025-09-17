package com.fantamomo.kevent

import com.fantamomo.kevent.Priority.Companion.HIGH
import com.fantamomo.kevent.Priority.Companion.HIGHEST
import com.fantamomo.kevent.Priority.Companion.LOW
import com.fantamomo.kevent.Priority.Companion.LOWEST
import com.fantamomo.kevent.Priority.Companion.MONITOR
import com.fantamomo.kevent.Priority.Companion.NORMAL


/**
 * Represents the priority of an event handler.
 *
 * Priorities determine the execution order of event handlers when an event is dispatched.
 * Handlers with higher priority values are executed before those with lower values.
 *
 * The system provides standard priorities ([HIGHEST], [HIGH], [NORMAL], [LOW], [LOWEST], [MONITOR])
 * and also supports custom priorities with arbitrary integer values.
 *
 * Example:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *         // or
 *         priority = Priority.Custom(750) // Between HIGH and HIGHEST
 *     }
 * }
 * ```
 *
 * @property priority The integer value of this priority.
 * @see EventConfigurationScope.priority
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
sealed interface Priority : Comparable<Priority> {

    /**
     * The integer value representing this priority.
     *
     * Higher values indicate higher priority and earlier execution.
     */
    val priority: Int

    /**
     * Compares this priority with another based on their integer values.
     *
     * @param other The priority to compare with.
     * @return Negative if lower, zero if equal, positive if higher.
     */
    override fun compareTo(other: Priority) = priority.compareTo(other.priority)

    /**
     * Standard predefined priorities as singleton objects.
     *
     * Includes commonly used levels: HIGHEST, HIGH, NORMAL, LOW, LOWEST, MONITOR.
     */
    sealed class Standard(override val priority: Int) : Priority {

        /**
         * The name of this standard priority (class simple name).
         */
        val name: String by lazy { this::class.simpleName!! }

        override fun equals(other: Any?) = other is Priority && other.priority == priority
        override fun hashCode() = priority
        override fun toString() = name

        /** Highest priority (1000). Executed first. */
        object HIGHEST : Standard(1000)

        /** High priority (500). */
        object HIGH : Standard(500)

        /** Normal priority (0). Default if none specified. */
        object NORMAL : Standard(0)

        /** Low priority (-500). */
        object LOW : Standard(-500)

        /** Lowest priority (-1000). Executed last (except MONITOR). */
        object LOWEST : Standard(-1000)

        /** Monitor priority (Int.MIN_VALUE). Executed after all others, for logging/monitoring. */
        object MONITOR : Standard(Int.MIN_VALUE)

        companion object {
            private val entries by lazy { listOf(HIGHEST, HIGH, NORMAL, LOW, LOWEST) }
            private val BY_PRIORITY by lazy { entries.associateBy(Standard::priority) }

            /**
             * Returns a standard priority matching the given integer value.
             *
             * @param priority The integer value to look up.
             * @return Corresponding standard priority or null if none exists.
             */
            fun fromPriority(priority: Int) = BY_PRIORITY[priority]

            /**
             * Returns a standard priority matching the given name.
             *
             * @param value The name of the priority.
             * @return Corresponding standard priority.
             * @throws IllegalArgumentException if no standard priority matches the name.
             */
            fun valueOf(value: String) = when (value) {
                "HIGHEST" -> HIGHEST
                "HIGH" -> HIGH
                "NORMAL" -> NORMAL
                "LOW" -> LOW
                "LOWEST" -> LOWEST
                "MONITOR" -> MONITOR
                else -> throw IllegalArgumentException("No object ${Standard::class.qualifiedName}.$value")
            }
        }
    }

    /**
     * Custom priority with a user-defined integer value.
     *
     * Allows fine-grained control beyond standard priorities.
     *
     * @property priority The integer value of this priority.
     */
    data class Custom(override val priority: Int) : Priority {
        override fun equals(other: Any?) = other is Priority && other.priority == priority
        override fun hashCode() = priority
    }

    companion object {
        /**
         * Creates a priority with the given integer value.
         *
         * Returns a standard priority if it matches the value, otherwise creates a custom one.
         *
         * @param priority The integer value for the priority.
         * @return A [Priority] instance corresponding to the value.
         */
        operator fun invoke(priority: Int): Priority = Standard.fromPriority(priority) ?: Custom(priority)

        /** Highest priority (1000). Executed first. */
        val HIGHEST = Standard.HIGHEST

        /** High priority (500). */
        val HIGH = Standard.HIGH

        /** Normal priority (0). Default. */
        val NORMAL = Standard.NORMAL

        /** Low priority (-500). */
        val LOW = Standard.LOW

        /** Lowest priority (-1000). Executed last (except MONITOR). */
        val LOWEST = Standard.LOWEST

        /** Monitor priority (Int.MIN_VALUE). Executed after all others. */
        val MONITOR = Standard.MONITOR
    }
}
