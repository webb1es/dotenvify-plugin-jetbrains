package dev.webbies.dotenvify.azure

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.webbies.dotenvify.core.DotEnvFormatter
import dev.webbies.dotenvify.core.EnvEntry
import dev.webbies.dotenvify.diagnostics.EnvFileWatcher
import dev.webbies.dotenvify.settings.DotEnvifyProjectSettings
import dev.webbies.dotenvify.settings.DotEnvifySettings
import dev.webbies.dotenvify.ui.EnvFileApplicator
import dev.webbies.dotenvify.ui.FormatOptionsPanel
import dev.webbies.dotenvify.ui.MONO_FONT
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.net.ConnectException
import java.net.http.HttpTimeoutException
import java.nio.file.Path
import javax.swing.*

class AzureVariableGroupPanel(private val project: Project) : JPanel(BorderLayout()) {

    // --- Connection fields ---
    private val orgUrlField = JTextField().apply {
        toolTipText = "e.g. https://dev.azure.com/myorg/myproject"
    }
    private val groupNameField = JTextField().apply {
        toolTipText = "e.g. my-variable-group"
    }

    // --- Auth controls ---
    private val signInButton = JButton("Sign in").apply { icon = AllIcons.Actions.Execute }
    private val signOutButton = JButton("Sign out").apply { isVisible = false }
    private val authStatusIcon = JLabel(AllIcons.General.InspectionsError)
    private val authStatusLabel = JLabel("Not connected")

    // --- Action controls ---
    private val fetchButton = JButton("Fetch Variables").apply { isEnabled = false; icon = AllIcons.Actions.Download }
    private val applyButton = JButton("Apply to .env").apply { isEnabled = false; icon = AllIcons.Actions.MenuSaveall }

    // --- Format options ---
    private val optionsPanel = FormatOptionsPanel(project)
    private val autoWatchCheckbox = JCheckBox("Auto-watch .env", true).apply {
        toolTipText = "Automatically refresh preview when .env file changes"
    }

    // --- Preview ---
    private val previewArea = JBTextArea().apply { isEditable = false; font = MONO_FONT; lineWrap = true }
    private val groupInfoLabel = JLabel(" ").apply {
        font = font.deriveFont(Font.ITALIC)
        foreground = JBColor.GRAY
    }
    private val statusLabel = JLabel(" ")

    private var fetchedEntries: List<EnvEntry> = emptyList()

    private val watcherListener = EnvFileWatcher.EnvChangeListener {
        ApplicationManager.getApplication().invokeLater {
            if (autoWatchCheckbox.isSelected) refreshPreview()
        }
    }

