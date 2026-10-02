package com.hisabnikash.app.backup.drive

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveRestClientTest {
    @Test
    fun `searches appDataFolder then creates multipart encrypted backup`() = runTest {
        val transport = FakeTransport(
            HttpResponse(200, "{\"files\":[]}".encodeToByteArray()),
            HttpResponse(200, "{\"id\":\"new-id\"}".encodeToByteArray()),
        )
        val client = DriveRestClient(transport)
        val encrypted = byteArrayOf(1, 7, 9, 2)

        client.uploadLatest("token", encrypted)

        assertTrue(transport.requests[0].url.contains("spaces=appDataFolder"))
        assertTrue(transport.requests[0].url.contains("orderBy=modifiedTime"))
        val upload = transport.requests[1]
        assertEquals("POST", upload.method)
        assertTrue(upload.url.contains("uploadType=multipart"))
        assertTrue(upload.body!!.containsSubsequence(encrypted))
        assertTrue(upload.body!!.decodeToString().contains("appDataFolder"))
    }

    @Test
    fun `updates newest existing file and downloads newest media`() = runTest {
        val listing = """{"files":[{"id":"latest","name":"hisab-backup.hisab","modifiedTime":"2026-01-02T00:00:00Z"},{"id":"old","name":"hisab-backup.hisab","modifiedTime":"2026-01-01T00:00:00Z"}]}"""
        val transport = FakeTransport(
            HttpResponse(200, listing.encodeToByteArray()),
            HttpResponse(200, "{}".encodeToByteArray()),
            HttpResponse(200, listing.encodeToByteArray()),
            HttpResponse(200, byteArrayOf(9, 8, 7)),
        )
        val client = DriveRestClient(transport)

        client.uploadLatest("token", byteArrayOf(5))
        val downloaded = client.downloadLatest("token")

        assertEquals("PATCH", transport.requests[1].method)
        assertTrue(transport.requests[1].url.contains("/latest"))
        assertTrue(transport.requests.last().url.endsWith("/latest?alt=media"))
        assertArrayEquals(byteArrayOf(9, 8, 7), downloaded)
    }

    @Test(expected = DriveReauthorizationRequired::class)
    fun `401 requests reauthorization`() = runTest {
        DriveRestClient(FakeTransport(HttpResponse(401, byteArrayOf()))).downloadLatest("expired")
    }

    @Test(expected = DriveRetryableException::class)
    fun `server errors are retryable`() = runTest {
        DriveRestClient(FakeTransport(HttpResponse(503, byteArrayOf()))).downloadLatest("token")
    }
}

private class FakeTransport(vararg responses: HttpResponse) : DriveHttpTransport {
    val requests = mutableListOf<HttpRequest>()
    private val queue = ArrayDeque(responses.toList())
    override suspend fun execute(request: HttpRequest): HttpResponse {
        requests += request.copy(body = request.body?.copyOf())
        return queue.removeFirst()
    }
}

private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean =
    indices.any { start -> start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] } }
