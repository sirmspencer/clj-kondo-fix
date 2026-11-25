package io.github.sirmspencer.cljkondofix

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties

/** Application-wide settings, stored in clj-kondo-fix.xml in the IDE config directory. */
@Service(Service.Level.APP)
@State(name = "CljKondoFixSettings", storages = [Storage("clj-kondo-fix.xml")])
class CljKondoFixSettings : SimplePersistentStateComponent<CljKondoFixSettings.Options>(Options()) {

    class Options : BaseState() {
        // empty means download the release binary that matches the plugin version
        var path by string("")
    }

    companion object {
        fun getInstance(): CljKondoFixSettings = service()
    }
}

/**
 * Finds the clj-kondo-fix executable. The plugin does not bundle binaries; it
 * downloads the one for this platform from the GitHub release that matches the
 * plugin version, checks it against the release's checksums.txt, and caches it
 * in the IDE's system directory.
 */
object CljKondoFixExecutable {
    private const val RELEASES = "https://github.com/sirmspencer/clj-kondo-fix/releases/download"

    private val name = if (SystemInfo.isWindows) "clj-kondo-fix.exe" else "clj-kondo-fix"

    // apps started from the dock may not see the shell path, so check homebrew directly
    private val homebrewPaths = listOf("/opt/homebrew/bin/clj-kondo-fix", "/usr/local/bin/clj-kondo-fix")

    /**
     * The configured path, then the cached or downloaded release binary, then
     * clj-kondo-fix on the path or in homebrew when the download fails, for
     * example offline. Runs off the ui thread, since it may download.
     */
    fun find(indicator: ProgressIndicator): String {
        val configured = CljKondoFixSettings.getInstance().state.path?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            return configured
        }
        try {
            return downloaded(indicator)
        } catch (error: IOException) {
            return installed()
                ?: throw IOException("could not download clj-kondo-fix (${error.message}). Install it or set its path in Settings | Tools | clj-kondo-fix.")
        }
    }

    private fun installed(): String? {
        val path = EnvironmentUtil.getValue("PATH") ?: System.getenv("PATH")
        val onPath = path?.split(File.pathSeparator)
            ?.filter { it.isNotBlank() }
            ?.map { File(it, name) }
            ?.firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
        return onPath ?: homebrewPaths.firstOrNull { File(it).canExecute() }
    }

    /** The release archive name for this machine, such as darwin-arm64. */
    private fun target(): String {
        val os = when {
            SystemInfo.isMac -> "darwin"
            SystemInfo.isLinux -> "linux"
            SystemInfo.isWindows -> "windows"
            else -> throw IOException("no clj-kondo-fix release for ${SystemInfo.OS_NAME}")
        }
        val arch = if (CpuArch.isArm64()) "arm64" else "amd64"
        return "$os-$arch"
    }

    // written by the gradle build, which avoids the platform's internal plugin lookup
    private fun pluginVersion(): String =
        CljKondoFixExecutable::class.java.getResourceAsStream("version.properties")
            ?.use { Properties().apply { load(it) }.getProperty("version") }
            ?: throw IOException("could not read the plugin version")

    @Synchronized
    private fun downloaded(indicator: ProgressIndicator): String {
        val version = pluginVersion()
        val target = target()
        val cached = Path.of(PathManager.getSystemPath(), "clj-kondo-fix", version, target, name)
        if (Files.isExecutable(cached)) {
            return cached.toString()
        }

        val archive = "clj-kondo-fix-$target" + if (SystemInfo.isWindows) ".zip" else ".tar.gz"
        val base = "$RELEASES/v$version"
        indicator.text = "Downloading clj-kondo-fix $version"
        val checksums = HttpRequests.request("$base/checksums.txt").productNameAsUserAgent().readString(indicator)
        val expected = checksums.lineSequence()
            .map { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.size == 2 && it[1] == archive }
            ?.get(0)
            ?: throw IOException("no checksum for $archive in release v$version")

        val work = Files.createTempDirectory("clj-kondo-fix")
        try {
            val file = work.resolve(archive)
            HttpRequests.request("$base/$archive").productNameAsUserAgent().saveToFile(file, indicator)
            if (sha256(file) != expected) {
                throw IOException("checksum mismatch for $archive")
            }
            val extracted = work.resolve("extracted")
            if (SystemInfo.isWindows) Decompressor.Zip(file).extract(extracted) else Decompressor.Tar(file).extract(extracted)
            val binary = extracted.resolve(name)
            if (!Files.isRegularFile(binary)) {
                throw IOException("$archive does not contain $name")
            }
            binary.toFile().setExecutable(true)
            Files.createDirectories(cached.parent)
            Files.move(binary, cached, StandardCopyOption.REPLACE_EXISTING)
            return cached.toString()
        } finally {
            deleteRecursively(work)
        }
    }

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }

    private fun sha256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
