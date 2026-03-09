package dev.webbies.dotenvify.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Global (application-level) settings for DotEnvify.
 */
@State(name = "DotEnvifySettings", storages = [Storage("DotEnvifySettings.xml")])
class DotEnvifySettings : PersistentStateComponent<DotEnvifySettings.State> {

    data class State(
        var exportPrefix: Boolean = false,
        var sort: Boolean = true,
        var ignoreLowercase: Boolean = true,
        var urlOnly: Boolean = false,
        var defaultOutputPath: String = ".env",
        var azureOrgUrl: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): DotEnvifySettings =
            ApplicationManager.getApplication().getService(DotEnvifySettings::class.java)
    }
}
