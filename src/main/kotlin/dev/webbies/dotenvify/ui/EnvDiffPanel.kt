package dev.webbies.dotenvify.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import dev.webbies.dotenvify.core.DotEnvParser
import dev.webbies.dotenvify.core.EnvEntry
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

/**
 * Merge preview dialog comparing existing .env with incoming variables.
 * Users can choose per-key which value to keep for conflicts.
 */
class EnvDiffDialog(
    project: Project,
    private val existingEntries: List<EnvEntry>,
    private val incomingEntries: List<EnvEntry>,
    private val sourceName: String = "Incoming",
) : DialogWrapper(project) {

    private enum class Status { ADDED, REMOVED, CHANGED, UNCHANGED }

    private data class MergeRow(
        val key: String,
        val existing: String?,
        val incoming: String?,
        val status: Status,
        var useIncoming: Boolean = true,
    )

    private val mergeRows = buildDiff()

    val mergedEntries: List<EnvEntry>
        get() = mergeRows.map { row ->
            val value = when {
                row.status == Status.CHANGED -> if (row.useIncoming) row.incoming!! else row.existing!!
                row.incoming != null -> row.incoming
                else -> row.existing!!
            }
            EnvEntry(row.key, value)
        }

    init {
        title = "Merge Preview"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val counts = Status.entries.associateWith { s -> mergeRows.count { it.status == s } }

        val summaryLabel = JLabel(
            "Existing .env: ${existingEntries.size} keys | $sourceName: ${incomingEntries.size} keys  —  " +
                "+ ${counts[Status.ADDED]} new  ~ ${counts[Status.CHANGED]} changed  " +
                "- ${counts[Status.REMOVED]} kept  = ${counts[Status.UNCHANGED]} unchanged"
        )

        val tableModel = object : AbstractTableModel() {
            private val columns = arrayOf("Key", "Existing Value", "$sourceName Value", "Status", "Use Incoming")

            override fun getRowCount() = mergeRows.size
            override fun getColumnCount() = columns.size
            override fun getColumnName(col: Int) = columns[col]

            override fun getValueAt(row: Int, col: Int): Any {
                val r = mergeRows[row]
                return when (col) {
                    0 -> r.key
                    1 -> r.existing ?: ""
                    2 -> r.incoming ?: ""
                    3 -> r.status.name
                    4 -> r.useIncoming
                    else -> ""
                }
            }

            override fun isCellEditable(row: Int, col: Int): Boolean {
                return col == 4 && mergeRows[row].status == Status.CHANGED
            }

            override fun setValueAt(value: Any?, row: Int, col: Int) {
                if (col == 4) {
                    mergeRows[row].useIncoming = value as Boolean
                    fireTableCellUpdated(row, col)
                }
            }

            override fun getColumnClass(col: Int): Class<*> {
                return if (col == 4) java.lang.Boolean::class.java else String::class.java
            }
        }

        val table = JTable(tableModel).apply {
            setDefaultRenderer(String::class.java, StatusCellRenderer())
            autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            columnModel.getColumn(0).preferredWidth = 150
            columnModel.getColumn(1).preferredWidth = 200
            columnModel.getColumn(2).preferredWidth = 200
            columnModel.getColumn(3).preferredWidth = 80
            columnModel.getColumn(4).preferredWidth = 90
        }

        val helpLabel = JLabel("Tip: Toggle 'Use Incoming' checkboxes on changed keys to pick which value to keep.").apply {
            border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
        }

        return JPanel(BorderLayout(0, 8)).apply {
            add(summaryLabel, BorderLayout.NORTH)
            add(JBScrollPane(table).apply { preferredSize = Dimension(720, 400) }, BorderLayout.CENTER)
            add(helpLabel, BorderLayout.SOUTH)
        }
    }

    private fun buildDiff(): MutableList<MergeRow> {
        val existingMap = existingEntries.associate { it.key to it.value }
        val incomingMap = incomingEntries.associate { it.key to it.value }
        return (existingMap.keys + incomingMap.keys).sorted().map { key ->
            val e = existingMap[key]
            val i = incomingMap[key]
            MergeRow(key, e, i, when {
                e == null -> Status.ADDED
                i == null -> Status.REMOVED
                DotEnvParser.unquote(e) != DotEnvParser.unquote(i) -> Status.CHANGED
                else -> Status.UNCHANGED
            })
        }.toMutableList()
    }

    private class StatusCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, col: Int,
        ): Component {
            val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col)
            if (!isSelected && col == 3) {
                foreground = when (value) {
                    "ADDED" -> JBColor(java.awt.Color(0, 128, 0), java.awt.Color(80, 200, 80))
                    "CHANGED" -> JBColor(java.awt.Color(200, 150, 0), java.awt.Color(220, 180, 50))
                    "REMOVED" -> JBColor(java.awt.Color(128, 128, 128), java.awt.Color(160, 160, 160))
                    else -> table.foreground
                }
            }
            return comp
        }
    }
}
