package io.github.gciatto.kt.mpp

import com.github.breadmoirai.githubreleaseplugin.GithubReleasePlugin
import com.jfrog.bintray.gradle.BintrayPlugin
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.AUTOMATICALLY_CONFIGURE_SUBPROJECTS
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.KT_FREE_COMPILER_ARGS_JVM
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.MAVEN_REPO
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.MOCHA_TIMEOUT
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureDokka
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureDokkaMultiModule
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureGitHubReleaseForRootProject
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureKtLint
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureMavenPublications
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureNpmPublishing
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureSigning
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureTestResultPrinting
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToBintray
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToGithub
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToMavenCentral
import io.github.gciatto.kt.mpp.ProjectConfiguration.createMavenPublications
import io.github.gciatto.kt.mpp.ProjectExtensions.isRootProject
import io.github.gciatto.kt.mpp.ProjectExtensions.ktMpp
import io.github.gciatto.kt.mpp.ProjectUtils.getPropertyOrDefault
import io.github.gciatto.kt.mpp.ProjectUtils.getPropertyOrWarnForAbsence
import io.github.gciatto.kt.mpp.ProjectUtils.log
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class KtMppPlusPlusPlugin : Plugin<Project> {

    private lateinit var extension: KtMppPlusPlusExtension

    override fun apply(target: Project) {
        extension = target.extensions.create(
            KtMppPlusPlusExtension.NAME,
            KtMppPlusPlusExtension::class.java,
            target.objects
        )
        if (target.isRootProject) {
            target.loadDefaultsFromProperties()
            target.configureProject(ProjectType.KT)
            target.configureSubprojects()
        } else {
            extension.copyPropertyValuesFromParentOf(target)
            target.loadDefaultsFromProperties()
        }
    }

    private fun Project.configureSubprojects() {
        if (extension.automaticallyConfigureSubprojects.getOrElse(AUTOMATICALLY_CONFIGURE_SUBPROJECTS)) {
            configureSubprojectsOfType(ProjectType.KT) { ktProjects }
            configureSubprojectsOfType(ProjectType.JVM) { jvmProjects }
            configureSubprojectsOfType(ProjectType.JS) { jsProjects }
            configureSubprojectsOfType(ProjectType.OTHER) { otherProjects }
        }
    }

    private fun Project.configureSubprojectsOfType(
        projectType: ProjectType,
        projectSet: KtMppPlusPlusExtension.() -> DomainObjectSet<String>
    ) {
        extension.projectSet().configureEach { subprojectName ->
            subprojects.find { it.name == subprojectName }?.let {
                it.apply<KtMppPlusPlusPlugin>()
                it.configureProject(projectType)
            }
        }
    }

    private fun Project.loadDefaultsFromProperties() {
        with(extension) {
            projectLongName.set(
                provider {
                    name.split('-').joinToString(" ") { it.capitalize() }
                }
            )
            getPropertyOrWarnForAbsence("projectDescription").let { desc ->
                projectDescription.set(provider { description.takeIf { it.isNullOrBlank() } ?: desc })
            }
            githubToken.set(getPropertyOrWarnForAbsence("githubToken"))
            githubOwner.set(getPropertyOrWarnForAbsence("githubOwner"))
            githubRepo.set(getPropertyOrWarnForAbsence("githubRepo"))
            bintrayUser.set(getPropertyOrWarnForAbsence("bintrayUser"))
            bintrayKey.set(getPropertyOrWarnForAbsence("bintrayKey"))
            bintrayRepo.set(getPropertyOrWarnForAbsence("bintrayRepo"))
            bintrayUserOrg.set(getPropertyOrWarnForAbsence("bintrayUserOrg"))
            projectHomepage.set(getPropertyOrWarnForAbsence("projectHomepage"))
            projectLicense.set(getPropertyOrWarnForAbsence("projectLicense"))
            projectLicenseUrl.set(getPropertyOrWarnForAbsence("projectLicenseUrl"))
            mavenRepo.set(getPropertyOrWarnForAbsence("mavenRepo"))
            mavenUsername.set(getPropertyOrWarnForAbsence("mavenUsername"))
            mavenPassword.set(getPropertyOrWarnForAbsence("mavenPassword"))
            scmUrl.set(getPropertyOrWarnForAbsence("scmUrl"))
            scmConnection.set(getPropertyOrWarnForAbsence("scmConnection"))
            signingKey.set(getPropertyOrWarnForAbsence("signingKey"))
            signingPassword.set(getPropertyOrWarnForAbsence("signingPassword"))
            npmToken.set(getPropertyOrWarnForAbsence("npmToken"))
            findProperty("npmOrganization")?.let { npmOrganization.set(it.toString()) }
            issuesUrl.set(getPropertyOrWarnForAbsence("issuesUrl"))
            issuesEmail.set(getPropertyOrWarnForAbsence("issuesEmail"))

            mochaTimeout.set(getPropertyOrDefault("mochaTimeout", MOCHA_TIMEOUT).toLong())
            ktFreeCompilerArgsJvm.set(getPropertyOrDefault("ktFreeCompilerArgsJvm", KT_FREE_COMPILER_ARGS_JVM))
            mavenRepo.set(getPropertyOrDefault("mavenRepo", MAVEN_REPO))
        }
    }

    private fun Project.configureProject(projectType: ProjectType) {
        when (this) {
            rootProject -> {
                require(projectType == ProjectType.KT) {
                    throw IllegalStateException("Root project must be configured as Kt project")
                }
                configureRootProject()
            }
            else -> when (projectType) {
                ProjectType.JVM -> configureJvmProject()
                ProjectType.JS -> configureJsProject()
                ProjectType.OTHER -> configureOtherProject()
                ProjectType.KT -> configureKtProject()
            }
        }
    }

    private fun Project.configureAllProjects() {
        description = extension.projectDescription.get()
        repositories.add(rootProject.repositories.gradlePluginPortal())
        repositories.add(rootProject.repositories.mavenCentral())
        configureTestResultPrinting()
    }

    private fun KotlinMultiplatformExtension.configureMppCommonSourceSets() {
        sourceSets.getByName("commonMain") {
            it.dependencies {
                api(kotlin("stdlib-common"))
            }
        }
        sourceSets.getByName("commonTest") {
            it.dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }

    private fun KotlinTarget.configureMppTarget() {
        mavenPublication {
            require(it.name in setOf("jvm", "js"))
            it.artifactId = "${project.name}-${it.name}"
        }
    }

    private fun KotlinJvmTarget.configureMppJvmSourceSets() {
        configureMppTarget()
        compilations["main"].defaultSourceSet {
            dependencies {
                api(kotlin("stdlib-jdk8"))
            }
        }
        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }

    private fun Project.configureKtJvmCompilation() {
        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = extension.javaVersion.get().toString()
                freeCompilerArgs = extension.ktFreeCompilerArgsJvm.get().split(';').toList()
            }
        }
    }

    private fun KotlinJvmTarget.configureJava() {
        withJava()
    }

    private fun KotlinJsTargetDsl.configureNodeJsTests() {
        nodejs {
            testTask {
                useMocha {
                    timeout = extension.mochaTimeout.get().toString()
                }
            }
        }
    }

    private fun KotlinJsTargetDsl.configureJsSourceSets() {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }

    private fun Project.configureKtJsCompilation() {
        tasks.withType<KotlinJsCompile> {
            kotlinOptions {
                moduleKind = "umd"
                // noStdlib = true
                metaInfo = true
                sourceMap = true
                sourceMapEmbedSources = "always"
            }
        }
    }

    private fun Project.configureMultiplatform() {
        configure<KotlinMultiplatformExtension> {
            configureMppCommonSourceSets()
            jvm {
                configureJava()
                configureMppTarget()
                configureKtJvmCompilation()
                configureMppJvmSourceSets()
            }
            js {
                configureNodeJsVersion()
                configureNodeJsTests()
                configureMppTarget()
                configureKtJsCompilation()
                configureJsSourceSets()
            }
        }
    }

    private fun Project.configureNodeJsVersion() {
        plugins.withType(NodeJsRootPlugin::class.java) {
            configure<NodeJsRootExtension> {
                ktMpp.nodeJsVersion.takeIf { it.isPresent }?.let {
                    nodeVersion = it.get()
                }
            }
        }
    }

    private fun Project.configureJvm() {
        configure<JavaPluginExtension> {
            sourceCompatibility = extension.javaVersion.get()
            targetCompatibility = extension.javaVersion.get()
        }
        configure<KotlinJvmProjectExtension> {
            dependencies {
                add("api", kotlin("stdlib-jdk8"))
                add("testImplementation", kotlin("test-junit"))
            }
        }
        configureKtJvmCompilation()
        tasks.create("jvmTest") { it.dependsOn("test") }
    }

    private fun Project.configureJvmProject() {
        log("Auto-configure project `$name` as JVM project")
        configureAllProjects()
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply<JavaLibraryPlugin>()
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()
        configureJvm()
        configureKtLint()
        configureDokka()
        createMavenPublications("jvm", "java", docArtifact = "packDokka")
        configureUploadToMavenCentral()
        configureUploadToBintray()
        configureSigning()
        configureUploadToGithub({ "shadow" in it })
    }

    private fun Project.configureKtProject() {
        log("Auto-configure project `$name` as Kt project")
        configureAllProjects()
        apply(plugin = "org.jetbrains.kotlin.multiplatform")
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()
        configureMultiplatform()
        configureKtLint()
        configureDokka("jvm", "js")
        configureMavenPublications("packDokka")
        configureUploadToMavenCentral()
//        configureUploadToBintray("kotlinMultiplatform", "js", "jvm", "metadata")
        configureUploadToBintray()
        configureSigning()
        configureNpmPublishing()
        configureUploadToGithub({ "shadow" in it })
    }

    private fun Project.configureJsProject() {
        log("Auto-configure project `$name` as JS project")
        configureAllProjects()
        apply(plugin = "org.jetbrains.kotlin.js")
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()

        configureJs()
        configureKtLint()
        configureDokka()
        createMavenPublications("js", "kotlin", docArtifact = "packDokka")
        configureUploadToMavenCentral()
        configureUploadToBintray()
        configureSigning()
        configureNpmPublishing()
    }

    private fun Project.configureJs() {
        configure<KotlinJsProjectExtension> {
            js {
                configureNodeJsVersion()
                configureNodeJsTests()
                configureKtJsCompilation()
                configureJsSourceSets()
            }
            tasks.maybeCreate("sourcesJar", Jar::class.java).run {
                sourceSets.forEach { sourceSet ->
                    sourceSet.kotlin.srcDirs.forEach {
                        from(it)
                    }
                }
            }
        }
        tasks.create("jsTest") { it.dependsOn("test") }
    }

    private fun Project.configureOtherProject() {
        log("Auto-configure project `$name` as other project")
        configureAllProjects()
    }

    private fun Project.configureRootProject() {
        apply<GithubReleasePlugin>()
        configureKtProject()
        configureGitHubReleaseForRootProject()
        configureDokkaMultiModule()
    }
}
