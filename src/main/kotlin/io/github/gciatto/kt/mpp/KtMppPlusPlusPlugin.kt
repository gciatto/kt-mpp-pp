package io.github.gciatto.kt.mpp

import com.jfrog.bintray.gradle.BintrayPlugin
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.AUTOMATICALLY_CONFIGURE_PROJECTS
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureDokka
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureNpmPublishing
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureKtLint
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureMavenPublications
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureSigning
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureTestResultPrinting
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToBintray
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToGithub
import io.github.gciatto.kt.mpp.ProjectConfiguration.configureUploadToMavenCentral
import io.github.gciatto.kt.mpp.ProjectConfiguration.createMavenPublications
import io.github.gciatto.kt.mpp.ProjectExtensions.isJsProject
import io.github.gciatto.kt.mpp.ProjectExtensions.isJvmProject
import io.github.gciatto.kt.mpp.ProjectExtensions.isOtherProject
import io.github.gciatto.kt.mpp.ProjectUtils.getPropertyOrWarnForAbsence
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.create
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class KtMppPlusPlusPlugin : Plugin<Project> {

    private lateinit var extension: KtMppPlusPlusExtension

    override fun apply(target: Project) {
        extension = target.extensions.create(KtMppPlusPlusExtension.NAME, KtMppPlusPlusExtension::class.java)
        target.loadDefaultsFromProperties()
        if (extension.automaticallyConfigureProjects.getOrElse(AUTOMATICALLY_CONFIGURE_PROJECTS)) {
            target.configureProject()
            target.subprojects {
                it.apply<KtMppPlusPlusPlugin>()
            }
        }
    }

    private fun Project.loadDefaultsFromProperties() {
        with(extension) {
            projectLongName.set(project.description)
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
            npmOrganization.set(getPropertyOrWarnForAbsence("npmOrganization"))
            issuesUrl.set(getPropertyOrWarnForAbsence("issuesUrl"))
            issuesEmail.set(getPropertyOrWarnForAbsence("issuesEmail"))
        }
    }

    private fun Project.configureProject() {
        when {
            this == rootProject -> configureRootProject()
            isJvmProject -> configureJvmProject()
            isJsProject -> configureJsProject()
            isOtherProject -> configureOtherProject()
            else -> configureKtProject()
        }
    }

    private fun Project.configureAllProjects() {
        group = rootProject.group
        version = rootProject.version

        repositories.addAll(rootProject.repositories)

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

    private fun KotlinJsTargetDsl.configureNodeJs() {
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
                configureNodeJs()
                configureMppTarget()
                configureKtJsCompilation()
                configureJsSourceSets()
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

    private fun Project.configureKtProject() {
        configureAllProjects()

        apply<KotlinMultiplatformPlugin>()
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

    private fun Project.configureJvmProject() {
        configureAllProjects()

        apply<KotlinPlatformJvmPlugin>()
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

    private fun Project.configureJsProject() {
        configureAllProjects()

        apply<KotlinPlatformJsPlugin>()
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
                configureNodeJs()
                configureKtJsCompilation()
                configureJsSourceSets()
            }
            tasks.getByName<Jar>("sourcesJar") {
                sourceSets.forEach { sourceSet ->
                    sourceSet.kotlin.srcDirs.forEach {
                        from(it)
                    }
                }
            }
        }
        tasks.create("jsTest") { it.dependsOn("test") }
        configure<PublishingExtension> {
            publications.withType<MavenPublication>().getByName("js") {
                it.from(components["kotlin"])
            }
        }
    }

    private fun Project.configureOtherProject() {
        configureAllProjects()
    }

    private fun Project.configureRootProject() {
        configureKtProject()
        val dokkaHtmlMultiModule = tasks.getByName("dokkaHtmlMultiModule") as DokkaMultiModuleTask
        val packDokkaMultiModule = tasks.create<Zip>("packDokkaMultiModule") {
            group = "documentation"
            dependsOn(dokkaHtmlMultiModule)
            from(dokkaHtmlMultiModule.outputDirectory.get())
            archiveBaseName.set(project.name)
            archiveVersion.set(project.version.toString())
            archiveAppendix.set("documentation")
        }
        configureUploadToGithub(packDokkaMultiModule)
    }
}
