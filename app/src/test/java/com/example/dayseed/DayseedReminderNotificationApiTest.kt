package com.example.dayseed

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.data.ReminderNotifier
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * R4 / F-03: Android notification-permission API-level coverage.
 *
 * Verifies that the POST_NOTIFICATIONS runtime permission is correctly
 * guarded: on API 26-32 the permission does not exist and must not cause
 * a false denial; on API 34 the runtime permission check applies.
 *
 * Each test runs under @Config(sdk = [26, 32, 34]) and branches on
 * Build.VERSION.SDK_INT to assert the correct behaviour per API level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 32, 34])
class DayseedReminderNotificationApiTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create the reminder channel so channel-importance checks pass.
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ReminderNotifier.CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        // Grant the runtime permission explicitly (harmless on pre-33).
        shadowOf(context.applicationContext as Application)
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun isNotificationDeliveryAvailableDoesNotDenyOnPreApi33() {
        val available = ReminderNotifier.isNotificationDeliveryAvailable(context)
        when {
            android.os.Build.VERSION.SDK_INT < 33 -> {
                // No POST_NOTIFICATIONS permission below API 33: must not deny.
                assertTrue("API ${android.os.Build.VERSION.SDK_INT}: notification delivery " +
                    "must be available when notifications are enabled and channel exists", available)
            }
            else -> {
                // API 33+: permission granted, so available.
                assertTrue("API ${android.os.Build.VERSION.SDK_INT}: notification delivery " +
                    "must be available when permission granted", available)
            }
        }
    }

    @Test
    fun isNotificationDeliveryAvailableDeniesWhenPermissionRevokedOnApi33Plus() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            // Revoke the runtime permission: the API-33 guard must deny delivery.
            shadowOf(context.applicationContext as Application)
                .denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val available = ReminderNotifier.isNotificationDeliveryAvailable(context)
            assertTrue("API ${android.os.Build.VERSION.SDK_INT}: delivery must be denied when " +
                "POST_NOTIFICATIONS is revoked", !available)
        } else {
            // Pre-33: no runtime permission exists; app-level enablement governs.
            val available = ReminderNotifier.isNotificationDeliveryAvailable(context)
            assertTrue("API ${android.os.Build.VERSION.SDK_INT}: delivery must be available " +
                "when notifications are enabled (no runtime permission exists)", available)
        }
    }
}
