package io.github.gciatto.kt.mpp

import com.jfrog.bintray.gradle.BintrayPlugin
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.AUTOMATICALLY_CONFIGURE_PROJECTS
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.create
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

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

    private fun Project.configureAllProject() {
        group = rootProject.group
        version = rootProject.version

        repositories.addAll(rootProject.repositories)

        configureTestResultPrinting()
    }

    private fun Project.configureKtProject() {
        configureAllProject()

        apply<KotlinMultiplatformPlugin>()
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()

        configure<KotlinMultiplatformExtension> {
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

            jvm {
                mavenPublication {
                    it.artifactId = project.name + "-jvm"
                }

                tasks.withType<KotlinJvmCompile> {
                    kotlinOptions {
                        jvmTarget = extension.javaVersion.get().toString()
                        freeCompilerArgs = extension.ktFreeCompilerArgsJvm.get().split(';').toList()
                    }
                }

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

            js {
                nodejs {
                    testTask {
                        useMocha {
                            timeout = extension.mochaTimeout.get().toString()
                        }
                    }
                }

                mavenPublication {
                    it.artifactId = project.name + "-js"
                }

                tasks.withType<KotlinJsCompile> {
                    kotlinOptions {
                        moduleKind = "umd"
                        // noStdlib = true
                        metaInfo = true
                        sourceMap = true
                        sourceMapEmbedSources = "always"
                    }
                }

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
        }

        configureKtLint()
        configureDokka("jvm", "js")
        configureMavenPublications("packDokka")
        configureUploadToMavenCentral()
//        configureUploadToBintray("kotlinMultiplatform", "js", "jvm", "metadata")
        configureUploadToBintray()
        configureSigning()
        configureJsPackage()
        configureUploadToGithub({ "shadow" in it })
    }

    private fun Project.configureJvmProject() {
        configureAllProject()
        apply<MavenPublishPlugin>()
        apply<JavaLibraryPlugin>()
        apply<KotlinPlatformJvmPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()
        configureKtLint()
        configureDokka()
        createMavenPublications("jvm", "java", docArtifact = "packDokka")
        configureUploadToMavenCentral()
        configureUploadToBintray()
        configureSigning()
        configureUploadToGithub({ "shadow" in it })
    }

    private fun Project.configureJsProject() {
        configureAllProject()
        apply<MavenPublishPlugin>()
        apply<KotlinPlatformJsPlugin>()
        apply<SigningPlugin>()
        apply<DokkaPlugin>()
        apply<BintrayPlugin>()

        configureKtLint()
        configureDokka()
        createMavenPublications("js", "kotlin", docArtifact = "packDokka")
        configureUploadToMavenCentral()
        configureUploadToBintray()
        configureSigning()
    }

    private fun Project.configureOtherProject() {
        configureAllProject()
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
