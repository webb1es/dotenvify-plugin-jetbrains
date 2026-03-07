package dev.webbies.dotenvify.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import dev.webbies.dotenvify.core.DotEnvFormatter
import dev.webbies.dotenvify.core.DotEnvIO
import dev.webbies.dotenvify.core.DotEnvParser
import java.nio.file.Path

class ConvertFileAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val content = String(virtualFile.contentsToByteArray(), Charsets.UTF_8)
        val parseResult = DotEnvParser.parse(content)

        if (parseResult.entries.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "No key-value pairs found in '${virtualFile.name}'.",
                "DotEnvify"
            )
            return
        }

        val dialog = PreviewDialog(project, parseResult.entries.size) { options ->
            DotEnvFormatter.format(parseResult.entries, options)
        }

        if (dialog.showAndGet()) {
            val output = dialog.getFormattedOutput()

            // Ask where to save
            val descriptor = FileSaverDescriptor("Save .env File", "Choose where to save the .env file")
            val wrapper = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, project)
                .save(virtualFile.parent, ".env")

            if (wrapper != null) {
                val targetPath = Path.of(wrapper.file.absolutePath)
                DotEnvIO.writeEnvFile(targetPath, output, backup = true)

                // Refresh VFS so the file shows up in the IDE
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)

                Messages.showInfoMessage(
                    project,
                    "Saved ${parseResult.entries.size} variables to '${wrapper.file.name}'.",
                    "DotEnvify"
                )
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.VIRTUAL_FILE) != null
    }
}
