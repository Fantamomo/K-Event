package com.fantamomo.kevent.manager.debug

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener

interface EventRecorder : Listener {

    fun getAllEvents(): List<RecordedEvent>

    fun getEventCount(eventClass: Class<out Dispatchable>): Int

    fun getLastEvent(eventClass: Class<out Dispatchable>): RecordedEvent?

    fun clear()

    fun enable()

    fun disable()

    fun isEnabled(): Boolean

    fun getEventsOfType(eventClass: Class<out Dispatchable>): List<RecordedEvent>

    fun hasEventOfType(eventClass: Class<out Dispatchable>): Boolean

    fun getEventsByThread(threadName: String): List<RecordedEvent>

    fun getEventsInTimeRange(start: Long, end: Long): List<RecordedEvent>

    fun getMostFrequentEvents(limit: Int = 5): List<Pair<Class<out Dispatchable>, Int>>

    fun getAverageInterval(eventClass: Class<out Dispatchable>): Double

    fun allowOnlyEventTypes(vararg types: Class<out Dispatchable>)

    fun allowOnlyThreads(vararg names: String)

    fun clearLiveFilters()
}
