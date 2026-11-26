package com.example.licensing

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.BuildConfig
import com.example.data.local.PreferencesStore
import com.example.data.model.LicenseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseConfig {
    const val SUPPORT_OWNER_NAME = "aryan"
    const val SUPPORT_WHATSAPP_NUMBER = "+918767079274"
    const val SUPPORT_WHATSAPP_DISPLAY = "+91 8767079274"
    const val OWNER_UPI_ID = "8767079274-1@nyes"
    const val PLAN_PRICE_INR = 499
    const val TRIAL_DAYS = 7
    const val TRIAL_DURATION_MS = 7L * 24L * 60L * 60L * 1000L

    // Shared offline HMAC-SHA256 passphrase synced identically with keygen.html & src/utils/license.ts
    internal const val HMAC_PASSPHRASE = "ARTHFOOD_PRO_LIFETIME_2026_HMAC"
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun expectedAf499KeyForDevice(deviceId: String): String {
        val cleanId = deviceId.trim().uppercase()
        val payload = "$cleanId:FULL_499"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_PASSPHRASE.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val chars = CharArray(12) { i ->
            ALPHABET[digest[i].toInt() and 0x1F]
        }
        val raw = String(chars)
        return "AF499-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
    }

    fun expectedKeyForDevice(deviceId: String): String {
        val cleanId = deviceId.trim().uppercase()
        val payload = "ARTHFOOD|V1|$cleanId"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_PASSPHRASE.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val chars = CharArray(16) { i ->
            ALPHABET[digest[i].toInt() and 0x1F]
        }
        val raw = String(chars)
        return "AF1-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}"
    }

    private val af499TokenRegex = Regex("""AF499[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})""")
    private val af1TokenRegex = Regex("""AF1[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})[\s-]*([A-Z0-9]{4})""")
    private val invisibleCharsRegex = Regex("""[\u200B-\u200D\uFEFF]""")

    /**
     * Sanitizes user input in the activation field. If the user pasted a full WhatsApp
     * activation message, automatically extracts `AF499-XXXX-XXXX-XXXX` (or legacy `AF1-...`).
     * Otherwise strips invisible characters, converts to uppercase, and preserves typing.
     */
    fun sanitizeKeyInput(rawInput: String): String {
        val cleaned = invisibleCharsRegex.replace(rawInput, "").uppercase()
        af499TokenRegex.find(cleaned)?.let { m ->
            return "AF499-${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}"
        }
        af1TokenRegex.find(cleaned)?.let { m ->
            return "AF1-${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}-${m.groupValues[4]}"
        }
        return cleaned.replace(Regex("[^A-Z0-9-]"), "").take(25)
    }

    fun isWellFormedKey(rawInput: String): Boolean {
        val compact = sanitizeKeyInput(rawInput).replace("-", "").uppercase()
        val is499 = (compact.startsWith("AF499") && compact.length == 17) ||
            (!compact.startsWith("AF") && compact.length == 12)
        val isAf1 = (compact.startsWith("AF1") && compact.length == 19) ||
            (!compact.startsWith("AF") && compact.length == 16)
        return is499 || isAf1
    }

    data class VerificationDiagnostic(
        val matchedKey: String?,
        val normalizedKey: String,
        val errorMessage: String
    ) {
        val isValid: Boolean get() = matchedKey != null
    }

    fun verifyKeyWithDiagnostics(deviceId: String, inputKey: String): VerificationDiagnostic {
        val cleanDevice = deviceId.trim().uppercase()
        val sanitized = sanitizeKeyInput(inputKey)
        val normInput = sanitized.replace("-", "").replace(" ", "").uppercase()

        if (normInput.isBlank()) {
            return VerificationDiagnostic(
                matchedKey = null,
                normalizedKey = "(empty)",
                errorMessage = "Empty key input: paste an AF499-XXXX-XXXX-XXXX activation key."
            )
        }

        val is499Shape = (normInput.startsWith("AF499") && normInput.length == 17) ||
            (!normInput.startsWith("AF") && normInput.length == 12)
        val isAf1Shape = (normInput.startsWith("AF1") && normInput.length == 19) ||
            (!normInput.startsWith("AF") && normInput.length == 16)

        if (!is499Shape && !isAf1Shape) {
            return VerificationDiagnostic(
                matchedKey = null,
                normalizedKey = sanitized.ifBlank { normInput },
                errorMessage = "Invalid key length (${normInput.length} alphanumeric chars). Expected AF499-XXXX-XXXX-XXXX (17 chars) or AF1-XXXX-XXXX-XXXX-XXXX (19 chars)."
            )
        }

        val body = when {
            normInput.startsWith("AF499") -> normInput.removePrefix("AF499")
            normInput.startsWith("AF1") -> normInput.removePrefix("AF1")
            else -> normInput
        }
        val invalidChars = body.filter { it !in ALPHABET }.toSet()
        if (invalidChars.isNotEmpty()) {
            return VerificationDiagnostic(
                matchedKey = null,
                normalizedKey = sanitized,
                errorMessage = "Invalid character(s) [${invalidChars.joinToString(", ")}] in key body. Allowed characters: A-Z (excluding I, O) and 2-9."
            )
        }

        val formattedNorm = if (is499Shape) {
            "AF499-${body.substring(0, 4)}-${body.substring(4, 8)}-${body.substring(8, 12)}"
        } else {
            "AF1-${body.substring(0, 4)}-${body.substring(4, 8)}-${body.substring(8, 12)}-${body.substring(12, 16)}"
        }

        val expected499 = expectedAf499KeyForDevice(cleanDevice)
        val norm499 = expected499.replace("-", "").uppercase()
        val norm499WithPrefix = if (normInput.startsWith("AF499")) normInput else "AF499$normInput"
        if (
            MessageDigest.isEqual(norm499.toByteArray(Charsets.UTF_8), normInput.toByteArray(Charsets.UTF_8)) ||
            MessageDigest.isEqual(norm499.toByteArray(Charsets.UTF_8), norm499WithPrefix.toByteArray(Charsets.UTF_8))
        ) {
            return VerificationDiagnostic(
                matchedKey = expected499,
                normalizedKey = expected499,
                errorMessage = "HMAC-SHA256 verification succeeded for Device ID $cleanDevice."
            )
        }

        val expectedLegacy = expectedKeyForDevice(cleanDevice)
        val normLegacy = expectedLegacy.replace("-", "").uppercase()
        val normLegacyWithPrefix = if (normInput.startsWith("AF1")) normInput else "AF1$normInput"
        if (
            MessageDigest.isEqual(normLegacy.toByteArray(Charsets.UTF_8), normInput.toByteArray(Charsets.UTF_8)) ||
            MessageDigest.isEqual(normLegacy.toByteArray(Charsets.UTF_8), normLegacyWithPrefix.toByteArray(Charsets.UTF_8))
        ) {
            return VerificationDiagnostic(
                matchedKey = expectedLegacy,
                normalizedKey = expectedLegacy,
                errorMessage = "HMAC-SHA256 (legacy AF1) verification succeeded for Device ID $cleanDevice."
            )
        }

        return VerificationDiagnostic(
            matchedKey = null,
            normalizedKey = formattedNorm,
            errorMessage = "HMAC-SHA256 signature mismatch for Device ID $cleanDevice. Key format is valid, so either the key was generated for another Device ID or keygen.html used a different SECRET_PASSPHRASE."
        )
    }

    fun resolveVerifiedKey(deviceId: String, inputKey: String): String? =
        verifyKeyWithDiagnostics(deviceId, inputKey).matchedKey

    fun verifyKey(deviceId: String, inputKey: String): Boolean =
        resolveVerifiedKey(deviceId, inputKey) != null

    fun trialIntegrityTag(deviceId: String, trialStartMs: Long, lastSeenMs: Long): String {
        val payload = "TRIAL|$deviceId|$trialStartMs|$lastSeenMs"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_PASSPHRASE.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
    }

    data class DeviceReportInfo(
        val deviceId: String,
        val manufacturer: String,
        val model: String,
        val brand: String,
        val androidRelease: String,
        val sdkInt: Int,
        val appPackage: String,
        val appVersion: String,
        val businessName: String,
        val ordersCount: Int,
        val isLicensed: Boolean,
        val isLockedOut: Boolean,
        val isTrialExpired: Boolean,
        val isClockTampered: Boolean,
        val daysRemaining: Int,
        val trialStartMillis: Long,
        val licenseKey: String,
        val exportedAtMillis: Long
    )

    fun canonicalReportPayload(info: DeviceReportInfo): String =
        "REPORT|V1|${info.deviceId}|${info.isLicensed}|${info.isLockedOut}|${info.isTrialExpired}|" +
            "${info.isClockTampered}|${info.daysRemaining}|${info.trialStartMillis}|" +
            "${info.licenseKey}|${info.ordersCount}|${info.exportedAtMillis}"

    fun signDeviceReportPayload(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_PASSPHRASE.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun verifyDeviceReportSignature(info: DeviceReportInfo, signatureHex: String): Boolean {
        val expected = signDeviceReportPayload(canonicalReportPayload(info))
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            signatureHex.trim().lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    private fun jsonEscape(value: String): String {
        val sb = StringBuilder(value.length + 8)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun buildSignedDeviceReportJson(info: DeviceReportInfo): String {
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val exportedAtIso = isoFmt.format(Date(info.exportedAtMillis))
        val status = when {
            info.isLicensed -> "ACTIVATED"
            info.isLockedOut -> "LOCKED_OUT"
            else -> "TRIAL_ACTIVE"
        }
        val canonical = canonicalReportPayload(info)
        val payloadSha256 = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val signature = signDeviceReportPayload(canonical)

        return """
            |{
            |  "reportType": "ARTHFOOD_DEVICE_LICENSE_REPORT",
            |  "schemaVersion": 1,
            |  "exportedAtIso": ${jsonEscape(exportedAtIso)},
            |  "exportedAtMillis": ${info.exportedAtMillis},
            |  "app": {
            |    "packageName": ${jsonEscape(info.appPackage)},
            |    "versionName": ${jsonEscape(info.appVersion)},
            |    "plan": "FULL_499",
            |    "planPriceInr": $PLAN_PRICE_INR
            |  },
            |  "device": {
            |    "deviceId": ${jsonEscape(info.deviceId)},
            |    "manufacturer": ${jsonEscape(info.manufacturer)},
            |    "model": ${jsonEscape(info.model)},
            |    "brand": ${jsonEscape(info.brand)},
            |    "androidRelease": ${jsonEscape(info.androidRelease)},
            |    "sdkInt": ${info.sdkInt}
            |  },
            |  "license": {
            |    "status": ${jsonEscape(status)},
            |    "isLicensed": ${info.isLicensed},
            |    "isLockedOut": ${info.isLockedOut},
            |    "isTrialExpired": ${info.isTrialExpired},
            |    "isClockTampered": ${info.isClockTampered},
            |    "daysRemaining": ${info.daysRemaining},
            |    "trialStartMillis": ${info.trialStartMillis},
            |    "licenseKey": ${jsonEscape(info.licenseKey)}
            |  },
            |  "dataPortability": {
            |    "businessName": ${jsonEscape(info.businessName)},
            |    "ordersCount": ${info.ordersCount},
            |    "note": "Full ledger backup JSON can always be exported independently even while locked out."
            |  },
            |  "integrity": {
            |    "algorithm": "HMAC-SHA256",
            |    "canonicalPayloadVersion": "REPORT|V1",
            |    "payloadSha256": ${jsonEscape(payloadSha256)},
            |    "signature": ${jsonEscape(signature)}
            |  }
            |}
        """.trimMargin()
    }
}

data class LicenseDiagnosticEntry(
    val timestampMs: Long,
    val normalizedKey: String,
    val outcome: String,
    val errorMessage: String
)

data class LicenseDiagnosticState(
    val lastNormalizedKey: String = "",
    val lastErrorMessage: String = "No activation attempt recorded yet.",
    val lastAttemptAtMillis: Long = 0L,
    val lastSucceeded: Boolean? = null,
    val recentLogs: List<LicenseDiagnosticEntry> = emptyList()
)

class LicenseManager(
    private val context: Context,
    private val prefs: PreferencesStore
) {
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val _diagnosticState = MutableStateFlow(LicenseDiagnosticState())
    val diagnosticState: StateFlow<LicenseDiagnosticState> = _diagnosticState.asStateFlow()

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty().ifBlank { "ARTHFOOD_DEVICE_FALLBACK" }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest("AF_DEV|$androidId".toByteArray(Charsets.UTF_8))
        val chars = CharArray(8) { i -> alphabet[digest[i].toInt() and 0x1F] }
        val raw = String(chars)
        return "AF-${raw.substring(0, 4)}-${raw.substring(4, 8)}"
    }

    suspend fun evaluateTrialOnLaunch() {
        val snap = prefs.getSnapshot()
        if (prefs.readIsLicensed(snap)) {
            // Once permanently activated, never run clock-rollback checks again.
            return
        }
        val deviceId = getDeviceId()
        val now = System.currentTimeMillis()
        val storedStart = prefs.readTrialStart(snap)
        val storedLast = prefs.readLastSeen(snap)
        val storedTag = prefs.readTrialTag(snap)
        val alreadyTampered = prefs.readTrialTamper(snap)

        if (storedStart <= 0L) {
            val tag = LicenseConfig.trialIntegrityTag(deviceId, now, now)
            prefs.saveTrialRecord(now, now, tag, tampered = false)
            return
        }

        val expectedTag = LicenseConfig.trialIntegrityTag(deviceId, storedStart, storedLast)
        val tagValid = storedTag == expectedTag
        // Clock rollback check ONLY during trial (1-hour grace window or clock before trial start)
        val clockRolledBack = (now < storedLast - 3_600_000L) || (now < storedStart)
        val tampered = alreadyTampered || !tagValid || clockRolledBack

        val newLast = maxOf(storedLast, now)
        val newTag = LicenseConfig.trialIntegrityTag(deviceId, storedStart, newLast)
        prefs.saveTrialRecord(storedStart, newLast, newTag, tampered = tampered)
    }

    val licenseState: Flow<LicenseState> = prefs.rawPreferences.map { snap ->
        val deviceId = getDeviceId()
        val isLicensed = prefs.readIsLicensed(snap)
        val licenseKey = prefs.readLicenseKey(snap)
        if (isLicensed) {
            LicenseState(
                deviceId = deviceId,
                isLicensed = true,
                licenseKey = licenseKey,
                trialStartMillis = prefs.readTrialStart(snap),
                daysRemaining = LicenseConfig.TRIAL_DAYS,
                isTrialExpired = false,
                isClockTampered = false
            )
        } else {
            val start = prefs.readTrialStart(snap).takeIf { it > 0L } ?: System.currentTimeMillis()
            val now = System.currentTimeMillis()
            val elapsed = (now - start).coerceAtLeast(0L)
            val remainingMs = (LicenseConfig.TRIAL_DURATION_MS - elapsed).coerceAtLeast(0L)
            val daysLeft = ((remainingMs + 86_399_999L) / 86_400_000L).toInt().coerceIn(0, LicenseConfig.TRIAL_DAYS)
            val expired = elapsed >= LicenseConfig.TRIAL_DURATION_MS
            val tampered = prefs.readTrialTamper(snap)
            LicenseState(
                deviceId = deviceId,
                isLicensed = false,
                licenseKey = "",
                trialStartMillis = start,
                daysRemaining = daysLeft,
                isTrialExpired = expired,
                isClockTampered = tampered
            )
        }
    }

    suspend fun activateWithKey(inputKey: String): Boolean {
        val deviceId = getDeviceId()
        val diag = LicenseConfig.verifyKeyWithDiagnostics(deviceId, inputKey)
        val now = System.currentTimeMillis()
        val entry = LicenseDiagnosticEntry(
            timestampMs = now,
            normalizedKey = diag.normalizedKey,
            outcome = if (diag.isValid) "VERIFIED" else "REJECTED",
            errorMessage = diag.errorMessage
        )
        val prev = _diagnosticState.value
        _diagnosticState.value = LicenseDiagnosticState(
            lastNormalizedKey = diag.normalizedKey,
            lastErrorMessage = diag.errorMessage,
            lastAttemptAtMillis = now,
            lastSucceeded = diag.isValid,
            recentLogs = (listOf(entry) + prev.recentLogs).take(5)
        )
        val verifiedKey = diag.matchedKey ?: return false
        prefs.saveLicenseUnlocked(verifiedKey)
        return true
    }

    fun buildDeviceReportJson(
        state: LicenseState,
        businessName: String,
        ordersCount: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val info = LicenseConfig.DeviceReportInfo(
            deviceId = state.deviceId.ifBlank { getDeviceId() },
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            appPackage = context.packageName,
            appVersion = BuildConfig.VERSION_NAME,
            businessName = businessName,
            ordersCount = ordersCount,
            isLicensed = state.isLicensed,
            isLockedOut = state.isLockedOut,
            isTrialExpired = state.isTrialExpired,
            isClockTampered = state.isClockTampered,
            daysRemaining = state.daysRemaining,
            trialStartMillis = state.trialStartMillis,
            licenseKey = state.licenseKey,
            exportedAtMillis = nowMillis
        )
        return LicenseConfig.buildSignedDeviceReportJson(info)
    }
}
