package io.github.gciatto.kt.mpp

import org.gradle.api.Project

object ProjectExtensions {

    internal val Project.ktMpp get() = extensions.getByType(KtMppPlusPlusExtension::class.java)

    internal val Project.isRootProject get() = this == rootProject

    val Project.isJvmProject: Boolean
        get() = name in ktMpp.jvmProjects

    val Project.isJsProject: Boolean
        get() = name in ktMpp.jsProjects

    val Project.isOtherProject: Boolean
        get() = name in ktMpp.otherProjects

    val Project.isKtProject: Boolean
        get() = name in ktMpp.ktProjects

    val Project.jvmProjects: Sequence<Project>
        get() {
            val jvmProjectNames = ktMpp.jvmProjects
            return allprojects.asSequence().filter { it.name in jvmProjectNames }
        }

    val Project.jsProjects: Sequence<Project>
        get() {
            val jsProjectNames = ktMpp.jsProjects
            return allprojects.asSequence().filter { it.name in jsProjectNames }
        }

    val Project.otherProjects: Sequence<Project>
        get() {
            val otherProjectNames = ktMpp.otherProjects
            return allprojects.asSequence().filter { it.name in otherProjectNames }
        }

    val Project.ktProjects: Sequence<Project>
        get() {
            val ktProjectNames = ktMpp.ktProjects
            return allprojects.asSequence().filter { it.name in ktProjectNames }
        }

    val Project.isMultiProject: Boolean
        get() = subprojects.any()
}
