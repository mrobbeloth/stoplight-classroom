package com.stoplight.classroom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * REST API client for Stoplight Classroom.
 * Replace with Retrofit/OkHttp for production use.
 */
class StoplightAPI(private val baseURL: String) {

    var token: String? = null

    suspend fun login(email: String, password: String): AuthResponse {
        val body = """{"email":"$email","password":"$password"}"""
        val json = post("/api/student/auth/login", body)
        // Minimal JSON parsing — use Gson/Moshi in production
        val accessToken = json.extractValue("accessToken")
        val refreshToken = json.extractValue("refreshToken")
        return AuthResponse(accessToken, refreshToken)
    }

    suspend fun joinSession(joinCode: String, displayName: String): JoinResponse {
        val body = """{"joinCode":"$joinCode","displayName":"$displayName"}"""
        val json = post("/api/sessions/join", body)
        return JoinResponse(
            participantId = json.extractValue("participantId").toLong(),
            sessionId = json.extractValue("sessionId").toLong(),
            participantToken = json.extractValue("participantToken"),
            activityMode = json.extractValue("activityMode")
        )
    }

    suspend fun submitStoplight(sessionId: Long, value: String) {
        post("/api/stoplight/$sessionId", """{"value":"$value"}""")
    }

    private suspend fun post(path: String, body: String): String = withContext(Dispatchers.IO) {
        val conn = URL(baseURL + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        conn.doOutput = true
        conn.outputStream.write(body.toByteArray())
        conn.inputStream.bufferedReader().readText()
    }

    private fun String.extractValue(key: String): String {
        val pattern = """"$key"\s*:\s*"?([^",}\]]+)"?""".toRegex()
        return pattern.find(this)?.groupValues?.get(1) ?: ""
    }
}

data class AuthResponse(val accessToken: String, val refreshToken: String)

data class JoinResponse(
    val participantId: Long,
    val sessionId: Long,
    val participantToken: String,
    val activityMode: String
)
