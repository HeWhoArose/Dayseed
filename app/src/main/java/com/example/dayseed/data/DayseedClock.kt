package com.example.dayseed.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Injectable clock abstraction (D-185 / Section 4.1) for deterministic local-date,
 * local-time, timezone, and UTC epoch millisecond evaluation across the app and Robolectric tests.
 */
interface DayseedClock {
    fun nowUtcMs(): Long
    fun todayLocal(): LocalDate
    fun nowLocalTime(): LocalTime
    fun zoneId(): ZoneId

    /**
     * Backward-compatible aliases so any caller using either naming convention works cleanly.
     */
    fun currentLocalDate(): LocalDate = todayLocal()
    fun currentLocalTime(): LocalTime = nowLocalTime()
    fun currentZoneId(): ZoneId = zoneId()

    /**
     * Returns true if the current device clock has moved backwards relative to [lastKnownUtcMs]
     * by more than 60 seconds, indicating uncertain local clock confidence (D-185).
     */
    fun isClockUncertain(lastKnownUtcMs: Long): Boolean {
        if (lastKnownUtcMs <= 0L) return false
        return nowUtcMs() + 60_000L < lastKnownUtcMs
    }

    fun isTimeConfidenceUncertain(lastObservedUtcMs: Long?): Boolean {
        if (lastObservedUtcMs == null) return false
        return isClockUncertain(lastObservedUtcMs)
    }
}

class SystemDayseedClock : DayseedClock {
    override fun nowUtcMs(): Long = System.currentTimeMillis()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
    override fun todayLocal(): LocalDate = Instant.ofEpochMilli(nowUtcMs()).atZone(zoneId()).toLocalDate()
    override fun nowLocalTime(): LocalTime = Instant.ofEpochMilli(nowUtcMs()).atZone(zoneId()).toLocalTime()
}

/**
 * Controllable clock for unit and Robolectric tests.
 */
class TestDayseedClock(
    var currentLocalDate: LocalDate = LocalDate.of(2026, 3, 15),
    var currentLocalTime: LocalTime = LocalTime.of(10, 0),
    var currentUtcMs: Long = 1_773_568_800_000L,
    var fixedZoneId: ZoneId = ZoneId.of("UTC")
) : DayseedClock {
    override fun nowUtcMs(): Long = currentUtcMs
    override fun todayLocal(): LocalDate = currentLocalDate
    override fun nowLocalTime(): LocalTime = currentLocalTime
    override fun zoneId(): ZoneId = fixedZoneId

    fun advanceMillis(deltaMs: Long) {
        currentUtcMs += deltaMs
    }

    fun setLocalDateAndTime(date: LocalDate, time: LocalTime = LocalTime.of(9, 0)) {
        currentLocalDate = date
        currentLocalTime = time
        currentUtcMs = date.atTime(time).atZone(fixedZoneId).toInstant().toEpochMilli()
    }
}
