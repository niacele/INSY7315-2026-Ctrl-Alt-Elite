package com.example.employeedigitalhandbook.repositories

import com.example.employeedigitalhandbook.admin.AuthResult
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.api.ApiLoginRequest
import com.example.employeedigitalhandbook.sessions.SessionManager
import com.example.employeedigitalhandbook.sessions.UserSession

class AuthRepository(private val sessionManager: SessionManager) {

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = ApiClient.apiService.login(ApiLoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // Unpack nested 'data' payload if available, else fallback to top-level fields
                val userData = body.data

                val rawUserId = userData?.userId?.takeIf { it > 0 }
                    ?: body.userId?.takeIf { it > 0 }

                val rawEmail = userData?.email?.takeIf { it.isNotBlank() }
                    ?: body.email?.takeIf { it.isNotBlank() }
                    ?: email.trim()

                val rawName = userData?.fullName?.takeIf { it.isNotBlank() }
                    ?: body.name?.takeIf { it.isNotBlank() }

                val derivedName = email.substringBefore("@")
                    .split(".", "_", "-")
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }

                val resolvedName = rawName ?: derivedName

                // Verify valid User ID was extracted
                val resolvedId = if (rawUserId != null && rawUserId > 0) {
                    rawUserId.toString()
                } else {
                    return AuthResult.Error("Failed to retrieve valid User ID from server.")
                }

                val userSession = UserSession(
                    userId = resolvedId,
                    email = rawEmail,
                    fullName = resolvedName
                )

                // Save session with true ID (3)
                sessionManager.saveSession(userSession)

                AuthResult.Success(userSession)
            } else {
                AuthResult.Error("Login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            AuthResult.Error("Could not connect to backend server: ${e.localizedMessage}")
        }
    }
}