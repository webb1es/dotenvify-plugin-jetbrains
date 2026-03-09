package dev.webbies.dotenvify.azure

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Azure AD OAuth via Device Code Flow.
 * Uses the VS Code public client ID — works across all tenants without app registration.
 * The Microsoft sign-in page shows "Visual Studio" as the app name — this is expected
 * since it's a shared Microsoft first-party client for developer tools.
 * Tokens are stored in JetBrains Password Safe.
 */
object AzureAuthProvider {

    private const val RESOURCE = "499b84ac-1321-427f-aa17-267ca6975798"
    private const val CLIENT_ID = "872cd9fa-d31f-45e0-9eab-6e460a02d1f1"
    private const val TENANT = "common"
    private const val TOKEN_URL = "https://login.microsoftonline.com/$TENANT/oauth2/token"
    private const val DEVICE_CODE_URL = "https://login.microsoftonline.com/$TENANT/oauth2/devicecode"
    private const val CREDENTIAL_KEY = "DotEnvify-AzureDevOps"

    private val gson = Gson()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun startDeviceCodeFlow(): DeviceCodeResponse {
        val json = post(DEVICE_CODE_URL, "client_id=$CLIENT_ID&resource=$RESOURCE")

        return DeviceCodeResponse(
            deviceCode = json.str("device_code") ?: error("Missing device_code"),
            userCode = json.str("user_code") ?: error("Missing user_code"),
            verificationUri = json.str("verification_uri") ?: json.str("verification_url") ?: "https://microsoft.com/devicelogin",
            expiresIn = json.int("expires_in") ?: 900,
            interval = json.int("interval") ?: 5,
            message = json.str("message") ?: "Enter the code at the verification URL",
        )
    }

    /** Returns null if still pending, throws on error/expiry. */
    fun pollForToken(deviceCode: String): TokenResponse? {
        val body = "grant_type=urn:ietf:params:oauth:grant-type:device_code&client_id=$CLIENT_ID&code=$deviceCode"
        val response = httpClient.send(
            formPost(TOKEN_URL, body),
            HttpResponse.BodyHandlers.ofString(),
        )
        val json = gson.fromJson(response.body(), JsonObject::class.java)

        json.str("error")?.let { error ->
            if (error == "authorization_pending" || error == "slow_down") return null
            throw RuntimeException(json.str("error_description") ?: error)
        }

        val token = StoredToken(
            accessToken = json.str("access_token") ?: error("Missing access_token"),
            refreshToken = json.str("refresh_token") ?: "",
            expiresAt = Instant.now().plusSeconds((json.int("expires_in") ?: 3600).toLong()).epochSecond,
        )
        saveToken(token)

        return TokenResponse(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresIn = json.int("expires_in") ?: 3600,
            tokenType = json.str("token_type") ?: "Bearer",
        )
    }

    /** Returns a valid access token, refreshing if needed. */
    fun getAccessToken(): String? {
        val stored = loadToken() ?: return null
        if (Instant.now().epochSecond < stored.expiresAt - 300) return stored.accessToken
        if (stored.refreshToken.isNotEmpty()) return refreshToken(stored.refreshToken)
        return null
    }

    fun isAuthenticated(): Boolean = loadToken() != null

    fun signOut() {
        PasswordSafe.instance.set(credentialAttributes(), null)
    }

    private fun refreshToken(refreshToken: String): String? {
        val body = "grant_type=refresh_token&client_id=$CLIENT_ID&refresh_token=$refreshToken&resource=$RESOURCE"
        val response = try {
            httpClient.send(formPost(TOKEN_URL, body), HttpResponse.BodyHandlers.ofString())
        } catch (_: Exception) {
            return null
        }
        if (response.statusCode() != 200) return null

        val json = gson.fromJson(response.body(), JsonObject::class.java)
        if (json.str("error") != null) return null

        val accessToken = json.str("access_token") ?: return null
        saveToken(StoredToken(
            accessToken = accessToken,
            refreshToken = json.str("refresh_token") ?: refreshToken,
            expiresAt = Instant.now().plusSeconds((json.int("expires_in") ?: 3600).toLong()).epochSecond,
        ))
        return accessToken
    }

    private fun post(url: String, body: String): JsonObject {
        val response = httpClient.send(formPost(url, body), HttpResponse.BodyHandlers.ofString())
        val json = gson.fromJson(response.body(), JsonObject::class.java)
        if (response.statusCode() != 200) {
            throw RuntimeException("Request failed: ${json.str("error_description") ?: json.str("error") ?: response.body()}")
        }
        return json
    }

    private fun formPost(url: String, body: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

    // --- Token storage ---

    private data class StoredToken(val accessToken: String, val refreshToken: String, val expiresAt: Long)

    private fun saveToken(token: StoredToken) {
        PasswordSafe.instance.set(credentialAttributes(), Credentials("azure", gson.toJson(token)))
    }

    private fun loadToken(): StoredToken? {
        val json = PasswordSafe.instance.get(credentialAttributes())?.getPasswordAsString() ?: return null
        return try { gson.fromJson(json, StoredToken::class.java) } catch (_: Exception) { null }
    }

    private fun credentialAttributes() = CredentialAttributes(
        generateServiceName("DotEnvify", CREDENTIAL_KEY)
    )
}
