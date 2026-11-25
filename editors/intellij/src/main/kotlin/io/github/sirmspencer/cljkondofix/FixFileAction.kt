package io.github.sirmspencer.cljkondofix

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

private const val TIMEOUT_MS = 30_000
private val CLOJURE_EXTENSIONS = setOf("clj", "cljs", "cljc")

/**
 * Pipes the editor's document through `clj-kondo-fix --stdin` and replaces the
 * document with the fixed source as one undoable command.
 *
 * This is an action rather than a formatting service, so reformat code stays
 * with cljfmt in Cursive or clojure-lsp.
 */
class FixFileAction : DumbAwareAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = event.project != null &&
            event.getData(CommonDataKeys.EDITOR) != null &&
            isClojure(file)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val document = event.getData(CommonDataKeys.EDITOR)?.document ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val source = document.text
        val stamp = document.modificationStamp
        val workDirectory = file.parent?.path

        object : Task.Backgroundable(project, "Running clj-kondo-fix", true) {
            override fun run(indicator: ProgressIndicator) {
                // finding the executable may download it, so it happens here, off the ui thread
                val executable = try {
                    CljKondoFixExecutable.find(indicator)
                } catch (error: IOException) {
                    notify(project, error.message ?: "clj-kondo-fix was not found", NotificationType.ERROR)
                    return
                }
                indicator.text = "Running clj-kondo-fix"
                val commandLine = GeneralCommandLine(executable, "--stdin", "--filename", file.name)
                    .withCharset(StandardCharsets.UTF_8)
                // clj-kondo finds the project's .clj-kondo config from the working directory
                workDirectory?.let { commandLine.withWorkDirectory(File(it)) }
                val process = try {
                    CapturingProcessHandler(commandLine)
                } catch (error: ExecutionException) {
                    notify(project, "could not start clj-kondo-fix: ${error.message}", NotificationType.ERROR)
                    return
                }
                // clj-kondo-fix reads all of stdin before writing anything, so writing first cannot deadlock
                process.processInput.use { it.write(source.toByteArray(StandardCharsets.UTF_8)) }
                val output = process.runProcessWithProgressIndicator(indicator, TIMEOUT_MS)
                when {
                    output.isCancelled -> return
                    output.isTimeout ->
                        notify(project, "clj-kondo-fix did not finish within ${TIMEOUT_MS / 1000} seconds", NotificationType.ERROR)
                    output.exitCode != 0 ->
                        notify(project, output.stderr.trim().ifEmpty { "clj-kondo-fix exited with status ${output.exitCode}" }, NotificationType.ERROR)
                    output.stdout == source ->
                        notify(project, "clj-kondo-fix found nothing to fix", NotificationType.INFORMATION)
                    else -> apply(project, document, stamp, output.stdout)
                }
            }
        }.queue()
    }

    private fun apply(project: Project, document: com.intellij.openapi.editor.Document, stamp: Long, fixed: String) {
        ApplicationManager.getApplication().invokeLater {
            if (document.modificationStamp != stamp) {
                notify(project, "the file changed while clj-kondo-fix was running; run it again", NotificationType.WARNING)
                return@invokeLater
            }
            WriteCommandAction.runWriteCommandAction(project, "clj-kondo-fix", null, { document.setText(fixed) })
        }
    }

    private fun isClojure(file: VirtualFile?): Boolean =
        file?.extension?.lowercase() in CLOJURE_EXTENSIONS

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("clj-kondo-fix")
            .createNotification(message, type)
            .notify(project)
    }
}
