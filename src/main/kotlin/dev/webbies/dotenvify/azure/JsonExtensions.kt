package dev.webbies.dotenvify.azure

import com.google.gson.JsonNull
import com.google.gson.JsonObject

/** Null-safe JSON accessors for Gson — handles JsonNull without throwing. */

internal fun JsonObject.str(key: String): String? {
    val el = get(key) ?: return null
    if (el is JsonNull) return null
    return try { el.asString } catch (_: Exception) { null }
}

internal fun JsonObject.int(key: String): Int? {
    val el = get(key) ?: return null
    if (el is JsonNull) return null
    return try { el.asInt } catch (_: Exception) { null }
}

internal fun JsonObject.bool(key: String): Boolean? {
    val el = get(key) ?: return null
    if (el is JsonNull) return null
    return try { el.asBoolean } catch (_: Exception) { null }
}
