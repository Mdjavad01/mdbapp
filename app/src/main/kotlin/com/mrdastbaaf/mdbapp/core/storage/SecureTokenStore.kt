package com.mrdastbaaf.mdbapp.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place the auth token is written to disk.
 *
 * The token is opaque, valid for 30 days, and has no refresh path — on a 401 the
 * app wipes it and sends the user to login. It therefore never leaves this class
 * except through [readToken], which the network interceptor calls synchronously
 * on the OkHttp thread.
 *
 * Storage is `SharedPreferences` holding AES-GCM ciphertext produced by
 * [KeystoreCrypto]; the key lives in the Android Keystore and is not exportable.
 * The file is excluded from cloud backup and device transfer (see
 * `res/xml/data_extraction_rules.xml`).
 *
 * NOTHING else belongs in this file. Caches, preferences and the last playback
 * position go in [LocalStore] — they are not secrets and encrypting them would
 * only cost CPU.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val crypto: KeystoreCrypto,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the stored token, or `null` when there is none or it can no longer
     * be decrypted. A `null` here means "not signed in" and is not an error.
     */
    @Synchronized
    fun readToken(): String? {
        val ciphertext = prefs.getString(KEY_TOKEN, null) ?: return null
        val plaintext = crypto.decrypt(ciphertext)
        if (plaintext == null) {
            // The Keystore key was invalidated (device credential reset, restore
            // onto another device). Drop the now-useless blob so we stop retrying.
            clear()
        }
        return plaintext
    }

    @Synchronized
    fun writeToken(token: String) {
        prefs.edit(commit = true) { putString(KEY_TOKEN, crypto.encrypt(token)) }
    }

    /**
     * Removes the token and destroys the Keystore key.
     *
     * Wiping the key (rather than just the ciphertext) guarantees that any copy
     * of the blob — a filesystem snapshot, a leftover backup — is undecryptable
     * from this point on.
     */
    @Synchronized
    fun clear() {
        prefs.edit(commit = true) { clear() }
        crypto.wipeKey()
    }

    fun hasToken(): Boolean = readToken() != null

    private companion object {
        const val PREFS_NAME = "mdbapp_secure_store"
        const val KEY_TOKEN = "auth_token"
    }
}
