package com.hisabnikash.app.backup.drive

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
)

data class HttpResponse(val status: Int, val body: ByteArray)

interface DriveHttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse
}

class UrlConnectionDriveTransport : DriveHttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        try {
            val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = request.method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            request.headers.forEach(connection::setRequestProperty)
            request.body?.let { body ->
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) throw DriveRetryableException(status)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: byteArrayOf()
            if (body.size > MAX_RESPONSE_BYTES) throw DriveRetryableException(status)
            connection.disconnect()
            return HttpResponse(status, body)
        } catch (error: DriveException) {
            throw error
        } catch (error: IOException) {
            throw DriveOfflineException(error)
        }
    }

    companion object { private const val MAX_RESPONSE_BYTES = 50 * 1024 * 1024 }
}

open class DriveException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DriveReauthorizationRequired : DriveException("Drive authorization required")
class DriveRetryableException(val status: Int, cause: Throwable? = null) : DriveException("Drive temporarily unavailable", cause)
class DriveOfflineException(cause: Throwable? = null) : DriveException("Drive offline", cause)
class DriveQuotaException : DriveException("Drive quota exceeded")

interface DriveRemoteStore {
    suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray)
    suspend fun downloadLatest(accessToken: String): ByteArray?
}

class DriveRestClient(
    private val transport: DriveHttpTransport = UrlConnectionDriveTransport(),
) : DriveRemoteStore {
    override suspend fun uploadLatest(accessToken: String, encryptedEnvelope: ByteArray) {
        val existing = listBackups(accessToken).firstOrNull()
        val boundary = "hisab-${java.util.UUID.randomUUID()}"
        val metadata = if (existing == null) {
            """{"name":"$BACKUP_NAME","parents":["appDataFolder"]}"""
        } else {
            """{"name":"$BACKUP_NAME"}"""
        }
        val body = multipart(boundary, metadata, encryptedEnvelope)
        val url = if (existing == null) {
            "$UPLOAD_BASE/files?uploadType=multipart&fields=id"
        } else {
            "$UPLOAD_BASE/files/${encode(existing.id)}?uploadType=multipart&fields=id"
        }
        val response = execute(
            HttpRequest(
                method = if (existing == null) "POST" else "PATCH",
                url = url,
                headers = auth(accessToken) + ("Content-Type" to "multipart/related; boundary=$boundary"),
                body = body,
            ),
        )
        checkStatus(response)
        body.fill(0)
    }

    override suspend fun downloadLatest(accessToken: String): ByteArray? {
        val file = listBackups(accessToken).firstOrNull() ?: return null
        val response = execute(
            HttpRequest("GET", "$API_BASE/files/${encode(file.id)}?alt=media", auth(accessToken)),
        )
        checkStatus(response)
        return response.body
    }

    suspend fun revoke(accessToken: String) {
        val response = execute(HttpRequest("POST", "https://oauth2.googleapis.com/revoke?token=${encode(accessToken)}"))
        if (response.status !in 200..299 && response.status != 400) checkStatus(response)
    }

    private suspend fun listBackups(accessToken: String): List<DriveFile> {
        val query = "name = '$BACKUP_NAME' and trashed = false"
        val fields = "files(id,name,modifiedTime)"
        val url = "$API_BASE/files?spaces=appDataFolder&q=${encode(query)}&orderBy=modifiedTime%20desc&fields=${encode(fields)}&pageSize=10"
        val response = execute(HttpRequest("GET", url, auth(accessToken)))
        checkStatus(response)
        val root = Json.parseToJsonElement(response.body.decodeToString()).jsonObject
        return root["files"]?.jsonArray?.map { element ->
            val item = element.jsonObject
            DriveFile(item.getValue("id").jsonPrimitive.content)
        }.orEmpty()
    }

    private suspend fun execute(request: HttpRequest): HttpResponse = try {
        transport.execute(request)
    } catch (error: DriveException) {
        throw error
    } catch (error: IOException) {
        throw DriveOfflineException(error)
    }

    private fun checkStatus(response: HttpResponse) {
        when {
            response.status in 200..299 -> Unit
            response.status == 401 -> throw DriveReauthorizationRequired()
            response.status == 403 || response.status == 429 -> throw DriveQuotaException()
            response.status >= 500 -> throw DriveRetryableException(response.status)
            else -> throw DriveException("Drive request failed with ${response.status}")
        }
    }

    private fun multipart(boundary: String, metadata: String, encrypted: ByteArray): ByteArray {
        val output = java.io.ByteArrayOutputStream(metadata.length + encrypted.size + 256)
        output.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n".encodeToByteArray())
        output.write("--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray())
        output.write(encrypted)
        output.write("\r\n--$boundary--\r\n".encodeToByteArray())
        return output.toByteArray()
    }

    private fun auth(token: String) = mapOf("Authorization" to "Bearer $token")
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    private data class DriveFile(val id: String)

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val BACKUP_NAME = "hisab-backup.hisab"
        private const val API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    }
}
