package dev.webbies.dotenvify.ui

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.core.*
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.dnd.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DotEnvifyToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val inputArea = JBTextArea().apply { font = MONO_FONT; lineWrap = true; emptyText.text = "Paste raw key-value data here..." }
    private val outputArea = JBTextArea().apply { font = MONO_FONT; isEditable = false; lineWrap = true }
    private val optionsPanel = FormatOptionsPanel(project)
    private val statusLabel = JLabel(" ")

    private var currentEntries: List<EnvEntry> = emptyList()

    /** Debounce timer — delays preview update by 200ms after last keystroke. */
    private val debounceTimer = Timer(200) { updatePreview() }.apply { isRepeats = false }

    init {
        val splitter = JBSplitter(true, 0.5f).apply {
            firstComponent = titledPanel("Input", JBScrollPane(inputArea))
            secondComponent = titledPanel("Output Preview", JBScrollPane(outputArea))
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
            add(JButton("Paste").apply { addActionListener { pasteFromClipboard() } })
            add(Box.createHorizontalStrut(8))
            add(JButton("Apply to .env").apply { addActionListener { applyToFile() } })
            add(Box.createHorizontalStrut(8))
            add(JButton("Copy to Clipboard").apply { addActionListener { copyToClipboard() } })
            add(Box.createHorizontalStrut(8))
            add(JButton("Clear").apply { addActionListener { inputArea.text = ""; outputArea.text = ""; statusLabel.text = " "; currentEntries = emptyList() } })
            add(Box.createHorizontalGlue())
            add(statusLabel)
        }

        add(optionsPanel, BorderLayout.NORTH)
        add(splitter, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // Debounced preview on input changes
        inputArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = schedulePreview()
            override fun removeUpdate(e: DocumentEvent) = schedulePreview()
            override fun changedUpdate(e: DocumentEvent) = schedulePreview()
        })
        optionsPanel.onChange { updatePreview() }

        // Drag-and-drop file support
        setupDragAndDrop()
    }

    private fun schedulePreview() {
        debounceTimer.restart()
    }

    private fun updatePreview() {
        val input = inputArea.text
        if (input.isBlank()) { outputArea.text = ""; statusLabel.text = " "; currentEntries = emptyList(); return }

        val result = DotEnvParser.parse(input)
        currentEntries = result.entries
        if (result.entries.isEmpty()) { outputArea.text = ""; statusLabel.text = "No entries found"; return }

        outputArea.text = DotEnvFormatter.format(result.entries, optionsPanel.options())
        val warnings = if (result.warnings.isNotEmpty()) " | ${result.warnings.size} warning(s)" else ""
        statusLabel.text = "${result.entries.size} entries$warnings"
    }

    private fun applyToFile() {
        if (currentEntries.isEmpty()) {
            EnvFileApplicator.notify(project, "Nothing to save. Paste input first.", com.intellij.notification.NotificationType.WARNING)
            return
        }

        val basePath = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(FileSaverDescriptor("Save .env File", "Choose where to save the .env file"), project)
            .save(basePath, ".env") ?: return

        val targetPath = Path.of(wrapper.file.absolutePath)
        EnvFileApplicator.apply(project, currentEntries, targetPath, "New Input", optionsPanel.options())
    }

    private fun copyToClipboard() {
        val output = outputArea.text
        if (output.isBlank()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output), null)
        statusLabel.text = "Copied to clipboard"
    }

    private fun pasteFromClipboard() {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val text = clipboard.getData(DataFlavor.stringFlavor) as? String ?: return
            inputArea.text = text
        } catch (_: Exception) {
            // Clipboard unavailable or wrong flavor
        }
    }

    private fun setupDragAndDrop() {
        DropTarget(inputArea, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                try {
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<java.io.File>
                        val file = files.firstOrNull() ?: return
                        inputArea.text = Files.readString(file.toPath())
                    } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        inputArea.text = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    }
                    dtde.dropComplete(true)
                } catch (_: Exception) {
                    dtde.dropComplete(false)
                }
            }
        })
    }

    private fun titledPanel(title: String, content: JComponent) = JPanel(BorderLayout()).apply {
        border = BorderFactory.createTitledBorder(title)
        add(content, BorderLayout.CENTER)
    }
}
