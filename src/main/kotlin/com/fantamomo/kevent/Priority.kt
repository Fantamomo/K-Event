package com.fantamomo.kevent

sealed interface Priority {
    val priority: Int

    enum class Standard(override val priority: Int) : Priority {
        HIGHEST(1000),
        HIGH(500),
        NORMAL(0),
        LOW(-500),
        LOWEST(1000);

        companion object {
            private val BY_PRIORITY = entries.associateBy(Standard::priority)

            fun fromPriority(priority: Int) = BY_PRIORITY[priority]
        }
    }

    data class Custom(override val priority: Int) : Priority

    companion object {
        operator fun invoke(priority: Int): Priority = Standard.fromPriority(priority) ?: Custom(priority)
    }
}