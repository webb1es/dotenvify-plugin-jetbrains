package dev.webbies.dotenvify.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.webbies.dotenvify.azure.AzureVariableGroupPanel
import dev.webbies.dotenvify.diagnostics.DiagnosticsPanel

class DotEnvifyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // Azure first — it's the primary feature
        val azureTab = contentFactory.createContent(AzureVariableGroupPanel(project), "Azure DevOps", false).apply {
            icon = AllIcons.Providers.Azure
        }
        val convertTab = contentFactory.createContent(DotEnvifyToolWindowPanel(project), "Paste & Format", false).apply {
            icon = AllIcons.Actions.RealIntentionBulb
        }
        val diagnosticsTab = contentFactory.createContent(DiagnosticsPanel(project), "Diagnostics", false).apply {
            icon = AllIcons.Actions.Find
        }

        toolWindow.contentManager.addContent(azureTab)
        toolWindow.contentManager.addContent(convertTab)
        toolWindow.contentManager.addContent(diagnosticsTab)
    }
}
