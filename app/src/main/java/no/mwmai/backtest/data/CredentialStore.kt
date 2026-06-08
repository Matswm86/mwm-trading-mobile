package no.mwmai.backtest.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Credentials

/**
 * Encrypted store for the Caddy basic-auth credentials used by POST /api/jobs.
 * Reads are public so they need nothing; only job submission authenticates.
 * File name `mwm_secure_prefs` is excluded from backup/transfer (see res/xml).
 */
class CredentialStore private constructor(context: Context) {

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "mwm_secure_prefs",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var username: String
        get() = prefs.getString(KEY_USER, "").orEmpty()
        set(v) = prefs.edit().putString(KEY_USER, v).apply()

    var password: String
        get() = prefs.getString(KEY_PASS, "").orEmpty()
        set(v) = prefs.edit().putString(KEY_PASS, v).apply()

    val hasCredentials: Boolean
        get() = username.isNotBlank() && password.isNotBlank()

    /** OkHttp Basic header, or null when unset. */
    fun authHeader(): String? =
        if (hasCredentials) Credentials.basic(username, password) else null

    fun save(user: String, pass: String) {
        prefs.edit().putString(KEY_USER, user).putString(KEY_PASS, pass).apply()
    }

    companion object {
        private const val KEY_USER = "basic_user"
        private const val KEY_PASS = "basic_pass"

        @Volatile
        private var instance: CredentialStore? = null

        fun get(context: Context): CredentialStore =
            instance ?: synchronized(this) {
                instance ?: CredentialStore(context.applicationContext).also { instance = it }
            }
    }
}
