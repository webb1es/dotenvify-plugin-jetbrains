package dev.webbies.dotenvify.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.core.EnvEntry
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Merge preview dialog comparing existing .env with incoming variables.
 * Incoming values take precedence for conflicts; existing-only keys are kept.
 */
class EnvDiffDialog(
    project: Project,
    private val existingEntries: List<EnvEntry>,
    private val incomingEntries: List<EnvEntry>,
    private val sourceName: String = "Incoming",
) : DialogWrapper(project) {

    private enum class Status { ADDED, REMOVED, CHANGED, UNCHANGED }
    private data class MergeRow(val key: String, val existing: String?, val incoming: String?, val status: Status)

    private val mergeRows = buildDiff()
    val mergedEntries: List<EnvEntry> = mergeRows.map { row ->
        EnvEntry(row.key, row.incoming ?: row.existing!!)
    }

    init {
        title = "Merge Preview"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val counts = Status.entries.associateWith { s -> mergeRows.count { it.status == s } }
        val sb = StringBuilder().apply {
            appendLine("=== MERGE PREVIEW ===")
            appendLine("Existing .env: ${existingEntries.size} keys | $sourceName: ${incomingEntries.size} keys")
            appendLine()
            appendLine("  + ${counts[Status.ADDED]} new  ~ ${counts[Status.CHANGED]} changed  - ${counts[Status.REMOVED]} kept  = ${counts[Status.UNCHANGED]} unchanged")
            appendLine()

            appendSection("NEW KEYS (from $sourceName)", Status.ADDED) { "  + ${it.key}=${it.incoming}" }
            appendSection("CHANGED VALUES", Status.CHANGED) { "  ${it.key}\n    existing: ${it.existing}\n    incoming: ${it.incoming}" }
            appendSection("ONLY IN EXISTING .env (kept)", Status.REMOVED) { "  ${it.key}=${it.existing}" }
        }

        val diffArea = JBTextArea(sb.toString()).apply {
            isEditable = false
            font = MONO_FONT
            caretPosition = 0
        }

        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(diffArea).apply { preferredSize = Dimension(650, 450) }, BorderLayout.CENTER)
            add(JLabel("Click OK to apply merge (incoming values take precedence for conflicts).").apply {
                border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
            }, BorderLayout.SOUTH)
        }
    }

    private fun StringBuilder.appendSection(title: String, status: Status, format: (MergeRow) -> String) {
        val rows = mergeRows.filter { it.status == status }
        if (rows.isEmpty()) return
        appendLine("--- $title ---")
        rows.forEach { appendLine(format(it)) }
        appendLine()
    }

    private fun buildDiff(): List<MergeRow> {
        val existingMap = existingEntries.associate { it.key to it.value }
        val incomingMap = incomingEntries.associate { it.key to it.value }
        return (existingMap.keys + incomingMap.keys).sorted().map { key ->
            val e = existingMap[key]
            val i = incomingMap[key]
            MergeRow(key, e, i, when {
                e == null -> Status.ADDED
                i == null -> Status.REMOVED
                e != i -> Status.CHANGED
                else -> Status.UNCHANGED
            })
        }
    }
}
