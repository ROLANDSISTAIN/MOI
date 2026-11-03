package com.roland.moi.backup

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.roland.moi.data.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Sauvegarde dans le dossier caché « appDataFolder » du Google Drive de l'utilisateur.
 * Ce dossier est invisible dans Drive : seule l'app MOI peut le lire.
 * On y garde : moi_backup.json (toutes les données) + une copie de chaque photo.
 */
object DriveBackup {
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "moi_backup.json"
    private const val PREFS = "moi_backup"
    const val STALE_MS = 3L * 24 * 60 * 60 * 1000

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
    fun connectedSince(c: Context) = prefs(c).getLong("since", 0L)

    /** true si la sauvegarde est connectée mais n'a pas réussi depuis plus de 3 jours. */
    fun isStale(c: Context): Boolean {
        if (!isConnected(c)) return false
        val ref = maxOf(lastBackup(c), connectedSince(c))
        return ref > 0 && System.currentTimeMillis() - ref > STALE_MS
    }

    private fun setResult(c: Context, error: String?) {
        val e = prefs(c).edit().putString("error", error)
        if (error == null) e.putLong("last", System.currentTimeMillis())
        if (!prefs(c).contains("since")) e.putLong("since", System.currentTimeMillis())
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

    /** Envoie les données et les nouvelles photos. Renvoie null si OK, sinon le message d'erreur. */
    suspend fun upload(c: Context, token: String, json: String): String? = withContext(Dispatchers.IO) {
        try {
            val files = listFiles(token)
            val id = files[FILE_NAME]
            if (id != null) {
                request("PATCH", "https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media",
                    token, "application/json; charset=UTF-8", json.toByteArray())
            } else {
                create(token, FILE_NAME, "application/json; charset=UTF-8", json.toByteArray())
            }
            // Photos : on envoie celles qui ne sont pas encore sur Drive (une photo ne change jamais)
            Photos.dir(c).listFiles()?.filter { it.isFile && it.name !in files }?.forEach { f ->
                create(token, f.name, "image/jpeg", f.readBytes())
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
        val id = listFiles(token)[FILE_NAME] ?: return@withContext null
        String(requestBytes("GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", token, null, null))
    }

    /** Récupère les photos manquantes sur ce téléphone. Renvoie le nombre de photos récupérées. */
    suspend fun restorePhotos(c: Context, token: String): Int = withContext(Dispatchers.IO) {
        var n = 0
        listFiles(token).filterKeys { it.startsWith("p_") }.forEach { (name, id) ->
            val f = Photos.file(c, name)
            if (!f.exists()) {
                runCatching {
                    f.writeBytes(requestBytes("GET", "https://www.googleapis.com/drive/v3/files/$id?alt=media", token, null, null))
                    n++
                }
            }
        }
        n
    }

    // ---------------- Outils réseau ----------------

    /** Tous les fichiers du dossier caché : nom → id (le plus récent si doublon). */
    private fun listFiles(token: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        var pageToken: String? = null
        do {
            val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&pageSize=1000" +
                "&fields=nextPageToken,files(id,name)&orderBy=modifiedTime%20desc" +
                (pageToken?.let { "&pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
            val res = JSONObject(String(requestBytes("GET", url, token, null, null)))
            val files = res.optJSONArray("files") ?: JSONArray()
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                out.putIfAbsent(f.getString("name"), f.getString("id"))
            }
            pageToken = res.optString("nextPageToken").ifBlank { null }
        } while (pageToken != null)
        return out
    }

    private fun create(token: String, name: String, mime: String, data: ByteArray) {
        val boundary = "moi" + System.nanoTime()
        val meta = JSONObject().put("name", name).put("parents", JSONArray().put("appDataFolder"))
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(meta.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: $mime\r\n\r\n".toByteArray())
            write(data)
            write("\r\n--$boundary--".toByteArray())
        }.toByteArray()
        request("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            token, "multipart/related; boundary=$boundary", body)
    }

    private fun request(method: String, url: String, token: String, type: String?, body: ByteArray?): String =
        String(requestBytes(method, url, token, type, body))

    private fun requestBytes(method: String, url: String, token: String, type: String?, body: ByteArray?): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (method == "PATCH") {
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            conn.requestMethod = method
        }
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", type)
            conn.setFixedLengthStreamingMode(body.size)
            conn.outputStream.use { it.write(body) }
        }
        val code = conn.responseCode
        val bytes = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes() } ?: ByteArray(0)
        conn.disconnect()
        if (code !in 200..299) throw RuntimeException("Google Drive ($code) : ${String(bytes).take(200)}")
        return bytes
    }
}
