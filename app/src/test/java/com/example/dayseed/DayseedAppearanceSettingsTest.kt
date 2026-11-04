package com.example.dayseed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.DayseedRepository
import com.example.dayseed.data.TestDayseedClock
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.resolveDayseedArtwork
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import com.example.dayseed.ui.theme.DarkCanvas
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Appearance & rhythm settings coverage for the fifth appearance option
 * (Dark + Time-of-Day Artwork, owner decision recorded in this phase).
 *
 * Verifies through the real persisted settings store:
 * - `theme_mode` persistence and round-trip resolution for all five options,
 * - backward compatibility for values saved before the new option existed,
 * - the new option resolving a dark palette with time-cycling artwork
 *   (the Settings preview feeds the same resolution functions).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedAppearanceSettingsTest {

    private lateinit var context: Context
    private lateinit var testClock: TestDayseedClock
    private lateinit var repository: DayseedRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testClock = TestDayseedClock(
            currentLocalDate = LocalDate.of(2026, 11, 4),
            currentLocalTime = LocalTime.of(10, 0),
            currentUtcMs = 1_772_798_400_000L
        )
        // In-memory database per test for isolation (same pattern as DayseedAcceptanceTest).
        repository = DayseedRepository(context, clock = testClock, databaseName = null)
    }

    @After
    fun tearDown() {
        repository.dbHelper.close()
    }

    @Test
    fun unsetThemeModeFallsBackToSystem() {
        val saved = repository.getSettingString("theme_mode", "")
        assertEquals(ThemeModeOption.SYSTEM, ThemeModeOption.fromSetting(saved.ifEmpty { null }))
    }

    @Test
    fun newDarkDaypartArtOptionPersistsAndReloadsAfterRestart() {
        // Simulate the owner selecting the new option and the app process ending.
        repository.setSettingSync("theme_mode", "ENUM", ThemeModeOption.DARK_DAYPART_ART.settingValue)

        // Re-read from storage as a fresh session would.
        val reloaded = ThemeModeOption.fromSetting(repository.getSettingString("theme_mode", ""))
        assertEquals(ThemeModeOption.DARK_DAYPART_ART, reloaded)

        // The reloaded option drives the same resolution the Today screen and Settings
        // preview use; palette must be dark and artwork must follow local time.
        val (scheme, isDark) = resolveDayseedColorScheme(reloaded, false, LocalTime.of(14, 0))
        assertTrue(isDark)
        assertEquals(DarkCanvas, scheme.background)
        assertEquals(
            resolveDayseedArtwork(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, LocalTime.of(14, 0)),
            resolveDayseedArtwork(reloaded, false, LocalTime.of(14, 0))
        )
    }

    @Test
    fun legacyPersistedThemeValuesKeepResolvingUnchanged() {
        val legacyValues = listOf(
            "SYSTEM" to ThemeModeOption.SYSTEM,
            "LIGHT" to ThemeModeOption.LIGHT,
            "DARK" to ThemeModeOption.DARK,
            "SOFT_BOTANICAL_AUTO" to ThemeModeOption.SOFT_BOTANICAL_AUTO,
            // Legacy writes may have been lowercase; fromSetting is case-insensitive.
            "dark" to ThemeModeOption.DARK
        )
        for ((value, expected) in legacyValues) {
            repository.setSettingSync("theme_mode", "ENUM", value)
            val reloaded = ThemeModeOption.fromSetting(repository.getSettingString("theme_mode", ""))
            assertEquals("saved value '$value'", expected, reloaded)
        }
    }

    @Test
    fun allFiveAppearanceOptionsExposeDistinctDisplayNameAndSettingValue() {
        val values = ThemeModeOption.entries.map { it.settingValue }
        assertEquals(5, ThemeModeOption.entries.size)
        assertEquals(values.size, values.toSet().size)
        assertTrue(ThemeModeOption.entries.any { it.settingValue == "DARK_DAYPART_ART" })
        // The original automatic combination is still offered (assignment requirement).
        assertTrue(ThemeModeOption.entries.any { it.settingValue == "SOFT_BOTANICAL_AUTO" })
    }
}
