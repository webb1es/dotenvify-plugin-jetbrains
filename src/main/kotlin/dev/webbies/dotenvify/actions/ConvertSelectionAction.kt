package dev.webbies.dotenvify.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.core.DotEnvFormatter
import dev.webbies.dotenvify.core.DotEnvParser
import dev.webbies.dotenvify.core.FormatOptions
import dev.webbies.dotenvify.ui.FormatOptionsPanel
import dev.webbies.dotenvify.ui.MONO_FONT
import dev.webbies.dotenvify.ui.EnvFileApplicator
import com.intellij.notification.NotificationType
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ConvertSelectionAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText

        if (selectedText.isNullOrBlank()) {
            EnvFileApplicator.notify(project, "Please select some text to convert.", NotificationType.WARNING)
            return
        }

        val parseResult = DotEnvParser.parse(selectedText)
        if (parseResult.entries.isEmpty()) {
            EnvFileApplicator.notify(project, "No key-value pairs found in selection.", NotificationType.WARNING)
            return
        }

        val dialog = PreviewDialog(project, parseResult.entries.size) { options ->
            DotEnvFormatter.format(parseResult.entries, options)
        }
        if (dialog.showAndGet()) {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.replaceString(
                    editor.selectionModel.selectionStart,
                    editor.selectionModel.selectionEnd,
                    dialog.getFormattedOutput().trimEnd('\n'),
                )
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null && editor.selectionModel.hasSelection()
    }
}

/** Preview dialog with format option toggles and live preview. */
class PreviewDialog(
    project: Project,
    entryCount: Int,
    private val formatter: (FormatOptions) -> String,
) : DialogWrapper(project) {

    private val previewArea = JBTextArea().apply { isEditable = false; font = MONO_FONT }
    private val optionsPanel = FormatOptionsPanel(project)

    init {
        title = "DotEnvify — Preview ($entryCount entries)"
        setOKButtonText("Apply")
        init()
        updatePreview()
    }

    override fun createCenterPanel(): JComponent {
        optionsPanel.onChange { updatePreview() }

        return JPanel(BorderLayout(0, 8)).apply {
            add(optionsPanel, BorderLayout.NORTH)
            add(JBScrollPane(previewArea).apply { preferredSize = Dimension(600, 400) }, BorderLayout.CENTER)
        }
    }

    fun getFormattedOutput(): String = formatter(optionsPanel.options())

    private fun updatePreview() { previewArea.text = getFormattedOutput() }
}
