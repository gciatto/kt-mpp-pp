@file:Suppress("unused")

package io.github.gciatto.kt.mpp

import com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension
import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import io.github.gciatto.kt.mpp.KtMppPlusPlusExtension.Companion.Defaults.PREVENT_PUBLISHING_OF_ROOT_PROJECT
import io.github.gciatto.kt.mpp.ProjectExtensions.isMultiProject
import io.github.gciatto.kt.mpp.ProjectExtensions.isRootProject
import io.github.gciatto.kt.mpp.ProjectExtensions.ktMpp
import io.github.gciatto.kt.mpp.ProjectUtils.docDir
import io.github.gciatto.kt.mpp.ProjectUtils.warn
import io.github.gciatto.kt.node.Bugs
import io.github.gciatto.kt.node.NpmPublishExtension
import io.github.gciatto.kt.node.NpmPublishPlugin
import io.github.gciatto.kt.node.NpmPublishTask
import io.github.gciatto.kt.node.PackageJson
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import java.io.File

object ProjectConfiguration {

    fun Project.configureKtLint() {
        plugins.apply(KtlintPlugin::class.java)

        project.configure<KtlintExtension> {
            debug.set(false)
            ignoreFailures.set(false)
            enableExperimentalRules.set(true)
            filter {
                it.exclude("**/generated/**")
                it.include("**/kotlin/**")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun Project.configureGitHubReleaseForRootProject() {
        if (isRootProject) {
            afterEvaluate {
                configure<GithubReleaseExtension> {
                    if (ktMpp.githubToken.isPresent && ktMpp.githubToken.get().isNotBlank()) {
                        token(ktMpp.githubToken.get())
                        owner(ktMpp.githubOwner.get())
                        repo(ktMpp.githubRepo.get())
                        tagName { version.toString() }
                        releaseName { version.toString() }
                        allowUploadToExisting { true }
                        prerelease { false }
                        draft { false }
                        try {
                            body(
                                """
                                |## CHANGELOG
                                |${changelog().call()}
                                """.trimMargin()
                            )
                        } catch (e: Throwable) {
                            e.message?.let { warn(it) }
                        }
                    }
                }
            }
        } else {
            throw IllegalStateException("Cannot run configureGitHubReleaseForRootProject on a non-root project")
        }
    }

    fun Project.configureUploadToGithub(vararg tasks: Zip) {
        if (ktMpp.githubToken.isPresent.not() || ktMpp.githubToken.get().isBlank()) return

        val archiveFiles = tasks.map { it.archiveFile }

        rootProject.afterEvaluate {
            it.configure<GithubReleaseExtension> {
                releaseAssets(*(releaseAssets.toList() + archiveFiles).toTypedArray())
            }

            it.tasks.withType(GithubReleaseTask::class.java) { releaseTask ->
                releaseTask.dependsOn(*tasks)
            }
        }
    }

    fun Project.configureUploadToGithub(
        jarTaskPositiveFilter: (String) -> Boolean = { "jar" in it },
        jarTaskNegativeFilter: (String) -> Boolean = { "dokka" in it || "source" in it }
    ) {
        tasks.withType(Zip::class.java).matching {
            val name = it.name.toLowerCase()
            jarTaskPositiveFilter(name) && !jarTaskNegativeFilter(name)
        }.configureEach {
            configureUploadToGithub(it)
        }
    }

    private fun Project.createPackDokkaTask(directory: File, platform: String? = null) {
        val packAllDokka = tasks.maybeCreate("packAllDokka", DefaultTask::class.java).also {
            it.group = "documentation"
        }
        val packDokkaTask = tasks.maybeCreate("packDokka${platform?.capitalize() ?: ""}", Jar::class.java)
        afterEvaluate {
            packDokkaTask.run {
                group = "documentation"
                dependsOn("dokkaHtml")
                from(directory)
                archiveBaseName.set(project.name)
                archiveVersion.set(project.version.toString())
                archiveClassifier.set("javadoc")
                packAllDokka.dependsOn(this)
            }
        }
    }

    fun Project.configureDokka(vararg platforms: String) {
        tasks.withType(DokkaTask::class.java).all { dokkaTask ->
            dokkaTask.outputDirectory.set(docDir)

            dokkaTask.dokkaSourceSets.apply {
                if (platforms.isNotEmpty()) {
                    for (platform in platforms) {
                        named("${platform}Main")
                    }
                }
            }

            if (platforms.isNotEmpty()) {
                configure<KotlinMultiplatformExtension> {
                    targets.all {
                        createPackDokkaTask(dokkaTask.outputDirectory.get(), it.name)
                    }
                }
            } else {
                createPackDokkaTask(dokkaTask.outputDirectory.get())
            }
        }
    }

    fun Project.configureSigning() {
        configure<SigningExtension> {
            if (arrayOf(ktMpp.signingKey, ktMpp.signingPassword).all { it.isPresent && it.get().isNotBlank() }) {
                useInMemoryPgpKeys(ktMpp.signingKey.get(), ktMpp.signingPassword.get())
                configure<PublishingExtension> {
                    sign(publications)
                    tasks.register("signAllPublications", DefaultTask::class.java).configure { signAllPubs ->
                        signAllPubs.group = "sign"
                        publications.withType(MavenPublication::class.java).configureEach {
                            signAllPubs.dependsOn("sign${it.name.capitalize()}Publication")
                        }
                    }
                }
            }
        }
    }

    fun Project.configureUploadToBintray(vararg publicationNames: String) {
        val publishAllToBintrayTask = tasks.maybeCreate("publishAllToBintray").also {
            it.group = "publishing"
        }
        afterEvaluate {
            configure<BintrayExtension> {
                user = ktMpp.bintrayUser.get()
                key = ktMpp.bintrayKey.get()
                if (publicationNames.isEmpty()) {
                    configure<PublishingExtension> {
                        setPublications(*publications.withType(MavenPublication::class.java).map { it.name }
                            .toTypedArray())
                    }
                } else {
                    setPublications(*publicationNames)
                }
                override = true
                with(pkg) {
                    repo = ktMpp.bintrayRepo.get() // bintrayRepo
                    name = project.name
                    userOrg = ktMpp.bintrayUserOrg.get()
                    vcsUrl = ktMpp.projectHomepage.get()
                    setLicenses(ktMpp.projectLicense.get())
                    with(version) {
                        name = project.version.toString()
                    }
                }
            }
        }
        tasks.withType(BintrayUploadTask::class.java) {
            publishAllToBintrayTask.dependsOn(it)
        }
    }

    fun Project.configureUploadToMavenCentral() {
        if (arrayOf(ktMpp.mavenUsername, ktMpp.mavenPassword).all { it.isPresent && it.get().isNotBlank() }) {
            configure<PublishingExtension> {
                repositories { repos ->
                    repos.maven(ktMpp.mavenRepo.get()) {
                        credentials {
                            it.username = ktMpp.mavenUsername.get()
                            it.password = ktMpp.mavenPassword.get()
                        }
                    }
                }
            }
        }
    }

    fun Project.createMavenPublications(name: String, component: String? = null, docArtifact: String? = null) {
        val sourcesJar = tasks.maybeCreate("sourcesJar", Jar::class.java)
        afterEvaluate {
            sourcesJar.run {
                archiveBaseName.set(project.name)
                archiveVersion.set(project.version.toString())
                archiveClassifier.set("sources")
            }
        }

        configure<PublishingExtension> {
            publications.maybeCreate(name, MavenPublication::class.java).run {
                val pubName = this.name
                if (component != null) {
                    if (component in components.names) {
                        from(components[component])
                    } else {
                        warn("Missing component $component in ${project.name} for publication $pubName")
                    }
                }
                if (docArtifact != null && docArtifact in tasks.names) {
                    artifact(tasks.getByName(docArtifact)) {
                        it.classifier = "javadoc"
                    }
                }
                artifact(sourcesJar)
                configurePom(project)
            }
        }
    }

    fun Project.configureDokkaMultiModule() {
        afterEvaluate {
            tasks.withType(DokkaMultiModuleTask::class.java)
                .matching { it.name.contains("html", ignoreCase = true) }
                .all { dokkaHtmlMultiModule ->
                    val packDokkaMultiModule = tasks.maybeCreate("packDokkaMultiModule", Zip::class.java).also {
                        it.group = "documentation"
                        it.dependsOn(dokkaHtmlMultiModule)
                        it.from(dokkaHtmlMultiModule.outputDirectory.get())
                        it.archiveBaseName.set(project.name)
                        it.archiveVersion.set(project.version.toString())
                        it.archiveAppendix.set("documentation")
                    }
                    configureUploadToGithub(packDokkaMultiModule)
                }
        }
    }

    fun Project.configureMavenPublications(docArtifactBaseName: String) {
        configure<PublishingExtension> {
            publications.withType(MavenPublication::class.java).configureEach { pub ->
                val docArtifact = "${docArtifactBaseName}${pub.name.capitalize()}"
                if (docArtifact in tasks.names) {
                    pub.artifact(tasks.getByName(docArtifact)) {
                        it.classifier = "javadoc"
                    }
                }
                pub.configurePom(project)
            }
        }
    }

    fun MavenPublication.configurePom(project: Project) {
        project.afterEvaluate {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            pom { pom ->
                val moduleName = project.name.split('-').joinToString(" ") { it.capitalize() }
                val pomName = project.ktMpp.projectLongName.get() + if (project.isMultiProject) {
                    " -- Module `$moduleName`"
                } else {
                    ""
                }
                pom.name.set(pomName)
                pom.description.set(project.ktMpp.projectDescription.get())
                pom.url.set(project.ktMpp.projectHomepage.get())
                pom.licenses { licenses ->
                    licenses.license {
                        it.name.set(project.ktMpp.projectLicense.get())
                        it.url.set(project.ktMpp.projectLicenseUrl.get())
                    }
                }

                pom.developers { developers ->
                    project.ktMpp.developers.all { developer ->
                        developers.developer { dev ->
                            dev.name.set(developer.name)
                            dev.email.set(developer.email)
                            developer.homepage?.let { dev.url.set(it) }
                            developer.organization?.let {
                                dev.organization.set(it.name)
                                dev.organizationUrl.set(it.url)
                            }
                        }
                    }
                }

                pom.scm { scm ->
                    scm.connection.set(project.ktMpp.scmConnection)
                    scm.url.set(project.ktMpp.scmUrl)
                }

                pom.issueManagement { issueManagement ->
                    issueManagement.url.set(project.ktMpp.issuesUrl.get())
                }
            }
        }
    }

    fun Project.configureNpmPublishing() {
        apply<NpmPublishPlugin>()
        afterEvaluate {
            configure<NpmPublishExtension> {
                token.set(ktMpp.npmToken.get())
                liftPackageJson {
                    it.people = ktMpp.developers.map(Developer::toNpmPeople).toMutableList()
                    // TODO support description
                    it.homepage = ktMpp.projectHomepage.get()
                    it.bugs = Bugs(ktMpp.issuesUrl.get(), ktMpp.issuesEmail.get())
                    it.license = ktMpp.projectLicense.get()
                    it.version = it.version?.substringBefore('+')
                    liftPackageJsonToFixDependencies(it)
                    if (ktMpp.npmOrganization.isPresent) {
                        liftPackageJsonToSetOrganization(ktMpp.npmOrganization.get(), it)
                    }
                }
                if (ktMpp.npmOrganization.isPresent) {
                    liftJsSources { _, _, line ->
                        liftJsSourcesToSetOrganization(ktMpp.npmOrganization.get(), line)
                    }
                }
            }
            if (isRootProject) {
                tasks.withType(NpmPublishTask::class.java).configureEach {
                    it.onlyIf {
                        !ktMpp.preventPublishingOfRootProject.getOrElse(PREVENT_PUBLISHING_OF_ROOT_PROJECT)
                    }
                }
            }
        }
    }

    fun Project.liftPackageJsonToFixDependencies(packageJson: PackageJson) {
        packageJson.dependencies = packageJson.dependencies?.filterKeys { key -> "kotlin-test" !in key }
            ?.mapValues { (key, value) ->
                val temp = if (value.startsWith("file:")) {
                    value.split('/', '\\').last()
                } else {
                    value
                }
                if (rootProject.name in key) temp.substringBefore('+') else temp
            }?.toMutableMap()
    }

    fun Project.liftPackageJsonToSetOrganization(organizationName: String, packageJson: PackageJson) {
        packageJson.name = "@$organizationName/${packageJson.name}"
        packageJson.dependencies = packageJson.dependencies
            ?.mapKeys { (key, _) ->
                if (rootProject.name in key) "@$organizationName/$key" else key
            }?.toMutableMap()
    }

    fun Project.liftJsSourcesToSetOrganization(organizationName: String, line: String): String =
        line.replace("'${rootProject.name}", "'@$organizationName/${rootProject.name}")
            .replace("\"${rootProject.name}", "\"@$organizationName/${rootProject.name}")

    fun Project.configureTestResultPrinting() {
        tasks.withType(AbstractTestTask::class.java) {
            it.afterSuite(
                KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
                    if (desc.parent == null) { // will match the outermost suite
                        println(
                            "Results: ${result.resultType} (${result.testCount} tests, " +
                                    "${result.successfulTestCount} successes, ${result.failedTestCount} failures, " +
                                    "${result.skippedTestCount} skipped)"
                        )
                    }
                })
            )
        }
    }
}
