package io.github.gciatto.kt.mpp

import org.gradle.api.DomainObjectSet
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class KtMppPlusPlusExtension(objects: ObjectFactory) {

    companion object {
        const val NAME = "kotlinMultiplatform"

        object Defaults {
            val JAVA_VERSION = JavaVersion.VERSION_1_8
            const val MOCHA_TIMEOUT = 60_000L // ms
            const val KT_FREE_COMPILER_ARGS_JVM = "-Xjvm-default=enable"
            const val AUTOMATICALLY_CONFIGURE_SUBPROJECTS = true
            const val PREVENT_PUBLISHING_OF_ROOT_PROJECT = false
            const val MAVEN_REPO = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
        }
    }

    val preventPublishingOfRootProject: Property<Boolean> = objects.property()

    val automaticallyConfigureSubprojects: Property<Boolean> = objects.property()

    val projectLongName: Property<String> = objects.property()

    val projectDescription: Property<String> = objects.property()

    val githubToken: Property<String> = objects.property()

    val githubOwner: Property<String> = objects.property()

    val githubRepo: Property<String> = objects.property()

    val bintrayUser: Property<String> = objects.property()

    val bintrayKey: Property<String> = objects.property()

    val bintrayRepo: Property<String> = objects.property()

    val bintrayUserOrg: Property<String> = objects.property()

    val projectHomepage: Property<String> = objects.property()

    val projectLicense: Property<String> = objects.property()

    val projectLicenseUrl: Property<String> = objects.property()

    val mochaTimeout: Property<Long> = objects.property()

    val javaVersion: Property<JavaVersion> = objects.property()

    val ktFreeCompilerArgsJvm: Property<String> = objects.property()

    val developers: DomainObjectSet<Developer> = objects.domainObjectSet(Developer::class.java)

    val mavenRepo: Property<String> = objects.property()

    val mavenUsername: Property<String> = objects.property()

    val mavenPassword: Property<String> = objects.property()

    val scmUrl: Property<String> = objects.property()

    val scmConnection: Property<String> = objects.property()

    val signingKey: Property<String> = objects.property()

    val signingPassword: Property<String> = objects.property()

    val npmToken: Property<String> = objects.property()

    val npmOrganization: Property<String> = objects.property()

    val issuesUrl: Property<String> = objects.property()

    val issuesEmail: Property<String> = objects.property()

    val ktProjects: DomainObjectSet<String> = objects.domainObjectSet(String::class.java)

    val jsProjects: DomainObjectSet<String> = objects.domainObjectSet(String::class.java)

    val jvmProjects: DomainObjectSet<String> = objects.domainObjectSet(String::class.java)

    val otherProjects: DomainObjectSet<String> = objects.domainObjectSet(String::class.java)

    @JvmOverloads
    fun developer(name: String, email: String, homepage: String? = null, organization: Organization? = null) =
            developers.add(Developer(name, email, homepage, organization))

    fun org(name: String, url: String) =
            Organization(name, url)

    fun ktProjects(vararg names: String) =
            ktProjects.addAll(listOf(*names))

    fun jvmOnlyProjects(vararg names: String) =
            jvmProjects.addAll(listOf(*names))

    fun jsOnlyProjects(vararg names: String) =
            jsProjects.addAll(listOf(*names))

    fun otherProjects(vararg names: String) =
            otherProjects.addAll(listOf(*names))

    fun ktProjects(vararg projects: Project) =
            ktProjects.addAll(listOf(*projects).map { it.name })

    fun jvmOnlyProjects(vararg projects: Project) =
            jvmProjects.addAll(listOf(*projects).map { it.name })

    fun jsOnlyProjects(vararg projects: Project) =
            jsProjects.addAll(listOf(*projects).map { it.name })

    fun otherProjects(vararg projects: Project) =
            otherProjects.addAll(listOf(*projects).map { it.name })

    fun ktProjects(projects: Iterable<Project>) =
            ktProjects.addAll(projects.map { it.name })

    fun jvmOnlyProjects(projects: Iterable<Project>) =
            jvmProjects.addAll(projects.map { it.name })

    fun jsOnlyProjects(projects: Iterable<Project>) =
            jsProjects.addAll(projects.map { it.name })

    fun otherProjects(projects: Iterable<Project>) =
            otherProjects.addAll(projects.map { it.name })

    val Project.allOtherSubprojects: List<Project>
        get() {
            val nonKtProjects = setOf(jvmProjects, jsProjects, otherProjects).flatMap { it.toSet() }
            return subprojects.filter { it.name !in nonKtProjects }.toList()
        }

    init {
        javaVersion.set(Defaults.JAVA_VERSION)
        mochaTimeout.set(Defaults.MOCHA_TIMEOUT)
        ktFreeCompilerArgsJvm.set(Defaults.KT_FREE_COMPILER_ARGS_JVM)
        automaticallyConfigureSubprojects.set(Defaults.AUTOMATICALLY_CONFIGURE_SUBPROJECTS)
        mavenRepo.set(Defaults.MAVEN_REPO)
        preventPublishingOfRootProject.set(Defaults.PREVENT_PUBLISHING_OF_ROOT_PROJECT)
    }

    fun copyPropertyValuesFrom(other: KtMppPlusPlusExtension) {
        preventPublishingOfRootProject.set(other.preventPublishingOfRootProject)
        automaticallyConfigureSubprojects.set(other.automaticallyConfigureSubprojects)
        projectLongName.set(other.projectLongName)
        projectDescription.set(other.projectDescription)
        githubToken.set(other.githubToken)
        githubOwner.set(other.githubOwner)
        githubRepo.set(other.githubRepo)
        bintrayUser.set(other.bintrayUser)
        bintrayKey.set(other.bintrayKey)
        bintrayRepo.set(other.bintrayRepo)
        bintrayUserOrg.set(other.bintrayUserOrg)
        projectHomepage.set(other.projectHomepage)
        projectLicense.set(other.projectLicense)
        projectLicenseUrl.set(other.projectLicenseUrl)
        mochaTimeout.set(other.mochaTimeout)
        javaVersion.set(other.javaVersion)
        ktFreeCompilerArgsJvm.set(other.ktFreeCompilerArgsJvm)
        developers.addAll(other.developers)
        mavenRepo.set(other.mavenRepo)
        mavenUsername.set(other.mavenUsername)
        mavenPassword.set(other.mavenPassword)
        scmUrl.set(other.scmUrl)
        scmConnection.set(other.scmConnection)
        signingKey.set(other.signingKey)
        signingPassword.set(other.signingPassword)
        npmToken.set(other.npmToken)
        npmOrganization.set(other.npmOrganization)
        issuesUrl.set(other.issuesUrl)
        issuesEmail.set(other.issuesEmail)
        ktProjects.addAll(other.ktProjects)
        jsProjects.addAll(other.jsProjects)
        jvmProjects.addAll(other.jvmProjects)
        otherProjects.addAll(other.otherProjects)
    }
}
