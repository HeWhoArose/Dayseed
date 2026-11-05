package com.example.dayseed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.dayseed.ui.screens.SettingsSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Per-authority cursor behaviours for the standalone test content provider. The provider
 * class is constructed by Robolectric ([Robolectric.setupContentProvider]) without arguments,
 * so query behaviour is supplied through this registry.
 */
private val providerBehaviours = mutableMapOf<String, () -> Cursor?>()

private class RegistryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = providerBehaviours[uri.authority]?.invoke()
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

private fun displayNameCursor(vararg values: String?): Cursor =
    MatrixCursor(arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)).apply {
        addRow(values)
    }

/**
 * Phase 5 remediation coverage: SAF document display names are resolved via
 * OpenableColumns.DISPLAY_NAME and handled gracefully when a provider answers differently
 * (null name, missing column, empty cursor, or a throwing provider — providers are NOT
 * assumed to behave identically).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayseedSafFilenameFeedbackTest {

    private lateinit var contentResolver: android.content.ContentResolver

    @Before
    fun setUp() {
        contentResolver = ApplicationProvider.getApplicationContext<android.content.Context>()
            .contentResolver
    }

    private fun registerProvider(authority: String, behaviour: () -> Cursor?) {
        providerBehaviours[authority] = behaviour
        // Robolectric registers the standalone provider under this exact authority string.
        Robolectric.setupContentProvider(RegistryProvider::class.java, authority)
    }

    private fun uriFor(authority: String) = Uri.parse("content://$authority/document/backups")

    @Test
    fun createDocumentProviderAnswersWithActualDisplayNameIncludingNumberedDuplicate() {
        // Simulates Android's numbered-duplicate behaviour: the user picked "backup-2026" but
        // the provider created "backup-2026 (1)".
        registerProvider(
            "dayseed_duplicate_name"
        ) { displayNameCursor("dayseed-backup-2026-03-15 (1).dayseed.json") }
        val name = SettingsSupport.resolveDocumentDisplayName(
            contentResolver, uriFor("dayseed_duplicate_name")
        )
        assertEquals("dayseed-backup-2026-03-15 (1).dayseed.json", name)
    }

    @Test
    fun openDocumentProviderAnswersWithSelectedFileName() {
        registerProvider(
            "dayseed_open_name"
        ) { displayNameCursor("my-trip-backup.dayseed.json") }
        assertEquals(
            "my-trip-backup.dayseed.json",
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_open_name"))
        )
    }

    @Test
    fun providerReportingNullDisplayNameIsHandledGracefully() {
        registerProvider("dayseed_null_name") { displayNameCursor(null as String?) }
        assertNull(
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_null_name"))
        )
    }

    @Test
    fun providerAnsweringWithBlankDisplayNameIsHandledGracefully() {
        registerProvider("dayseed_blank_name") { displayNameCursor("   ") }
        assertNull(
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_blank_name"))
        )
    }

    @Test
    fun providerWithEmptyCursorIsHandledGracefully() {
        registerProvider("dayseed_empty_cursor") { displayNameCursor() }
        assertNull(
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_empty_cursor"))
        )
    }

    @Test
    fun throwingProviderIsHandledGracefullyAsUnknownName() {
        registerProvider("dayseed_throwing") {
            throw IllegalStateException("provider failure")
        }
        assertNull(
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_throwing"))
        )
    }

    @Test
    fun providerWithoutTheDisplayNameColumnIsHandledGracefully() {
        // Some providers may return a cursor with a DIFFERENT column set.
        registerProvider("dayseed_other_column") {
            MatrixCursor(arrayOf("_data")).apply { addRow(arrayOf("/tmp/x.json")) }
        }
        assertNull(
            SettingsSupport.resolveDocumentDisplayName(contentResolver, uriFor("dayseed_other_column"))
        )
    }
}