    init {
        border = JBUI.Borders.empty(8)

        // === TOP: Connection fields + auth ===
        val connectionPanel = buildConnectionPanel()

        // === OPTIONS: Format options + auto-watch ===
        val optionsRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            add(optionsPanel, BorderLayout.CENTER)
            add(autoWatchCheckbox, BorderLayout.EAST)
        }

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            connectionPanel.alignmentX = LEFT_ALIGNMENT
            optionsRow.alignmentX = LEFT_ALIGNMENT
            add(connectionPanel)
            add(optionsRow)
        }

        // === CENTER: Preview ===
        val previewPanel = JPanel(BorderLayout(0, 4)).apply {
            border = BorderFactory.createTitledBorder("Preview")
            add(groupInfoLabel, BorderLayout.NORTH)
            add(JBScrollPane(previewArea), BorderLayout.CENTER)
        }

        // === BOTTOM: Action buttons + status ===
        val bottomRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            val actions = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(fetchButton)
                add(applyButton)
            }
            add(actions, BorderLayout.WEST)
            add(statusLabel, BorderLayout.EAST)
        }

        add(topPanel, BorderLayout.NORTH)
        add(previewPanel, BorderLayout.CENTER)
        add(bottomRow, BorderLayout.SOUTH)

        // --- Wire events ---
        signInButton.addActionListener { startDeviceCodeSignIn() }
        signOutButton.addActionListener { signOut() }
        fetchButton.addActionListener { fetchVariables() }
        applyButton.addActionListener { applyToFile() }
        optionsPanel.onChange { refreshPreview() }

        loadPersistedFields()
        updateAuthState()
        persistFieldsOnChange()

        // Auto-watch enabled by default — register listener immediately
        project.service<EnvFileWatcher>().addListener(watcherListener)
        autoWatchCheckbox.addItemListener {
            val watcher = project.service<EnvFileWatcher>()
            if (autoWatchCheckbox.isSelected) watcher.addListener(watcherListener)
            else watcher.removeListener(watcherListener)
        }
    }

    private fun buildConnectionPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // Row 0: Azure DevOps URL (full width)
        gbc.gridx = 0; gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE
        gbc.insets = Insets(0, 0, 4, 8); gbc.weightx = 0.0
        panel.add(JLabel("Azure DevOps URL:"), gbc)

        gbc.gridx = 1; gbc.gridwidth = 3
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(orgUrlField, gbc)

        // Row 1: Variable Group + Auth
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        gbc.insets = Insets(0, 0, 0, 8)
        panel.add(JLabel("Variable Group:"), gbc)

        gbc.gridx = 1; gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(groupNameField, gbc)

        gbc.gridx = 2; gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        gbc.insets = Insets(0, 16, 0, 0)
        val authRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(authStatusIcon)
            add(authStatusLabel)
            add(signInButton)
            add(signOutButton)
        }
        panel.add(authRow, gbc)

        return panel
    }

    // --- Persistence ---

    private fun loadPersistedFields() {
        val globalState = DotEnvifySettings.getInstance().state
        if (globalState.azureOrgUrl.isNotEmpty()) orgUrlField.text = globalState.azureOrgUrl

        val projectState = DotEnvifyProjectSettings.getInstance(project).state
        if (projectState.azureGroupName.isNotEmpty()) groupNameField.text = projectState.azureGroupName
    }

    private fun persistFieldsOnChange() {
        orgUrlField.document.addDocumentListener(SimpleDocListener {
            DotEnvifySettings.getInstance().state.azureOrgUrl = orgUrlField.text.trim()
        })
        groupNameField.document.addDocumentListener(SimpleDocListener {
            DotEnvifyProjectSettings.getInstance(project).state.azureGroupName = groupNameField.text.trim()
        })
    }

    // --- Auth ---

    private fun updateAuthState() {
        val authed = AzureAuthProvider.isAuthenticated()
        signInButton.isVisible = !authed
        signOutButton.isVisible = authed
        fetchButton.isEnabled = authed
        if (authed) {
            authStatusIcon.icon = AllIcons.General.InspectionsOK
            authStatusLabel.text = "Connected"
            authStatusLabel.foreground = JBColor(Color(0, 128, 0), Color(80, 200, 80))
        } else {
            authStatusIcon.icon = AllIcons.General.InspectionsError
            authStatusLabel.text = "Not connected"
            authStatusLabel.foreground = JBColor.GRAY
        }
    }

    private fun startDeviceCodeSignIn() {
        signInButton.isEnabled = false
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Signing in to Azure DevOps...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Requesting device code..."
                    val deviceCode = AzureAuthProvider.startDeviceCodeFlow()
                    ApplicationManager.getApplication().invokeLater {
                        val dialog = DeviceCodeDialog(project, deviceCode)
                        if (dialog.showAndGet()) {
                            BrowserUtil.browse(deviceCode.verificationUri)
                            pollForToken(deviceCode)
                        } else {
                            signInButton.isEnabled = true
                        }
                    }
                } catch (e: ConnectException) {
                    showError("Cannot reach Azure AD. Check your network connection.")
                    resetSignInButton()
                } catch (e: HttpTimeoutException) {
                    showError("Connection to Azure AD timed out. Try again.")
                    resetSignInButton()
                } catch (e: RuntimeException) {
                    showError("Sign in failed: ${e.message}")
                    resetSignInButton()
                }
            }
        })
    }

    private fun resetSignInButton() {
        ApplicationManager.getApplication().invokeLater { signInButton.isEnabled = true }
    }

    private fun pollForToken(deviceCode: DeviceCodeResponse) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Waiting for authentication...", true) {
            override fun run(indicator: ProgressIndicator) {
                val deadline = System.currentTimeMillis() + (deviceCode.expiresIn * 1000L)
                val interval = (deviceCode.interval * 1000L).coerceAtLeast(5000)

                while (System.currentTimeMillis() < deadline) {
                    if (indicator.isCanceled) { resetSignInButton(); return }
                    Thread.sleep(interval)
                    indicator.text = "Waiting for browser sign-in..."
                    try {
                        if (AzureAuthProvider.pollForToken(deviceCode.deviceCode) != null) {
                            ApplicationManager.getApplication().invokeLater {
                                updateAuthState()
                                EnvFileApplicator.notify(project, "Connected to Azure DevOps")
                            }
                            return
                        }
                    } catch (e: RuntimeException) {
                        showError("Authentication failed: ${e.message}")
                        resetSignInButton()
                        return
                    }
                }
                showError("Authentication timed out. Please try again.")
                resetSignInButton()
            }
        })
    }

    private fun signOut() {
        AzureAuthProvider.signOut()
        updateAuthState()
        previewArea.text = ""
        groupInfoLabel.text = " "
        statusLabel.text = " "
        fetchedEntries = emptyList()
        applyButton.isEnabled = false
    }

    // --- Fetch & Apply ---

    private fun fetchVariables() {
        val orgUrl = orgUrlField.text.trim()
        val groupName = groupNameField.text.trim()

        if (orgUrl.isEmpty()) {
            EnvFileApplicator.notify(project, "Please enter your Azure DevOps URL.", NotificationType.WARNING)
            return
        }
        if (groupName.isEmpty()) {
            EnvFileApplicator.notify(project, "Please enter a variable group name.", NotificationType.WARNING)
            return
        }

        fetchButton.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching variables from Azure DevOps...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val accessToken = AzureAuthProvider.getAccessToken()
                        ?: throw RuntimeException("Not authenticated. Please sign in first.")
                    val (org, proj) = AzureConnection.parseUrl(orgUrl)
                    if (proj == null) throw IllegalArgumentException("URL must include the project, e.g. https://dev.azure.com/myorg/myproject")

                    indicator.text = "Fetching '$groupName'..."
                    val result = AzureDevOpsClient(org, proj).fetchVariables(groupName, accessToken)
                    val entries = result.variables.map { (k, v) -> EnvEntry(k, v) }

                    ApplicationManager.getApplication().invokeLater {
                        fetchedEntries = entries
                        groupInfoLabel.text = if (result.groupDescription.isNotEmpty()) {
                            "${result.groupName} — ${result.groupDescription}"
                        } else {
                            result.groupName
                        }
                        refreshPreview()

                        if (result.warnings.isNotEmpty()) {
                            val warningText = result.warnings.joinToString("\n")
                            EnvFileApplicator.notify(project, warningText, NotificationType.WARNING)
                        }

                        applyButton.isEnabled = entries.isNotEmpty()
                        fetchButton.isEnabled = true
                    }
                } catch (e: IllegalArgumentException) {
                    showError("Invalid input: ${e.message}")
                    enableFetch()
                } catch (e: ConnectException) {
                    showError("Cannot reach Azure DevOps. Check your network and URL.")
                    enableFetch()
                } catch (e: HttpTimeoutException) {
                    showError("Request timed out. Try again.")
                    enableFetch()
                } catch (e: RuntimeException) {
                    val rootCause = generateSequence<Throwable>(e) { it.cause }.last()
                    val detail = if (rootCause !== e) "${e.message} (${rootCause.javaClass.simpleName}: ${rootCause.message})" else e.message
                    showError("Fetch failed: $detail")
                    enableFetch()
                }
            }
        })
    }

    private fun refreshPreview() {
        if (fetchedEntries.isEmpty()) {
            previewArea.text = ""
            statusLabel.text = " "
            return
        }
        val output = DotEnvFormatter.format(fetchedEntries, optionsPanel.options())
        previewArea.text = output
        previewArea.caretPosition = 0
        statusLabel.text = "${fetchedEntries.size} variables"
    }

    private fun applyToFile() {
        if (fetchedEntries.isEmpty()) return
        val targetPath = Path.of(project.basePath ?: return, ".env")
        EnvFileApplicator.apply(project, fetchedEntries, targetPath, "Azure DevOps", optionsPanel.options())
    }

    // --- Helpers ---

    private fun enableFetch() {
        ApplicationManager.getApplication().invokeLater { fetchButton.isEnabled = true }
    }

    private fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            EnvFileApplicator.notify(project, message, NotificationType.ERROR)
        }
    }

    private class SimpleDocListener(private val action: () -> Unit) : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = action()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = action()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = action()
    }
}

