package com.warungsync.app.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class DevicePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    var deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_NAME, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var createdTokoCount: Int
        get() = prefs.getInt(KEY_CREATED_TOKO_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_CREATED_TOKO_COUNT, value).apply()

    var activeTokoId: String?
        get() = prefs.getString(KEY_ACTIVE_TOKO_ID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_TOKO_ID, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()

    fun incrementCreatedTokoCount() {
        createdTokoCount = createdTokoCount + 1
    }

    companion object {
        private const val PREFS_NAME = "warungsync_device_prefs"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_CREATED_TOKO_COUNT = "key_created_toko_count"
        private const val KEY_ACTIVE_TOKO_ID = "key_active_toko_id"
        private const val KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp"
    }
}
