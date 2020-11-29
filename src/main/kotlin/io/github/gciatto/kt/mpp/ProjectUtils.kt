package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import java.io.File

object ProjectUtils {
    private val FULL_VERSION_REGEX = "^[0-9]+\\.[0-9]+\\.[0-9]+$".toRegex()

    val Project.isFullVersion: Boolean
        get() = version.toString().matches(FULL_VERSION_REGEX)

    fun log(message: String) {
        println("LOG: $message")
    }

    fun warn(message: String) {
        System.err.println("WARNING: $message")
    }

    fun Project.getPropertyOrWarnForAbsence(key: String): String? {
        val value = property(key)?.toString()
        if (value.isNullOrBlank()) {
            warn("$key is not set")
        }
        return value
    }

    val Project.docDir: File
        get() = buildDir.resolve("doc")
}
