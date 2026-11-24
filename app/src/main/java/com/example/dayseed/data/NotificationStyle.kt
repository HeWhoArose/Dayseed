package com.example.dayseed.data

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.example.dayseed.ui.theme.BotanicalDaypart
import com.example.dayseed.ui.theme.SoftBotanicalThemeConfig
import com.example.dayseed.ui.theme.ThemeModeOption
import com.example.dayseed.ui.theme.greetingFor
import com.example.dayseed.ui.theme.resolveDayseedColorScheme
import java.time.LocalTime

/**
 * How a reminder notification looks (D-245): it follows the appearance the person chose.
 *
 * The accent colour (status-bar tint, expanded header, light colour) is the chosen theme's brand colour for the
 * current time of day, and the large icon is a small seedling tile painted in that theme's own canvas and brand.
 * The status-bar icon itself must be one colour (Android draws it tinted), so a distinct seedling shape stands in
 * for a multi-colour design there.
 *
 * Only the theme *name* is remembered here (a tiny preference written when the theme changes), so the alarm
 * receiver can style a notification without opening the database.
 */
internal object NotificationStyle {
    private const val PREFS = "dayseed_ui_prefs"
    private const val KEY_THEME = "theme_mode"

    data class Look(
        val accent: Int,
        val canvas: Int,
        val daypart: BotanicalDaypart,
        val greeting: String
    )

    fun rememberTheme(context: Context, mode: ThemeModeOption) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode.settingValue).apply()
    }

    fun rememberedTheme(context: Context): ThemeModeOption =
        ThemeModeOption.fromSetting(
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        )

    fun lookFor(context: Context, now: LocalTime = LocalTime.now()): Look = lookFor(rememberedTheme(context), isSystemDark(context), now)

    fun lookFor(mode: ThemeModeOption, systemDark: Boolean, now: LocalTime): Look {
        val (scheme, _) = resolveDayseedColorScheme(mode, systemDark, now)
        val daypart = SoftBotanicalThemeConfig.resolveDaypart(now)
        return Look(
            accent = scheme.primary.toArgb(),
            canvas = scheme.background.toArgb(),
            daypart = daypart,
            greeting = greetingFor(daypart)
        )
    }

    private fun isSystemDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** A 192 px rounded tile in the theme's canvas colour with a seedling in its brand colour. */
    fun largeIcon(look: Look, sizePx: Int = 192): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = sizePx / 24f
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = look.canvas }
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), 5f * s, 5f * s, tile)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = look.accent; style = Paint.Style.STROKE; strokeWidth = 0.7f * s }
        canvas.drawRoundRect(RectF(0.35f * s, 0.35f * s, sizePx - 0.35f * s, sizePx - 0.35f * s), 4.7f * s, 4.7f * s, ring)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = look.accent; style = Paint.Style.FILL }
        canvas.save()
        canvas.translate(0f, 1.2f * s)
        canvas.drawRect(11f * s, 12f * s, 13f * s, 21f * s, ink)
        val left = Path().apply {
            moveTo(11f * s, 13f * s); cubicTo(6f * s, 13f * s, 3f * s, 10f * s, 3f * s, 5f * s)
            cubicTo(8f * s, 5f * s, 11f * s, 8f * s, 11f * s, 13f * s); close()
        }
        val right = Path().apply {
            moveTo(13f * s, 11f * s); cubicTo(13f * s, 6f * s, 16f * s, 3f * s, 21f * s, 3f * s)
            cubicTo(21f * s, 8f * s, 18f * s, 11f * s, 13f * s, 11f * s); close()
        }
        canvas.drawPath(left, ink)
        canvas.drawPath(right, ink)
        canvas.restore()
        return bmp
    }
}
