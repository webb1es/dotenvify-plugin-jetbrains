package dev.webbies.dotenvify.azure

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.core.DotEnvFormatter
import dev.webbies.dotenvify.core.DotEnvIO
import dev.webbies.dotenvify.core.EnvEntry
import dev.webbies.dotenvify.core.FormatOptions
import dev.webbies.dotenvify.ui.EnvDiffDialog
import dev.webbies.dotenvify.ui.MONO_FONT
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.*

class AzureVariableGroupPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val orgUrlField = JTextField(25).apply { toolTipText = "e.g. https://dev.azure.com/myorg/myproject" }
    private val groupNamesField = JTextField(20).apply { toolTipText = "Comma-separated, e.g. group1,group2" }
    private val signInButton = JButton("Sign in to Azure DevOps")
    private val signOutButton = JButton("Sign out").apply { isVisible = false }
    private val fetchButton = JButton("Fetch Variables").apply { isEnabled = false }
    private val applyButton = JButton("Apply to .env").apply { isEnabled = false }
    private val statusLabel = JLabel("Not signed in")
    private val previewArea = JBTextArea().apply { isEditable = false; font = MONO_FONT }
    private var fetchedEntries: List<EnvEntry> = emptyList()

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val fieldsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(labeledRow("Azure DevOps URL:", orgUrlField))
            add(Box.createVerticalStrut(4))
            add(labeledRow("Variable Group(s):", groupNamesField))
        }

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            listOf(signInButton, signOutButton, fetchButton, applyButton).forEach {
                add(it); add(Box.createHorizontalStrut(8))
            }
            add(Box.createHorizontalGlue())
            add(statusLabel)
        }

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(fieldsPanel)
            add(Box.createVerticalStrut(8))
            add(buttonPanel)
        }

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(previewArea).apply {
            preferredSize = Dimension(600, 300)
            border = BorderFactory.createTitledBorder("Preview")
        }, BorderLayout.CENTER)

        signInButton.addActionListener { startDeviceCodeSignIn() }
        signOutButton.addActionListener { signOut() }
        fetchButton.addActionListener { fetchVariables() }
        applyButton.addActionListener { applyToFile() }
        updateAuthState()
    }

    private fun updateAuthState() {
        val authed = AzureAuthProvider.isAuthenticated()
        signInButton.isVisible = !authed
        signOutButton.isVisible = authed
        fetchButton.isEnabled = authed
        statusLabel.text = if (authed) "Signed in" else "Not signed in"
    }

    private fun startDeviceCodeSignIn() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Signing in to Azure DevOps...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Requesting device code..."
                    val deviceCode = AzureAuthProvider.startDeviceCodeFlow()
                    ApplicationManager.getApplication().invokeLater {
                        val choice = Messages.showOkCancelDialog(
                            project,
                            "Enter this code: ${deviceCode.userCode}\n\nA browser will open to ${deviceCode.verificationUri}\nPaste the code there to sign in.",
                            "Azure DevOps Sign In", "Open Browser", "Cancel", Messages.getInformationIcon()
                        )
                        if (choice == Messages.OK) {
                            BrowserUtil.browse(deviceCode.verificationUri)
                            pollForToken(deviceCode)
                        }
                    }
                } catch (e: Exception) {
                    showError("Sign in failed: ${e.message}")
                }
            }
        })
    }

    private fun pollForToken(deviceCode: DeviceCodeResponse) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Waiting for authentication...", true) {
            override fun run(indicator: ProgressIndicator) {
                val deadline = System.currentTimeMillis() + (deviceCode.expiresIn * 1000L)
                val interval = (deviceCode.interval * 1000L).coerceAtLeast(5000)

                while (System.currentTimeMillis() < deadline) {
                    if (indicator.isCanceled) return
                    Thread.sleep(interval)
                    indicator.text = "Waiting for browser sign-in..."
                    try {
                        if (AzureAuthProvider.pollForToken(deviceCode.deviceCode) != null) {
                            ApplicationManager.getApplication().invokeLater {
                                updateAuthState()
                                Messages.showInfoMessage(project, "Successfully signed in to Azure DevOps!", "DotEnvify")
                            }
                            return
                        }
                    } catch (e: Exception) {
                        showError("Authentication failed: ${e.message}")
                        return
                    }
                }
                showError("Authentication timed out. Please try again.")
            }
        })
    }

    private fun signOut() {
        AzureAuthProvider.signOut()
        updateAuthState()
        previewArea.text = ""
        fetchedEntries = emptyList()
        applyButton.isEnabled = false
    }

    private fun fetchVariables() {
        val orgUrl = orgUrlField.text.trim()
        val groups = groupNamesField.text.trim()

        if (orgUrl.isEmpty() || groups.isEmpty()) {
            Messages.showWarningDialog(project, "Please fill in Azure DevOps URL and group name(s).", "DotEnvify")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching variables from Azure DevOps...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val accessToken = AzureAuthProvider.getAccessToken()
                        ?: throw RuntimeException("Not authenticated. Please sign in first.")
                    val (org, proj) = AzureConnection.parseUrl(orgUrl)
                    if (proj == null) throw IllegalArgumentException("URL must include the project, e.g. https://dev.azure.com/myorg/myproject")

                    val result = AzureDevOpsClient(org, proj).fetchVariables(groups, accessToken)
                    val entries = result.variables.map { (k, v) -> EnvEntry(k, v) }

                    ApplicationManager.getApplication().invokeLater {
                        fetchedEntries = entries
                        val output = DotEnvFormatter.format(entries, FormatOptions())
                        previewArea.text = if (result.warnings.isNotEmpty()) {
                            "# Warnings:\n${result.warnings.joinToString("\n") { "# $it" }}\n\n$output"
                        } else output
                        applyButton.isEnabled = entries.isNotEmpty()
                        statusLabel.text = "${entries.size} variables fetched"
                    }
                } catch (e: Exception) {
                    val rootCause = generateSequence<Throwable>(e) { it.cause }.last()
                    val detail = if (rootCause !== e) "${e.message} (${rootCause.javaClass.simpleName}: ${rootCause.message})" else e.message
                    showError("Fetch failed: $detail")
                }
            }
        })
    }

    private fun applyToFile() {
        if (fetchedEntries.isEmpty()) return
        val targetPath = Path.of(project.basePath ?: return, ".env")
        val existingEntries = DotEnvIO.readEnvFile(targetPath)

        if (existingEntries.isNotEmpty()) {
            val dialog = EnvDiffDialog(project, existingEntries, fetchedEntries, "Azure DevOps")
            if (dialog.showAndGet()) {
                val output = DotEnvFormatter.format(dialog.mergedEntries, FormatOptions())
                DotEnvIO.writeEnvFile(targetPath, output, backup = true)
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
                Messages.showInfoMessage(project, "Merged ${dialog.mergedEntries.size} variables to .env", "DotEnvify")
            }
        } else {
            val output = DotEnvFormatter.format(fetchedEntries, FormatOptions())
            DotEnvIO.writeEnvFile(targetPath, output, backup = true)
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
            Messages.showInfoMessage(project, "Saved ${fetchedEntries.size} variables to .env", "DotEnvify")
        }
    }

    private fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(project, message, "DotEnvify")
        }
    }

    private fun labeledRow(label: String, field: JComponent): JPanel {
        return JPanel(BorderLayout(8, 0)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            val lbl = JLabel(label)
            lbl.preferredSize = Dimension(130, lbl.preferredSize.height)
            add(lbl, BorderLayout.WEST)
            add(field, BorderLayout.CENTER)
        }
    }
}
