package com.fantamomo.kevent

/**
 * Represents the priority of an event handler.
 * 
 * Priorities determine the order in which event handlers are executed when an event
 * is dispatched. Handlers with higher priority values are executed before those with
 * lower priority values.
 * 
 * The event system provides standard priorities ([HIGHEST], [HIGH], [NORMAL], [LOW], [LOWEST]),
 * but also allows for custom priorities with any integer value.
 * 
 * Example usage:
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
 * @property priority The integer value of this priority
 * 
 * @see EventConfigurationScope.priority
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
sealed interface Priority : Comparable<Priority> {
    /**
     * The integer value of this priority.
     * 
     * Higher values indicate higher priority (executed earlier).
     */
    val priority: Int

    /**
     * Compares this priority with another based on their integer values.
     * 
     * @param other The priority to compare with
     * @return A negative value if this priority is lower, zero if they are equal,
     *         or a positive value if this priority is higher
     */
    override fun compareTo(other: Priority) = priority.compareTo(other.priority)

    /**
     * Standard predefined priorities.
     * 
     * This sealed class contains commonly used priority levels as singleton objects.
     * 
     * @property priority The integer value of this priority
     */
    sealed class Standard(override val priority: Int) : Priority {
        /**
         * The name of this standard priority.
         */
        val name: String by lazy { this::class.simpleName!! }

        override fun equals(other: Any?) = other is Priority && other.priority == priority

        override fun hashCode() = priority

        override fun toString() = name

        /**
         * Highest priority level (1000).
         * Event handlers with this priority are executed first.
         */
        object HIGHEST : Standard(1000)

        /**
         * High priority level (500).
         */
        object HIGH : Standard(500)

        /**
         * Normal priority level (0).
         * This is the default priority if none is specified.
         */
        object NORMAL : Standard(0)

        /**
         * Low priority level (-500).
         */
        object LOW : Standard(-500)

        /**
         * Lowest priority level (-1000).
         * Event handlers with this priority are executed last (except for MONITOR).
         */
        object LOWEST : Standard(-1000)

        /**
         * Monitor priority level (Int.MIN_VALUE).
         * Event handlers with this priority are executed after all others and
         * are typically used for monitoring or logging purposes.
         */
        object MONITOR : Standard(Int.MIN_VALUE)

        companion object {
            private val entries by lazy { listOf(HIGHEST, HIGH, NORMAL, LOW, LOWEST) }
            private val BY_PRIORITY by lazy { entries.associateBy(Standard::priority) }

            /**
             * Finds a standard priority with the given integer value.
             * 
             * @param priority The integer value to look up
             * @return The standard priority with the given value, or null if none exists
             */
            fun fromPriority(priority: Int) = BY_PRIORITY[priority]

            /**
             * Finds a standard priority with the given name.
             * 
             * @param value The name of the priority to look up
             * @return The standard priority with the given name
             * @throws IllegalArgumentException if no standard priority with the given name exists
             */
            fun valueOf(value: String) = when (value) {
                "HIGHEST" -> HIGHEST
                "HIGH" -> HIGH
                "NORMAL" -> NORMAL
                "LOW" -> LOW
                "LOWEST" -> LOWEST
                "MONITOR" -> MONITOR
                else -> throw IllegalArgumentException("No object com.fantamomo.kevent.Priority.Standard.$value")
            }
        }
    }

    /**
     * Custom priority with a user-defined integer value.
     * 
     * This class allows for creating priorities with any integer value,
     * providing more fine-grained control over execution order than the
     * standard priorities.
     * 
     * @property priority The integer value of this priority
     */
    data class Custom(override val priority: Int) : Priority {
        override fun equals(other: Any?) = other is Priority && other.priority == priority
        override fun hashCode() = priority
    }

    companion object {
        /**
         * Creates a priority with the given integer value.
         * 
         * If the value matches a standard priority, that standard priority is returned.
         * Otherwise, a new custom priority is created.
         * 
         * @param priority The integer value for the priority
         * @return A priority with the given value
         */
        operator fun invoke(priority: Int): Priority = Standard.fromPriority(priority) ?: Custom(priority)

        /**
         * Highest priority level (1000).
         * Event handlers with this priority are executed first.
         */
        val HIGHEST = Standard.HIGHEST

        /**
         * High priority level (500).
         */
        val HIGH = Standard.HIGH

        /**
         * Normal priority level (0).
         * This is the default priority if none is specified.
         */
        val NORMAL = Standard.NORMAL

        /**
         * Low priority level (-500).
         */
        val LOW = Standard.LOW

        /**
         * Lowest priority level (-1000).
         * Event handlers with this priority are executed last (except for MONITOR).
         */
        val LOWEST = Standard.LOWEST

        /**
         * Monitor priority level (Int.MIN_VALUE).
         * Event handlers with this priority are executed after all others and
         * are typically used for monitoring or logging purposes.
         */
        val MONITOR = Standard.MONITOR
    }
}
