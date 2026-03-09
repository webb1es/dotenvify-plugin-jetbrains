package dev.webbies.dotenvify.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.FormBuilder
import java.awt.event.ItemEvent
import javax.swing.*

class DotEnvifyProjectSettingsConfigurable(private val project: Project) : Configurable {

    private val useGlobalCheckbox = JCheckBox("Use global defaults", true)
    private val exportCheckbox = JCheckBox("Add export prefix")
    private val sortCheckbox = JCheckBox("Sort alphabetically", true)
    private val noLowerCheckbox = JCheckBox("Ignore lowercase keys")
    private val urlOnlyCheckbox = JCheckBox("URL-only values")
    private val outputPathField = JTextField(".env", 20)
    private val preserveKeysField = JTextField("", 30)
    private val azureGroupNameField = JTextField("", 25).apply {
        toolTipText = "Azure DevOps variable group name for this project"
    }
    private val formatControls = listOf<JComponent>(
        exportCheckbox, sortCheckbox, noLowerCheckbox, urlOnlyCheckbox, outputPathField, preserveKeysField,
    )

    override fun getDisplayName(): String = "DotEnvify (Project)"

    override fun createComponent(): JComponent {
        useGlobalCheckbox.addItemListener { e ->
            formatControls.forEach { it.isEnabled = e.stateChange == ItemEvent.DESELECTED }
        }
        reset()

        return FormBuilder.createFormBuilder()
            .addComponent(useGlobalCheckbox)
            .addSeparator()
            .addComponent(JLabel("Format Options"))
            .addComponent(exportCheckbox)
            .addComponent(sortCheckbox)
            .addComponent(noLowerCheckbox)
            .addComponent(urlOnlyCheckbox)
            .addSeparator()
            .addLabeledComponent("Output path:", outputPathField)
            .addLabeledComponent("Preserve keys (comma-separated):", preserveKeysField)
            .addSeparator()
            .addComponent(JLabel("Azure DevOps"))
            .addLabeledComponent("Variable group:", azureGroupNameField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val s = DotEnvifyProjectSettings.getInstance(project).state
        return useGlobalCheckbox.isSelected != s.useGlobalDefaults ||
            exportCheckbox.isSelected != s.exportPrefix ||
            sortCheckbox.isSelected != s.sort ||
            noLowerCheckbox.isSelected != s.ignoreLowercase ||
            urlOnlyCheckbox.isSelected != s.urlOnly ||
            outputPathField.text != s.outputPath ||
            preserveKeysField.text != s.preserveKeys ||
            azureGroupNameField.text != s.azureGroupName
    }

    override fun apply() {
        val s = DotEnvifyProjectSettings.getInstance(project).state
        s.useGlobalDefaults = useGlobalCheckbox.isSelected
        s.exportPrefix = exportCheckbox.isSelected
        s.sort = sortCheckbox.isSelected
        s.ignoreLowercase = noLowerCheckbox.isSelected
        s.urlOnly = urlOnlyCheckbox.isSelected
        s.outputPath = outputPathField.text
        s.preserveKeys = preserveKeysField.text
        s.azureGroupName = azureGroupNameField.text.trim()
    }

    override fun reset() {
        val s = DotEnvifyProjectSettings.getInstance(project).state
        useGlobalCheckbox.isSelected = s.useGlobalDefaults
        exportCheckbox.isSelected = s.exportPrefix
        sortCheckbox.isSelected = s.sort
        noLowerCheckbox.isSelected = s.ignoreLowercase
        urlOnlyCheckbox.isSelected = s.urlOnly
        outputPathField.text = s.outputPath
        preserveKeysField.text = s.preserveKeys
        azureGroupNameField.text = s.azureGroupName
        formatControls.forEach { it.isEnabled = !s.useGlobalDefaults }
    }
}
