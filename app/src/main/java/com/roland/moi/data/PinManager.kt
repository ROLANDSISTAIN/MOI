package com.roland.moi.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/** Code PIN stocké uniquement sur le téléphone (jamais sauvegardé), sous forme chiffrée (hash + sel). */
object PinManager {
    private const val PREFS = "moi_lock"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(c: Context) = prefs(c).contains("hash")

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs(c).edit().putString("salt", salt).putString("hash", hash(pin, salt)).apply()
    }

    fun check(c: Context, pin: String): Boolean {
        val p = prefs(c)
        val salt = p.getString("salt", null) ?: return false
        return hash(pin, salt) == p.getString("hash", null)
    }

    fun biometricEnabled(c: Context) = prefs(c).getBoolean("bio", true)
    fun setBiometric(c: Context, on: Boolean) = prefs(c).edit().putBoolean("bio", on).apply()

    private fun hash(pin: String, salt: String): String {
        var bytes = (salt + pin).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        repeat(10_000) { bytes = md.digest(bytes) }
        return bytes.toHex()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
