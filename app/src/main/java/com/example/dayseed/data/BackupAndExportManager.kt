package com.example.dayseed.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class JournalExportFormat {
    MARKDOWN,
    CSV,
    JSON
}

object BackupCryptoConfig {
    // Provisional encryption parameters isolated in one configuration object (D-106 / D-196)
    const val ENVELOPE_FORMAT = "dayseed_encrypted_backup_v1"
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val KDF_ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_BYTES = 16
    const val IV_BYTES = 12
    const val GCM_TAG_BITS = 128
    const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
}

/**
 * Centralised resource and cryptographic limits for the backup importer (F-02).
 *
 * Every generated backup uses values inside these accepted ranges, so
 * legitimate backups produced by this app are never rejected. Values
 * outside these bounds indicate a malformed or adversarial file and must
 * be rejected before any expensive cryptographic work.
 */
object BackupLimits {
    // Maximum accepted backup size. 8 MiB is the initial limit; it must be
    // validated against realistic long-term user datasets before adoption
    // is finalised (R2 work item).
    const val MAX_BACKUP_SIZE_BYTES = 8L * 1024 * 1024

    // KDF iteration bounds (inclusive).
    const val MIN_KDF_ITERATIONS = 10_000
    const val MAX_KDF_ITERATIONS = 600_000

    // Accepted key lengths (bits).
    val ACCEPTED_KEY_LENGTH_BITS = setOf(128, 256)

    // Accepted GCM tag lengths (bits).
    val ACCEPTED_GCM_TAG_BITS = setOf(96, 104, 112, 120, 128)

    // Exact salt / IV sizes (bytes).
    const val SALT_BYTES_EXACT = 16
    const val IV_BYTES_EXACT = 12
}

/**
 * Thrown when a backup file is rejected before any expensive processing.
 * Application data is never modified when this exception propagates.
 */
class BackupRejectedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Reads a text file with a hard size cap, throwing [BackupRejectedException]
 * when the file exceeds [BackupLimits.MAX_BACKUP_SIZE_BYTES]. Replaces
 * unbounded File.readBytes()/readText() call sites in the backup import path.
 */
fun readBackupFileBounded(file: File, maxSizeBytes: Long = BackupLimits.MAX_BACKUP_SIZE_BYTES): String {
    val size = file.length()
    if (size > maxSizeBytes) {
        throw BackupRejectedException("Backup file size $size bytes exceeds limit of $maxSizeBytes bytes")
    }
    return file.readText(StandardCharsets.UTF_8)
}

/**
 * Reads from an [InputStream] with a hard byte cap, throwing
 * [BackupRejectedException] when the stream exceeds [maxBytes]. Used for
 * SAF document streams where the file length is not known up front.
 */
fun InputStream.readBoundedBytes(maxBytes: Long): ByteArray {
    val buf = ByteArray((maxBytes + 1).toInt())
    var total = 0
    while (true) {
        val read = read(buf, total, (buf.size - total).coerceAtLeast(1))
        if (read == -1) break
        total += read
        if (total > maxBytes) {
            throw BackupRejectedException("Backup stream size exceeds limit of $maxBytes bytes")
        }
    }
    return buf.copyOfRange(0, total)
}

object BackupAndExportManager {

    const val BACKUP_FORMAT_VERSION = 3
    const val BACKUP_SCHEMA_VERSION = 3

