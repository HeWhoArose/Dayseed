package com.example.dayseed

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.NotificationStyle
import com.example.dayseed.data.ReminderNotifier
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalTime

/** Reminder notifications follow the chosen theme, greet by the time of day, and use Dayseed's own chime (D-245). */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class NotificationStyleTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test
    fun accentFollowsTheChosenThemeAndTheTimeOfDay() {
        // Time-of-day theme: clay at 5pm, leaf green in the morning.
        assertEquals(SoftBotanicalThemeConfig.EveningBrand.toArgb(), NotificationStyle.lookFor(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(17, 30)).accent)
        assertEquals(SoftBotanicalThemeConfig.MorningBrand.toArgb(), NotificationStyle.lookFor(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(8)).accent)
        // Always-dark botanical theme uses the dark evening brand; plain Light and Dark never change with the hour.
        assertEquals(SoftBotanicalThemeConfig.EveningDarkBrand.toArgb(), NotificationStyle.lookFor(ThemeModeOption.DARK_DAYPART_ART, false, t(17, 30)).accent)
        assertEquals(
            NotificationStyle.lookFor(ThemeModeOption.LIGHT, false, t(8)).accent,
            NotificationStyle.lookFor(ThemeModeOption.LIGHT, false, t(22)).accent
        )
        assertNotEquals(
            NotificationStyle.lookFor(ThemeModeOption.LIGHT, false, t(8)).accent,
            NotificationStyle.lookFor(ThemeModeOption.DARK, false, t(8)).accent
        )
        // Different themes give different looks at the same moment.
        val looks = ThemeModeOption.entries.map { NotificationStyle.lookFor(it, false, t(17, 30)).accent to NotificationStyle.lookFor(it, false, t(17, 30)).canvas }
        assertTrue(looks.toSet().size >= 4)
    }

    @Test
    fun everyThemeGreetsByTheTimeOfDay() {
        for (mode in ThemeModeOption.entries) {
            assertEquals("Good morning", NotificationStyle.lookFor(mode, false, t(7)).greeting)
            assertEquals("Good afternoon", NotificationStyle.lookFor(mode, false, t(13)).greeting)
            assertEquals("Good evening", NotificationStyle.lookFor(mode, false, t(17)).greeting)
            assertEquals("Good night", NotificationStyle.lookFor(mode, false, t(22)).greeting)
        }
        assertEquals(BotanicalDaypart.EVENING, NotificationStyle.lookFor(ThemeModeOption.LIGHT, false, t(16)).daypart)
    }

    @Test
    fun postedReminderCarriesTheThemeAccentSeedlingIconGreetingAndLargeTile() {
        NotificationStyle.rememberTheme(context, ThemeModeOption.DARK)
        assertEquals(ThemeModeOption.DARK, NotificationStyle.rememberedTheme(context))
        assertTrue(ReminderNotifier.postHabitReminder(context, "tag", 1, "Read"))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(nm).allNotifications.single()
        assertEquals(R.drawable.ic_stat_dayseed, posted.smallIcon.resId)
        assertNotNull("themed large icon tile", posted.getLargeIcon())
        assertEquals(ReminderNotifier.CHANNEL_ID, posted.channelId)
        val greeting = posted.extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT).toString()
        assertTrue(greeting, greeting.startsWith("Dayseed · Good "))
        assertEquals(NotificationStyle.lookFor(ThemeModeOption.DARK, false, LocalTime.now()).accent, posted.color)

        // Changing the theme changes the next notification.
        NotificationStyle.rememberTheme(context, ThemeModeOption.LIGHT)
        ReminderNotifier.postHabitReminder(context, "tag2", 2, "Walk")
        val second = shadowOf(nm).getNotification("tag2", 2)
        assertNotEquals(posted.color, second.color)
        assertEquals(NotificationStyle.lookFor(ThemeModeOption.LIGHT, false, LocalTime.now()).accent, second.color)
    }

    @Test
    fun channelUsesTheUniqueChimeAndAGentleVibration() {
        ReminderNotifier.ensureReminderChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(ReminderNotifier.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("android.resource://${context.packageName}/${R.raw.dayseed_chime}", channel.sound.toString())
        assertEquals(android.media.AudioAttributes.USAGE_NOTIFICATION, channel.audioAttributes.usage)
        assertTrue(channel.shouldVibrate())
        assertNotEquals("legacy channel id is retired", "dayseed_reminders", ReminderNotifier.CHANNEL_ID)
        assertEquals(null, nm.getNotificationChannel("dayseed_reminders"))
    }

    @Test
    fun theChimeIsARealShortWavFile() {
        context.resources.openRawResource(R.raw.dayseed_chime).use { input ->
            val bytes = input.readBytes()
            assertEquals("RIFF", String(bytes, 0, 4))
            assertEquals("WAVE", String(bytes, 8, 4))
            assertTrue("chime is under 3 seconds of 22.05 kHz mono (${bytes.size} bytes)", bytes.size in 20_000..140_000)
        }
    }

    @Test
    fun largeIconTileIsPaintedInTheThemeColours() {
        val look = NotificationStyle.lookFor(ThemeModeOption.SOFT_BOTANICAL_AUTO, false, t(17, 30))
        val bmp = NotificationStyle.largeIcon(look, 96)
        assertEquals(96, bmp.width)
        // The centre-left of the tile (outside the seedling) is the theme canvas; the stem pixel is the accent.
        assertEquals(look.canvas, bmp.getPixel(48, 10))
        assertEquals(look.accent, bmp.getPixel(48, 66))
    }
}
