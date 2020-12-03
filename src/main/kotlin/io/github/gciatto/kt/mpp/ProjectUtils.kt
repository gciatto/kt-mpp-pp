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

    private val alreadyWarned: MutableSet<String> = hashSetOf()

    fun Project.getPropertyOrWarnForAbsence(key: String): String? {
        val value = findProperty(key)?.toString()
        if (value.isNullOrBlank() && key !in alreadyWarned) {
            warn("$key is not set")
            alreadyWarned += key
        }
        return value
    }

    fun Project.getPropertyOrDefault(key: String, default: Any): String {
        val value = findProperty(key)?.toString()
        if (value.isNullOrBlank()) {
            return default.toString()
        }
        return value
    }

    val Project.docDir: File
        get() = buildDir.resolve("doc")
}
