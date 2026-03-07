package dev.webbies.dotenvify.azure

data class VariableGroup(
    val id: Int,
    val name: String,
    val variables: Map<String, Variable>,
    val description: String = "",
)

data class Variable(
    val value: String,
    val isSecret: Boolean = false,
)

data class DeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
    val message: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val tokenType: String,
)

data class AzureConnection(
    val organizationUrl: String,
    val project: String,
) {
    val organization: String
        get() = parseOrg(organizationUrl)

    companion object {
        private val DEV_AZURE_REGEX = Regex("""https?://dev\.azure\.com/([^/]+)/([^/]+)""")
        private val VSTUDIO_REGEX = Regex("""https?://([^.]+)\.visualstudio\.com/([^/]+)""")

        fun parseUrl(url: String): Pair<String, String?> {
            DEV_AZURE_REGEX.find(url)?.let {
                return it.groupValues[1] to it.groupValues[2].ifEmpty { null }
            }
            VSTUDIO_REGEX.find(url)?.let {
                return it.groupValues[1] to it.groupValues[2].ifEmpty { null }
            }
            throw IllegalArgumentException(
                "Invalid Azure DevOps URL. Expected: https://dev.azure.com/{org}/{project} " +
                        "or https://{org}.visualstudio.com/{project}"
            )
        }

        private fun parseOrg(url: String): String = parseUrl(url).first
    }
}
