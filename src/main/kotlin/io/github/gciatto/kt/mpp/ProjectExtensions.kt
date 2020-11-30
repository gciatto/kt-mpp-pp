package io.github.gciatto.kt.mpp

import org.gradle.api.Project

object ProjectExtensions {

    internal val Project.ktMpp get() = extensions.getByType(KtMppPlusPlusExtension::class.java)

    val Project.isJvmProject: Boolean
        get() = name in ktMpp.jvmProjects.getOrElse(emptySet())

    val Project.isJsProject: Boolean
        get() = name in ktMpp.jsProjects.getOrElse(emptySet())

    val Project.isOtherProject: Boolean
        get() = name in ktMpp.otherProjects.getOrElse(emptySet())

    val Project.isKtProject: Boolean
        get() = !isJvmProject && !isJsProject && !isOtherProject

    val Project.jvmProjects: Sequence<Project>
        get() {
            if (this != rootProject) error("jvmProjects property is only defined for the root project")
            val jvmProjectNames = ktMpp.jvmProjects.getOrElse(emptySet())
            return allprojects.asSequence().filter { it.name in jvmProjectNames }
        }

    val Project.jsProjects: Sequence<Project>
        get() {
            if (this != rootProject) error("jsProjects property is only defined for the root project")
            val jsProjectNames = ktMpp.jsProjects.getOrElse(emptySet())
            return allprojects.asSequence().filter { it.name in jsProjectNames }
        }

    val Project.otherProjects: Sequence<Project>
        get() {
            if (this != rootProject) error("otherProjects property is only defined for the root project")
            val otherProjectNames = ktMpp.otherProjects.getOrElse(emptySet())
            return allprojects.asSequence().filter { it.name in otherProjectNames }
        }

    val Project.ktProjects: Sequence<Project>
        get() {
            if (this != rootProject) error("ktProjects property is only defined for the root project")
            val nonKtProjectsName = setOf(ktMpp.jvmProjects, ktMpp.jsProjects, ktMpp.otherProjects)
                    .flatMap { it.getOrElse(emptySet()) }
                    .toSet()
            return allprojects.asSequence().filter { it.name !in nonKtProjectsName }
        }

    val Project.isMultiProject: Boolean
        get() = (jvmProjects + jsProjects).any()
}
