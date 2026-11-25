package io.github.sirmspencer.cljkondofix

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import com.intellij.util.system.CpuArch
import java.io.File

/** Application-wide settings, stored in clj-kondo-fix.xml in the IDE config directory. */
@Service(Service.Level.APP)
@State(name = "CljKondoFixSettings", storages = [Storage("clj-kondo-fix.xml")])
class CljKondoFixSettings : SimplePersistentStateComponent<CljKondoFixSettings.Options>(Options()) {

    class Options : BaseState() {
        // empty means use the bundled binary, then clj-kondo-fix on the path or in homebrew
        var path by string("")
    }

    companion object {
        fun getInstance(): CljKondoFixSettings = service()
    }
}

object CljKondoFixExecutable {
    private val name = if (SystemInfo.isWindows) "clj-kondo-fix.exe" else "clj-kondo-fix"

    // apps started from the dock may not see the shell path, so check homebrew directly
    private val homebrewPaths = listOf("/opt/homebrew/bin/clj-kondo-fix", "/usr/local/bin/clj-kondo-fix")

    /** The configured path, then the bundled binary, then clj-kondo-fix on the path or in homebrew. */
    fun find(): String? {
        val configured = CljKondoFixSettings.getInstance().state.path?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            return configured
        }
        bundled()?.let { return it }
        onPath()?.let { return it }
        return homebrewPaths.firstOrNull { File(it).canExecute() }
    }

    /** clj-kondo-fix on the path; on macOS this is the login shell's path, which apps from the dock lack. */
    private fun onPath(): String? {
        val path = EnvironmentUtil.getValue("PATH") ?: System.getenv("PATH") ?: return null
        return path.split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
    }

    /** The binary shipped in the plugin's bin/<os>-<arch> folder for this machine. */
    private fun bundled(): String? {
        // the plugin's classes are in <plugin>/lib/<jar>, and the binaries in <plugin>/bin
        val jar = PathManager.getJarPathForClass(CljKondoFixExecutable::class.java) ?: return null
        val pluginDir = File(jar).parentFile?.parentFile ?: return null
        val os = when {
            SystemInfo.isMac -> "darwin"
            SystemInfo.isLinux -> "linux"
            SystemInfo.isWindows -> "windows"
            else -> return null
        }
        val arch = if (CpuArch.isArm64()) "arm64" else "amd64"
        val file = File(pluginDir, "bin/$os-$arch/$name")
        if (!file.isFile) {
            return null
        }
        // unpacking a plugin zip can drop the executable bit
        if (!file.canExecute()) {
            file.setExecutable(true)
        }
        return file.absolutePath
    }
}
