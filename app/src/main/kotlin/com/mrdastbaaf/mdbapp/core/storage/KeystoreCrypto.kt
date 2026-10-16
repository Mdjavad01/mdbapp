package com.mrdastbaaf.mdbapp.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption backed by a non-exportable key in the Android Keystore.
 *
 * Why not `androidx.security:security-crypto` (EncryptedSharedPreferences)?
 * That library was deprecated by Google and its last release is an alpha. This
 * is the replacement it recommends: generate a key in the hardware-backed
 * Keystore, keep the ciphertext yourself, and never let the key material leave
 * the TEE/StrongBox. The behaviour the brief asks for — an encrypted store that
 * survives process death — is identical.
 *
 * Ciphertext layout: `[12-byte IV][GCM tag + payload]`, Base64-encoded by the
 * caller. GCM already appends the 16-byte tag to the payload.
 *
 * No `Context` is injected: `AndroidKeyStore` is a process-wide JCA provider,
 * and the ciphertext is handed to [SecureTokenStore] to persist.
 */
@Singleton
class KeystoreCrypto @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No user-authentication requirement: the app must be able to read
                // the token on a cold start without a biometric prompt. The key is
                // still non-exportable and bound to this device's Keystore.
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    /** Encrypts [plaintext] and returns Base64 of `IV || ciphertext||tag`. */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val payload = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + payload, Base64.NO_WRAP)
    }

    /**
     * Decrypts a value produced by [encrypt].
     *
     * Returns `null` rather than throwing when the payload cannot be decrypted —
     * this happens legitimately when the Keystore key was invalidated (device
     * credential reset, app data restored from a backup on another device). The
     * caller treats `null` as "no stored token" and sends the user to login,
     * which is the correct recovery.
     */
    fun decrypt(encoded: String): String? = runCatching {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > IV_LENGTH_BYTES) { "ciphertext too short" }

        val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
        val payload = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(payload), Charsets.UTF_8)
    }.getOrNull()

    /** Drops the Keystore key, rendering every previously stored ciphertext unreadable. */
    fun wipeKey() {
        runCatching {
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mdbapp_token_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
