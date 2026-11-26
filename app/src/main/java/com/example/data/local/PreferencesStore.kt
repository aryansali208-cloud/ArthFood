package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arthfood_prefs")

class PreferencesStore(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val INVOICE_LANGUAGE = stringPreferencesKey("invoice_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val LOCAL_BUSINESS_NAME = stringPreferencesKey("local_business_name")
        val TAGLINE = stringPreferencesKey("tagline")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val PHONE = stringPreferencesKey("phone")
        val WHATSAPP_SAME = booleanPreferencesKey("whatsapp_same")
        val WHATSAPP_NUMBER = stringPreferencesKey("whatsapp_number")
        val ADDRESS = stringPreferencesKey("address")
        val CITY = stringPreferencesKey("city")
        val STATE = stringPreferencesKey("state")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val UPI_ID = stringPreferencesKey("upi_id")
        val TAX_PERCENT = doublePreferencesKey("tax_percent")
        val TAX_LINE = stringPreferencesKey("tax_line")
        val THANK_YOU_NOTE = stringPreferencesKey("thank_you_note")
        val QR_CONTACT_NAME = stringPreferencesKey("qr_contact_name")
        val LOGO_PATH = stringPreferencesKey("logo_path")

        val LOCKED_RATES = booleanPreferencesKey("locked_rates")
        val STOCK_TRACKING = booleanPreferencesKey("stock_tracking")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val AUTO_BACKUP_URI = stringPreferencesKey("auto_backup_uri")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val INVOICE_SEQ = intPreferencesKey("invoice_seq")

        val TRIAL_START_MS = longPreferencesKey("trial_start_ms")
        val LAST_SEEN_MS = longPreferencesKey("last_seen_ms")
        val TRIAL_TAG = stringPreferencesKey("trial_tag")
        val TRIAL_TAMPER = booleanPreferencesKey("trial_tamper")
        val IS_LICENSED = booleanPreferencesKey("is_licensed")
        val LICENSE_KEY = stringPreferencesKey("license_key")
    }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map {
        AppLanguage.fromCode(it[Keys.APP_LANGUAGE])
    }

    val invoiceLanguage: Flow<AppLanguage> = context.dataStore.data.map {
        AppLanguage.fromCode(it[Keys.INVOICE_LANGUAGE])
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val businessProfile: Flow<BusinessProfile> = context.dataStore.data.map { prefs ->
        BusinessProfile(
            businessName = prefs[Keys.BUSINESS_NAME] ?: "ArthFood Kitchen",
            localBusinessName = prefs[Keys.LOCAL_BUSINESS_NAME] ?: "",
            tagline = prefs[Keys.TAGLINE] ?: "Made billing easy",
            ownerName = prefs[Keys.OWNER_NAME] ?: "",
            phone = prefs[Keys.PHONE] ?: "",
            whatsappSameAsPhone = prefs[Keys.WHATSAPP_SAME] ?: true,
            whatsappNumber = prefs[Keys.WHATSAPP_NUMBER] ?: "",
            address = prefs[Keys.ADDRESS] ?: "",
            city = prefs[Keys.CITY] ?: "",
            state = prefs[Keys.STATE] ?: "",
            pinCode = prefs[Keys.PIN_CODE] ?: "",
            upiId = prefs[Keys.UPI_ID] ?: "",
            taxPercent = prefs[Keys.TAX_PERCENT] ?: 0.0,
            taxLine = prefs[Keys.TAX_LINE] ?: "",
            thankYouNote = prefs[Keys.THANK_YOU_NOTE] ?: "Thank you for your order! Visit again.",
            qrContactName = prefs[Keys.QR_CONTACT_NAME] ?: "",
            logoPath = prefs[Keys.LOGO_PATH] ?: ""
        )
    }

    val lockedRates: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCKED_RATES] ?: true }
    val stockTracking: Flow<Boolean> = context.dataStore.data.map { it[Keys.STOCK_TRACKING] ?: false }
    val lastBackupAt: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP_AT] ?: 0L }
    val autoBackupFolderUri: Flow<String> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_URI] ?: "" }
    val pinHash: Flow<String> = context.dataStore.data.map { it[Keys.PIN_HASH] ?: "" }

    val rawPreferences: Flow<Preferences> = context.dataStore.data

    suspend fun getSnapshot(): Preferences = context.dataStore.data.first()

    suspend fun setAppLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = lang.code }
    }

    suspend fun setInvoiceLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.INVOICE_LANGUAGE] = lang.code }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUSINESS_NAME] = profile.businessName
            prefs[Keys.LOCAL_BUSINESS_NAME] = profile.localBusinessName
            prefs[Keys.TAGLINE] = profile.tagline
            prefs[Keys.OWNER_NAME] = profile.ownerName
            prefs[Keys.PHONE] = profile.phone
            prefs[Keys.WHATSAPP_SAME] = profile.whatsappSameAsPhone
            prefs[Keys.WHATSAPP_NUMBER] = profile.whatsappNumber
            prefs[Keys.ADDRESS] = profile.address
            prefs[Keys.CITY] = profile.city
            prefs[Keys.STATE] = profile.state
            prefs[Keys.PIN_CODE] = profile.pinCode
            prefs[Keys.UPI_ID] = profile.upiId
            prefs[Keys.TAX_PERCENT] = profile.taxPercent
            prefs[Keys.TAX_LINE] = profile.taxLine
            prefs[Keys.THANK_YOU_NOTE] = profile.thankYouNote
            prefs[Keys.QR_CONTACT_NAME] = profile.qrContactName
            prefs[Keys.LOGO_PATH] = profile.logoPath
        }
    }

    suspend fun setLockedRates(locked: Boolean) {
        context.dataStore.edit { it[Keys.LOCKED_RATES] = locked }
    }

    suspend fun setStockTracking(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STOCK_TRACKING] = enabled }
    }

    suspend fun setLastBackupAt(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = timestamp }
    }

    suspend fun setAutoBackupFolderUri(uri: String) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_URI] = uri }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun nextInvoiceSequence(): Int {
        var next = 1
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.INVOICE_SEQ] ?: 0
            next = current + 1
            prefs[Keys.INVOICE_SEQ] = next
        }
        return next
    }

    suspend fun syncMinInvoiceSequence(minSeq: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.INVOICE_SEQ] ?: 0
            if (minSeq > current) {
                prefs[Keys.INVOICE_SEQ] = minSeq
            }
        }
    }

    suspend fun saveTrialRecord(
        trialStartMs: Long,
        lastSeenMs: Long,
        integrityTag: String,
        tampered: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TRIAL_START_MS] = trialStartMs
            prefs[Keys.LAST_SEEN_MS] = lastSeenMs
            prefs[Keys.TRIAL_TAG] = integrityTag
            prefs[Keys.TRIAL_TAMPER] = tampered
        }
    }

    suspend fun saveLicenseUnlocked(licenseKey: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LICENSED] = true
            prefs[Keys.LICENSE_KEY] = licenseKey
            prefs[Keys.TRIAL_TAMPER] = false
        }
    }

    fun readTrialStart(prefs: Preferences): Long = prefs[Keys.TRIAL_START_MS] ?: 0L
    fun readLastSeen(prefs: Preferences): Long = prefs[Keys.LAST_SEEN_MS] ?: 0L
    fun readTrialTag(prefs: Preferences): String = prefs[Keys.TRIAL_TAG] ?: ""
    fun readTrialTamper(prefs: Preferences): Boolean = prefs[Keys.TRIAL_TAMPER] ?: false
    fun readIsLicensed(prefs: Preferences): Boolean = prefs[Keys.IS_LICENSED] ?: false
    fun readLicenseKey(prefs: Preferences): String = prefs[Keys.LICENSE_KEY] ?: ""
}
