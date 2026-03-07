package dev.webbies.dotenvify.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.awt.Component
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
    private val projectControls = listOf<JComponent>(
        exportCheckbox, sortCheckbox, noLowerCheckbox, urlOnlyCheckbox, outputPathField, preserveKeysField,
    )

    override fun getDisplayName(): String = "DotEnvify (Project)"

    override fun createComponent(): JComponent {
        useGlobalCheckbox.addItemListener { e ->
            projectControls.forEach { it.isEnabled = e.stateChange == ItemEvent.DESELECTED }
        }
        reset()

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(useGlobalCheckbox)
            add(Box.createVerticalStrut(12))
            add(JLabel("Project format options:"))
            add(Box.createVerticalStrut(8))
            add(exportCheckbox)
            add(sortCheckbox)
            add(noLowerCheckbox)
            add(urlOnlyCheckbox)
            add(Box.createVerticalStrut(12))
            add(labeledRow("Output path: ", outputPathField))
            add(Box.createVerticalStrut(8))
            add(labeledRow("Preserve keys (comma-separated): ", preserveKeysField))
        }
    }

    override fun isModified(): Boolean {
        val s = DotEnvifyProjectSettings.getInstance(project).state
        return useGlobalCheckbox.isSelected != s.useGlobalDefaults ||
            exportCheckbox.isSelected != s.exportPrefix ||
            sortCheckbox.isSelected != s.sort ||
            noLowerCheckbox.isSelected != s.ignoreLowercase ||
            urlOnlyCheckbox.isSelected != s.urlOnly ||
            outputPathField.text != s.outputPath ||
            preserveKeysField.text != s.preserveKeys
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
        projectControls.forEach { it.isEnabled = !s.useGlobalDefaults }
    }

    private fun labeledRow(label: String, field: JComponent) = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        add(JLabel(label))
        add(field)
    }
}
