package dev.webbies.dotenvify.diagnostics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Watches for .env file changes in the project and notifies listeners.
 * Registered as a project-level service so it's scoped to the project lifecycle.
 */
@Service(Service.Level.PROJECT)
class EnvFileWatcher(private val project: Project) : Disposable {

    fun interface EnvChangeListener {
        fun onEnvFileChanged()
    }

    private val listeners = CopyOnWriteArrayList<EnvChangeListener>()
    private var watching = false

    fun addListener(listener: EnvChangeListener) {
        listeners.add(listener)
        if (!watching) {
            startWatching()
        }
    }

    fun removeListener(listener: EnvChangeListener) {
        listeners.remove(listener)
    }

    private fun startWatching() {
        watching = true
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val basePath = project.basePath ?: return
                    val envPath = Path.of(basePath, ".env").toString()

                    val envChanged = events.any { event ->
                        val path = event.path
                        path == envPath || path.endsWith("/.env")
                    }

                    if (envChanged && listeners.isNotEmpty()) {
                        listeners.forEach { it.onEnvFileChanged() }
                    }
                }
            }
        )
    }

    override fun dispose() {
        listeners.clear()
        watching = false
    }
}