    /**
     * Readable journal export (D-105 / D-200): exports saved journal_entries ONLY.
     * Never includes journal_drafts, habits, settings, or reminder ledgers.
     */
    fun exportSavedJournals(
        entries: List<JournalEntry>,
        habitsById: Map<String, String>,
        format: JournalExportFormat
    ): String {
        val sorted = entries.sortedWith(compareByDescending<JournalEntry> { it.localDate }.thenByDescending { it.createdAtUtcMs })
        return when (format) {
            JournalExportFormat.MARKDOWN -> buildString {
                append("# Dayseed Journal Archive\n\n")
                for (entry in sorted) {
                    append("## ${entry.localDate}")
                    if (!entry.title.isNullOrBlank()) {
                        append(" — ${entry.title}")
                    }
                    append("\n")
                    val moods = listOfNotNull(entry.primaryMoodCode, entry.secondaryMoodCode)
                        .map { MoodCatalog.findByCode(it)?.let { m -> "${m.symbol} ${m.label}" } ?: it }
                    if (moods.isNotEmpty()) {
                        append("- **Mood:** ${moods.joinToString(", ")}\n")
                    }
                    if (!entry.linkedHabitId.isNullOrBlank()) {
                        val habitName = habitsById[entry.linkedHabitId] ?: entry.linkedHabitId
                        append("- **Linked habit:** $habitName\n")
                    }
                    if (!entry.promptId.isNullOrBlank()) {
                        val promptText = DailyPrompts.promptTextForId(entry.promptId) ?: entry.promptId
                        append("- **Prompt:** $promptText\n")
                    }
                    append("\n${entry.body}\n\n---\n\n")
                }
            }

            JournalExportFormat.CSV -> buildString {
                append("journal_entry_id,local_date,title,primary_mood_code,secondary_mood_code,prompt_id,linked_habit_id,body,created_at_utc_ms,updated_at_utc_ms\n")
                for (e in sorted) {
                    append(
                        listOf(
                            csvEscape(e.journalEntryId),
                            csvEscape(e.localDate),
                            csvEscape(e.title ?: ""),
                            csvEscape(e.primaryMoodCode ?: ""),
                            csvEscape(e.secondaryMoodCode ?: ""),
                            csvEscape(e.promptId ?: ""),
                            csvEscape(e.linkedHabitId ?: ""),
                            csvEscape(e.body),
                            e.createdAtUtcMs.toString(),
                            e.updatedAtUtcMs.toString()
                        ).joinToString(",")
                    )
                    append("\n")
                }
            }

            JournalExportFormat.JSON -> {
                val root = JSONObject()
                root.put("export_kind", "dayseed_readable_journal_export")
                root.put("format_version", 1)
                val arr = JSONArray()
                for (e in sorted) {
                    arr.put(
                        JSONObject().apply {
                            put("journal_entry_id", e.journalEntryId)
                            put("local_date", e.localDate)
                            put("title", e.title ?: JSONObject.NULL)
                            put("body", e.body)
                            put("primary_mood_code", e.primaryMoodCode ?: JSONObject.NULL)
                            put("secondary_mood_code", e.secondaryMoodCode ?: JSONObject.NULL)
                            put("prompt_id", e.promptId ?: JSONObject.NULL)
                            put("linked_habit_id", e.linkedHabitId ?: JSONObject.NULL)
                            put("created_at_utc_ms", e.createdAtUtcMs)
                            put("updated_at_utc_ms", e.updatedAtUtcMs)
                        }
                    )
                }
                root.put("journal_entries", arr)
                root.toString(2)
            }
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Encrypts a portable backup JSON payload with password-based AES-256-GCM.
     */
    fun encryptBackupPayload(plainJson: String, password: String): String {
        require(password.isNotEmpty()) { "Encryption password must not be empty" }
        val random = SecureRandom()
        val salt = ByteArray(BackupCryptoConfig.SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(BackupCryptoConfig.IV_BYTES).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            BackupCryptoConfig.KDF_ITERATIONS,
            BackupCryptoConfig.KEY_LENGTH_BITS
        )
        val secretKey = SecretKeyFactory.getInstance(BackupCryptoConfig.KDF_ALGORITHM)
            .generateSecret(keySpec).encoded
        val aesKey = SecretKeySpec(secretKey, "AES")

        val cipher = Cipher.getInstance(BackupCryptoConfig.CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(BackupCryptoConfig.GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))

        val envelope = JSONObject().apply {
            put("envelope_format", BackupCryptoConfig.ENVELOPE_FORMAT)
            put("encrypted", true)
            put("kdf", BackupCryptoConfig.KDF_ALGORITHM)
            put("kdf_iterations", BackupCryptoConfig.KDF_ITERATIONS)
            put("key_length_bits", BackupCryptoConfig.KEY_LENGTH_BITS)
            put("cipher", BackupCryptoConfig.CIPHER_TRANSFORMATION)
            put("gcm_tag_bits", BackupCryptoConfig.GCM_TAG_BITS)
            put("salt_b64", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv_b64", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext_b64", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
        return envelope.toString(2)
    }

    /**
     * Decrypts an encrypted backup envelope or returns plain JSON if not encrypted.
     *
     * The envelope is fully validated before any PBKDF2/AES-GCM work, so
     * malformed or adversarial inputs are rejected with [BackupRejectedException]
     * before expensive cryptographic processing. Application data is never
     * modified when this exception propagates.
     */
    fun unwrapBackupPayload(rawPayload: String, password: String? = null): String {
        val obj: JSONObject
        try {
            obj = JSONObject(rawPayload)
        } catch (e: Exception) {
            throw BackupRejectedException("Backup payload is not valid JSON", e)
        }

        val encrypted = obj.optBoolean("encrypted", false)
        if (!encrypted) {
            return rawPayload
        }

        // ---- Envelope format validation (before any crypto) ----
        val envelopeFormat = obj.optString("envelope_format", null)
        if (envelopeFormat != BackupCryptoConfig.ENVELOPE_FORMAT) {
            throw BackupRejectedException("Unsupported backup envelope format: $envelopeFormat")
        }

        val iterations = obj.optInt("kdf_iterations", BackupCryptoConfig.KDF_ITERATIONS)
        if (iterations < BackupLimits.MIN_KDF_ITERATIONS || iterations > BackupLimits.MAX_KDF_ITERATIONS) {
            throw BackupRejectedException("KDF iterations $iterations outside accepted range " +
                "[${BackupLimits.MIN_KDF_ITERATIONS}, ${BackupLimits.MAX_KDF_ITERATIONS}]")
        }

        val keyLengthBits = obj.optInt("key_length_bits", BackupCryptoConfig.KEY_LENGTH_BITS)
        if (keyLengthBits !in BackupLimits.ACCEPTED_KEY_LENGTH_BITS) {
            throw BackupRejectedException("Key length $keyLengthBits bits not accepted: " +
                BackupLimits.ACCEPTED_KEY_LENGTH_BITS)
        }

        val tagBits = obj.optInt("gcm_tag_bits", BackupCryptoConfig.GCM_TAG_BITS)
        if (tagBits !in BackupLimits.ACCEPTED_GCM_TAG_BITS) {
            throw BackupRejectedException("GCM tag length $tagBits bits not accepted: " +
                BackupLimits.ACCEPTED_GCM_TAG_BITS)
        }

        val pwd = password ?: throw BackupRejectedException("Password required to decrypt backup")

        val saltBytes: ByteArray
        val ivBytes: ByteArray
        try {
            saltBytes = Base64.decode(obj.getString("salt_b64"), Base64.NO_WRAP)
            ivBytes = Base64.decode(obj.getString("iv_b64"), Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw BackupRejectedException("Malformed Base64 in backup envelope", e)
        }
        if (saltBytes.size != BackupLimits.SALT_BYTES_EXACT) {
            throw BackupRejectedException("Salt length ${saltBytes.size} bytes; expected exactly " +
                "${BackupLimits.SALT_BYTES_EXACT} bytes")
        }
        if (ivBytes.size != BackupLimits.IV_BYTES_EXACT) {
            throw BackupRejectedException("IV length ${ivBytes.size} bytes; expected exactly " +
                "${BackupLimits.IV_BYTES_EXACT} bytes")
        }

        // ---- Expensive cryptographic work only after validation passes ----
        val keySpec = PBEKeySpec(pwd.toCharArray(), saltBytes, iterations, keyLengthBits)
        val secretKey = SecretKeyFactory.getInstance(BackupCryptoConfig.KDF_ALGORITHM)
            .generateSecret(keySpec).encoded
        val aesKey = SecretKeySpec(secretKey, "AES")

        val cipher = Cipher.getInstance(BackupCryptoConfig.CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(tagBits, ivBytes))
        val ciphertext = Base64.decode(obj.getString("ciphertext_b64"), Base64.NO_WRAP)
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, StandardCharsets.UTF_8)
    }
}