/**
 * Custom dialog for the device code sign-in flow.
 * Shows the code prominently with a copy-to-clipboard button.
 */
private class DeviceCodeDialog(
    project: Project,
    private val deviceCode: DeviceCodeResponse,
) : DialogWrapper(project) {

    init {
        title = "Azure DevOps — Sign In"
        setOKButtonText("Open Browser")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 12)).apply {
            border = JBUI.Borders.empty(8)
        }

        val instructions = JLabel("<html>Enter this code on the Microsoft sign-in page:</html>")

        val codeLabel = JLabel(deviceCode.userCode).apply {
            font = Font(Font.MONOSPACED, Font.BOLD, 28)
            horizontalAlignment = SwingConstants.CENTER
        }

        val copyButton = JButton("Copy Code").apply {
            icon = AllIcons.Actions.Copy
            addActionListener {
                val selection = StringSelection(deviceCode.userCode)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                text = "Copied!"
                Timer(2000) { text = "Copy Code" }.apply { isRepeats = false; start() }
            }
        }

        val codePanel = JPanel(BorderLayout(8, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(12),
            )
            add(codeLabel, BorderLayout.CENTER)
            add(copyButton, BorderLayout.EAST)
        }

        val hint = JLabel("<html><i>Click 'Open Browser' to navigate to the Microsoft sign-in page,<br>" +
                "then paste the code above when prompted.</i></html>").apply {
            foreground = JBColor.GRAY
        }

        panel.add(instructions, BorderLayout.NORTH)
        panel.add(codePanel, BorderLayout.CENTER)
        panel.add(hint, BorderLayout.SOUTH)

        return panel
    }
}
