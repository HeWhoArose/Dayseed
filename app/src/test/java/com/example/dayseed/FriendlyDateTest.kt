package com.example.dayseed

import com.example.dayseed.ui.components.friendlyDate
import com.example.dayseed.ui.components.friendlyDateRange
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class FriendlyDateTest {
    private lateinit var saved: Locale

    @Before fun pinLocale() { saved = Locale.getDefault(); Locale.setDefault(Locale.US) }
    @After fun restoreLocale() { Locale.setDefault(saved) }

    private val today = LocalDate.of(2026, 11, 14)

    @Test fun currentYearOmitsTheYear() {
        assertEquals("Sat, Nov 14", friendlyDate(LocalDate.of(2026, 11, 14), today))
        assertEquals("Fri, Nov 13", friendlyDate("2026-11-13", today))
    }

    @Test fun otherYearsShowTheYear() {
        assertEquals("Wed, Dec 31, 2025", friendlyDate(LocalDate.of(2025, 12, 31), today))
    }

    @Test fun neverShowsIsoFormatToPeople() {
        assertFalse(friendlyDate("2026-11-13", today).contains("2026-11"))
    }

    @Test fun unparseableTextIsReturnedUnchangedRatherThanCrashing() {
        assertEquals("not a date", friendlyDate("not a date", today))
    }

    @Test fun weekRangeIsCompactAndAddsYearsOnlyAcrossYears() {
        assertEquals("Nov 9 – Nov 15", friendlyDateRange("2026-11-09", "2026-11-15"))
        assertEquals("Dec 29, 2025 – Jan 4, 2026", friendlyDateRange("2025-12-29", "2026-01-04"))
    }
}
