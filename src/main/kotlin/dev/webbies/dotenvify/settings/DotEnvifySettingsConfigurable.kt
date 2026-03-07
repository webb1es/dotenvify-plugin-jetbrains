package dev.webbies.dotenvify.settings

import com.intellij.openapi.options.Configurable
import java.awt.Component
import javax.swing.*

class DotEnvifySettingsConfigurable : Configurable {

    private val exportCheckbox = JCheckBox("Add export prefix")
    private val sortCheckbox = JCheckBox("Sort alphabetically", true)
    private val noLowerCheckbox = JCheckBox("Ignore lowercase keys")
    private val urlOnlyCheckbox = JCheckBox("URL-only values")
    private val outputPathField = JTextField(".env", 20)

    override fun getDisplayName(): String = "DotEnvify"

    override fun createComponent(): JComponent {
        reset()
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JLabel("Default format options:"))
            add(Box.createVerticalStrut(8))
            add(exportCheckbox)
            add(sortCheckbox)
            add(noLowerCheckbox)
            add(urlOnlyCheckbox)
            add(Box.createVerticalStrut(12))
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                add(JLabel("Default output path: "))
                add(outputPathField)
            })
        }
    }

    override fun isModified(): Boolean {
        val s = DotEnvifySettings.getInstance().state
        return exportCheckbox.isSelected != s.exportPrefix ||
            sortCheckbox.isSelected != s.sort ||
            noLowerCheckbox.isSelected != s.ignoreLowercase ||
            urlOnlyCheckbox.isSelected != s.urlOnly ||
            outputPathField.text != s.defaultOutputPath
    }

    override fun apply() {
        val s = DotEnvifySettings.getInstance().state
        s.exportPrefix = exportCheckbox.isSelected
        s.sort = sortCheckbox.isSelected
        s.ignoreLowercase = noLowerCheckbox.isSelected
        s.urlOnly = urlOnlyCheckbox.isSelected
        s.defaultOutputPath = outputPathField.text
    }

    override fun reset() {
        val s = DotEnvifySettings.getInstance().state
        exportCheckbox.isSelected = s.exportPrefix
        sortCheckbox.isSelected = s.sort
        noLowerCheckbox.isSelected = s.ignoreLowercase
        urlOnlyCheckbox.isSelected = s.urlOnly
        outputPathField.text = s.defaultOutputPath
    }
}
