package dev.webbies.dotenvify.ui

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.core.*
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DotEnvifyToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val inputArea = JBTextArea().apply { font = MONO_FONT; lineWrap = true; emptyText.text = "Paste raw key-value data here..." }
    private val outputArea = JBTextArea().apply { font = MONO_FONT; isEditable = false; lineWrap = true }
    private val exportCheckbox = JCheckBox("export prefix")
    private val sortCheckbox = JCheckBox("Sort A-Z", true)
    private val noLowerCheckbox = JCheckBox("Ignore lowercase")
    private val urlOnlyCheckbox = JCheckBox("URL-only")
    private val statusLabel = JLabel(" ")

    init {
        val optionsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            listOf(exportCheckbox, sortCheckbox, noLowerCheckbox, urlOnlyCheckbox).forEachIndexed { i, cb ->
                if (i > 0) add(Box.createHorizontalStrut(8))
                add(cb)
            }
        }

        val splitter = JBSplitter(true, 0.5f).apply {
            firstComponent = titledPanel("Input", JBScrollPane(inputArea))
            secondComponent = titledPanel("Output Preview", JBScrollPane(outputArea))
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(JButton("Apply to .env").apply { addActionListener { applyToFile() } })
            add(Box.createHorizontalStrut(8))
            add(JButton("Copy to Clipboard").apply { addActionListener { copyToClipboard() } })
            add(Box.createHorizontalStrut(8))
            add(JButton("Clear").apply { addActionListener { inputArea.text = ""; outputArea.text = ""; statusLabel.text = " " } })
            add(Box.createHorizontalGlue())
            add(statusLabel)
        }

        add(optionsPanel, BorderLayout.NORTH)
        add(splitter, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        inputArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = updatePreview()
            override fun removeUpdate(e: DocumentEvent) = updatePreview()
            override fun changedUpdate(e: DocumentEvent) = updatePreview()
        })
        val optionListener = { _: java.awt.event.ItemEvent -> updatePreview() }
        listOf(exportCheckbox, sortCheckbox, noLowerCheckbox, urlOnlyCheckbox).forEach { it.addItemListener(optionListener) }
    }

    private fun updatePreview() {
        val input = inputArea.text
        if (input.isBlank()) { outputArea.text = ""; statusLabel.text = " "; return }

        val result = DotEnvParser.parse(input)
        if (result.entries.isEmpty()) { outputArea.text = ""; statusLabel.text = "No entries found"; return }

        outputArea.text = DotEnvFormatter.format(result.entries, currentOptions())
        val warnings = if (result.warnings.isNotEmpty()) " | ${result.warnings.size} warning(s)" else ""
        statusLabel.text = "${result.entries.size} entries$warnings"
    }

    private fun applyToFile() {
        val output = outputArea.text
        if (output.isBlank()) {
            Messages.showWarningDialog(project, "Nothing to save. Paste input first.", "DotEnvify")
            return
        }

        val basePath = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(FileSaverDescriptor("Save .env File", "Choose where to save the .env file"), project)
            .save(basePath, ".env") ?: return

        val targetPath = Path.of(wrapper.file.absolutePath)
        val existingEntries = DotEnvIO.readEnvFile(targetPath)
        val newEntries = DotEnvParser.parse(output).entries

        if (existingEntries.isNotEmpty() && Files.exists(targetPath)) {
            val dialog = EnvDiffDialog(project, existingEntries, newEntries, "New Input")
            if (dialog.showAndGet()) {
                val merged = DotEnvFormatter.format(dialog.mergedEntries, currentOptions())
                DotEnvIO.writeEnvFile(targetPath, merged, backup = true)
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
                statusLabel.text = "Merged to ${wrapper.file.name}"
            }
        } else {
            DotEnvIO.writeEnvFile(targetPath, output, backup = true)
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
            statusLabel.text = "Saved to ${wrapper.file.name}"
        }
    }

    private fun copyToClipboard() {
        val output = outputArea.text
        if (output.isBlank()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output), null)
        statusLabel.text = "Copied to clipboard"
    }

    private fun currentOptions() = FormatOptions(
        exportPrefix = exportCheckbox.isSelected,
        sort = sortCheckbox.isSelected,
        ignoreLowercase = noLowerCheckbox.isSelected,
        urlOnly = urlOnlyCheckbox.isSelected,
    )

    private fun titledPanel(title: String, content: JComponent) = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder(title)
        add(content, BorderLayout.CENTER)
    }
}
