package dev.webbies.dotenvify.ui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import dev.webbies.dotenvify.core.*
import java.nio.file.Path

/**
 * Shared utility for the apply-to-.env workflow:
 * read existing → show merge dialog if needed → write with backup → VFS refresh → notify.
 */
object EnvFileApplicator {

    fun apply(
        project: Project,
        entries: List<EnvEntry>,
        targetPath: Path,
        sourceName: String,
        options: FormatOptions = FormatOptions(),
    ) {
        val existingEntries = DotEnvIO.readEnvFile(targetPath)

        if (existingEntries.isNotEmpty()) {
            val dialog = EnvDiffDialog(project, existingEntries, entries, sourceName)
            if (!dialog.showAndGet()) return
            val output = DotEnvFormatter.format(dialog.mergedEntries, options)
            DotEnvIO.writeEnvFile(targetPath, output, backup = true)
        } else {
            val output = DotEnvFormatter.format(entries, options)
            DotEnvIO.writeEnvFile(targetPath, output, backup = true)
        }

        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
        notify(project, "Saved ${entries.size} variables to ${targetPath.fileName}")
    }

    fun notify(project: Project, message: String, type: NotificationType = NotificationType.INFORMATION) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("DotEnvify.Notifications")
            .createNotification(message, type)
            .notify(project)
    }
}
