package dev.webbies.dotenvify.settings

import com.intellij.openapi.options.Configurable
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class DotEnvifySettingsConfigurable : Configurable {

    private val exportCheckbox = JCheckBox("Add export prefix")
    private val sortCheckbox = JCheckBox("Sort alphabetically", true)
    private val noLowerCheckbox = JCheckBox("Ignore lowercase keys")
    private val urlOnlyCheckbox = JCheckBox("URL-only values")
    private val outputPathField = JTextField(".env", 20)
    private val azureOrgUrlField = JTextField("", 30).apply {
        toolTipText = "e.g. https://dev.azure.com/myorg/myproject — shared across all projects"
    }

    override fun getDisplayName(): String = "DotEnvify"

    override fun createComponent(): JComponent {
        reset()
        return FormBuilder.createFormBuilder()
            .addSeparator()
            .addComponent(JLabel("Format Options"))
            .addComponent(exportCheckbox)
            .addComponent(sortCheckbox)
            .addComponent(noLowerCheckbox)
            .addComponent(urlOnlyCheckbox)
            .addSeparator()
            .addLabeledComponent("Default output path:", outputPathField)
            .addSeparator()
            .addComponent(JLabel("Azure DevOps"))
            .addLabeledComponent("Organization URL:", azureOrgUrlField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val s = DotEnvifySettings.getInstance().state
        return exportCheckbox.isSelected != s.exportPrefix ||
            sortCheckbox.isSelected != s.sort ||
            noLowerCheckbox.isSelected != s.ignoreLowercase ||
            urlOnlyCheckbox.isSelected != s.urlOnly ||
            outputPathField.text != s.defaultOutputPath ||
            azureOrgUrlField.text != s.azureOrgUrl
    }

    override fun apply() {
        val s = DotEnvifySettings.getInstance().state
        s.exportPrefix = exportCheckbox.isSelected
        s.sort = sortCheckbox.isSelected
        s.ignoreLowercase = noLowerCheckbox.isSelected
        s.urlOnly = urlOnlyCheckbox.isSelected
        s.defaultOutputPath = outputPathField.text
        s.azureOrgUrl = azureOrgUrlField.text.trim()
    }

    override fun reset() {
        val s = DotEnvifySettings.getInstance().state
        exportCheckbox.isSelected = s.exportPrefix
        sortCheckbox.isSelected = s.sort
        noLowerCheckbox.isSelected = s.ignoreLowercase
        urlOnlyCheckbox.isSelected = s.urlOnly
        outputPathField.text = s.defaultOutputPath
        azureOrgUrlField.text = s.azureOrgUrl
    }
}
