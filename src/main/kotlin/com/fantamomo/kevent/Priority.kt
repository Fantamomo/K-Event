package com.fantamomo.kevent

import java.util.Collections

sealed interface Priority : Comparable<Priority> {
    val priority: Int

    override fun compareTo(other: Priority) = priority.compareTo(other.priority)

    sealed class Standard(override val priority: Int) : Priority {

        val name: String by lazy { this::class.simpleName!! }

        override fun equals(other: Any?) = other is Priority && other.priority == priority

        override fun hashCode() = priority

        override fun toString() = name

        object HIGHEST : Standard(1000)
        object HIGH : Standard(500)
        object NORMAL : Standard(0)
        object LOW : Standard(-500)
        object LOWEST : Standard(1000)

        companion object {
            private val entries by lazy { Collections.unmodifiableList(listOf(HIGHEST, HIGH, NORMAL, LOW, LOWEST)) }
            private val BY_PRIORITY by lazy { entries.associateBy(Standard::priority) }

            fun fromPriority(priority: Int) = BY_PRIORITY[priority]

            fun valueOf(value: String) = when (value) {
                "HIGHEST" -> HIGHEST
                "HIGH" -> HIGH
                "NORMAL" -> NORMAL
                "LOW" -> LOW
                "LOWEST" -> LOWEST
                else -> throw IllegalArgumentException("No object com.fantamomo.kevent.Priority.Standard.$value")
            }
        }
    }

    data class Custom(override val priority: Int) : Priority {
        override fun equals(other: Any?) = other is Priority && other.priority == priority
        override fun hashCode() = priority
    }

    companion object {
        operator fun invoke(priority: Int): Priority = Standard.fromPriority(priority) ?: Custom(priority)
    }
}