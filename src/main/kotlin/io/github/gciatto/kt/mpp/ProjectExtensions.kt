package io.github.gciatto.kt.mpp

import org.gradle.api.Project

object ProjectExtensions {

    internal val Project.ktMpp get() = extensions.getByType(KtMppPlusPlusExtension::class.java)

    internal val Project.isRootProject get() = this == rootProject

    val Project.isMultiProject: Boolean
        get() = subprojects.any()
}
