package io.github.gciatto.kt.mpp

import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.AUTOMATICALLY_CONFIGURE_PROJECTS
import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property

open class KtMppPlusPlusExtension(objects: ObjectFactory) {

    companion object {
        const val NAME = "kotlinMultiplatform"

        object Defaults {
            val JAVA_VERSION = JavaVersion.VERSION_1_8
            const val MOCHA_TIMEOUT = 60_000L // ms
            const val KT_FREE_COMPILER_ARGS_JVM = "-Xjvm-default=enable"
            const val AUTOMATICALLY_CONFIGURE_PROJECTS = true
        }
    }

    val automaticallyConfigureProjects: Property<Boolean> = objects.property()

    val projectLongName: Property<String> = objects.property()

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

    val developers: ListProperty<Developer> = objects.listProperty(Developer::class.java)

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

    val jsProjects: SetProperty<String> = objects.setProperty(String::class.java)

    val jvmProjects: SetProperty<String> = objects.setProperty(String::class.java)

    val otherProjects: SetProperty<String> = objects.setProperty(String::class.java)

    @JvmOverloads
    fun developer(name: String, email: String, homepage: String? = null, organization: Organization? = null) =
            developers.add(Developer(name, email, homepage, organization))

    fun org(name: String, url: String) =
            Organization(name, url)

    init {
        javaVersion.set(Defaults.JAVA_VERSION)
        mochaTimeout.set(Defaults.MOCHA_TIMEOUT)
        ktFreeCompilerArgsJvm.set(Defaults.KT_FREE_COMPILER_ARGS_JVM)
        automaticallyConfigureProjects.set(AUTOMATICALLY_CONFIGURE_PROJECTS)
    }
}
