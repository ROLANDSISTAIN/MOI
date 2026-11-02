package com.roland.moi.backup

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Sauvegarde dans le dossier caché « appDataFolder » du Google Drive de l'utilisateur.
 * Ce dossier est invisible dans Drive : seule l'app MOI peut le lire.
 */
object DriveBackup {
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "moi_backup.json"
    private const val PREFS = "moi_backup"

    sealed class Auth {
        data class Token(val token: String) : Auth()
        data class NeedsConsent(val intent: PendingIntent) : Auth()
        data class Error(val message: String) : Auth()
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun isConnected(c: Context) = prefs(c).getBoolean("connected", false)
    fun setConnected(c: Context, on: Boolean) = prefs(c).edit().putBoolean("connected", on).apply()
    fun lastBackup(c: Context) = prefs(c).getLong("last", 0L)
    fun lastError(c: Context): String? = prefs(c).getString("error", null)
    private fun setResult(c: Context, error: String?) {
        val e = prefs(c).edit().putString("error", error)
        if (error == null) e.putLong("last", System.currentTimeMillis())
        e.apply()
    }

    suspend fun authorize(c: Context): Auth = try {
        val req = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
            .build()
        val res: AuthorizationResult = Identity.getAuthorizationClient(c).authorize(req).await()
        handle(res)
    } catch (t: Throwable) {
        Auth.Error(t.message ?: t.javaClass.simpleName)
    }

    fun handle(res: AuthorizationResult): Auth {
        val pi = res.pendingIntent
        if (res.hasResolution() && pi != null) return Auth.NeedsConsent(pi)
        val tok = res.accessToken ?: return Auth.Error("Pas de jeton Google")
        return Auth.Token(tok)
    }

    /** Envoie les données. Renvoie null si OK, sinon le message d'erreur. */
    suspend fun upload(c: Context, token: String, json: String): String? = withContext(Dispatchers.IO) {
        try {
            val id = findFileId(token)
            if (id != null) {
                request("PATCH", "https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media",
                    token, "application/json; charset=UTF-8", json.toByteArray())
            } else {
                val boundary = "moi" + System.currentTimeMillis()
                val meta = JSONObject().put("name", FILE_NAME).put("parents", org.json.JSONArray().put("appDataFolder"))
                val body = buildString {
                    append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(meta.toString()).append("\r\n")
                    append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(json).append("\r\n--$boundary--")
                }
                request("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
                    token, "multipart/related; boundary=$boundary", body.toByteArray())
            }
            setResult(c, null)
            null
        } catch (t: Throwable) {
            val msg = t.message ?: "Erreur inconnue"
            setResult(c, msg)
            msg
        }
    }

    /** Télécharge la dernière sauvegarde, ou null s'il n'y en a pas. */
    suspend fun download(token: String): String? = withContext(Dispatchers.IO) {
        val id = findFileId(token) ?: return@withContext null
        request("GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", token, null, null)
    }

    private fun findFileId(token: String): String? {
        val q = URLEncoder.encode("name='$FILE_NAME'", "UTF-8")
        val res = request("GET",
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime)&orderBy=modifiedTime%20desc",
            token, null, null)
        val files = JSONObject(res).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun request(method: String, url: String, token: String, type: String?, body: ByteArray?): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (method == "PATCH") {
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            conn.requestMethod = method
        }
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", type)
            conn.outputStream.use { it.write(body) }
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw RuntimeException("Google Drive ($code) : ${text.take(200)}")
        return text
    }
}
