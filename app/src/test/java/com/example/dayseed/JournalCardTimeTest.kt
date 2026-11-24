package com.example.dayseed

import com.example.dayseed.ui.screens.journalCardTimeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class JournalCardTimeTest {
    private val day = LocalDate.of(2026, 11, 14)
    private val at930 = day.atTime(9, 30).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun plainTwelveHourTime() =
        assertEquals("9:30 AM", journalCardTimeText(at930, day, false, ZoneOffset.UTC, Locale.US))

    @Test fun twentyFourHourTime() =
        assertEquals("09:30", journalCardTimeText(at930, day, true, ZoneOffset.UTC, Locale.US))

    @Test fun backDatedEntryShowsNoTime() =
        assertNull(journalCardTimeText(at930, day.minusDays(3), false, ZoneOffset.UTC, Locale.US))

    @Test fun missingTimestampShowsNoTime() =
        assertNull(journalCardTimeText(0L, day, false, ZoneOffset.UTC, Locale.US))
}
