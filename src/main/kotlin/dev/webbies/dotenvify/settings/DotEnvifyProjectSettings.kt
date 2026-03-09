package dev.webbies.dotenvify.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * Per-project settings for DotEnvify.
 */
@State(name = "DotEnvifyProjectSettings", storages = [Storage("dotenvify.xml")])
class DotEnvifyProjectSettings : PersistentStateComponent<DotEnvifyProjectSettings.State> {

    data class State(
        var useGlobalDefaults: Boolean = true,
        var exportPrefix: Boolean = false,
        var sort: Boolean = true,
        var ignoreLowercase: Boolean = true,
        var urlOnly: Boolean = false,
        var outputPath: String = ".env",
        var preserveKeys: String = "",
        var azureGroupName: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): DotEnvifyProjectSettings =
            project.getService(DotEnvifyProjectSettings::class.java)
    }
}
