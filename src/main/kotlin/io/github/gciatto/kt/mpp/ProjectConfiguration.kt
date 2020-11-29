@file:Suppress("unused")

package io.github.gciatto.kt.mpp

import com.github.breadmoirai.githubreleaseplugin.GithubReleaseExtension
import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import io.github.gciatto.kt.mpp.ProjectExtensions.ktMpp
import io.github.gciatto.kt.mpp.ProjectUtils.docDir
import io.github.gciatto.kt.mpp.ProjectUtils.warn
import io.github.gciatto.kt.node.Bugs
import io.github.gciatto.kt.node.NpmPublishExtension
import io.github.gciatto.kt.node.PackageJson
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
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
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.KotlinClosure2
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

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

    fun Project.configureGitHubReleaseForRootProject() {
        if (this == rootProject) {
            project.configure<GithubReleaseExtension> {
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
                    } catch (e: Exception) {
                        e.message?.let { warn(it) }
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

        rootProject.configure<GithubReleaseExtension> {
            releaseAssets(*(releaseAssets.toList() + archiveFiles).toTypedArray())
        }

        rootProject.tasks.withType(GithubReleaseTask::class.java) {
            it.dependsOn(*tasks)
        }
    }

    fun Project.configureUploadToGithub(
        jarTaskPositiveFilter: (String) -> Boolean = { "jar" in it },
        jarTaskNegativeFilter: (String) -> Boolean = { "dokka" in it || "source" in it }
    ) {
        val zipTasks = tasks.withType(Zip::class.java).asSequence()
                .filter { jarTaskPositiveFilter(it.name.toLowerCase()) }
                .filter { !jarTaskNegativeFilter(it.name.toLowerCase()) }
                .toList()
                .toTypedArray()

        configureUploadToGithub(*zipTasks)
    }

    private inline fun <reified T : Task> Project.task(name: String, crossinline conf: T.() -> Unit): T =
            tasks.getByName(name) {
                with(it as T, conf)
            } as T

    fun Project.configureDokka(vararg platforms: String) {
        tasks.withType(DokkaTask::class.java).configureEach {
            it.outputDirectory.set(docDir)

            it.dokkaSourceSets.apply {
                if (platforms.isNotEmpty()) {
                    for (p in platforms) {
                        named("${p}Main")
                    }
                }
            }
        }

        val packAllDokka = tasks.create("packAllDokka", DefaultTask::class.java) {
            it.group = "documentation"
        }

        if (platforms.isNotEmpty()) {
            val jarPlatform = tasks.withType(Jar::class.java).map { it.name.replace("Jar", "") }

            jarPlatform.forEach { p ->
                val packDokkaForPlatform = task<Jar>("packDokka${p.capitalize()}") {
                    group = "documentation"
                    dependsOn("dokkaHtml")
                    from(docDir)
                    archiveBaseName.set(project.name)
                    archiveVersion.set(project.version.toString())
                    archiveAppendix.set(p)
                    archiveClassifier.set("javadoc")
                }

                packAllDokka.dependsOn(packDokkaForPlatform)
            }
        } else {
            val packDokka = tasks.create("packDokka", Jar::class.java) {
                it.group = "documentation"
                it.dependsOn("dokkaHtml")
                it.from(docDir)
                it.archiveBaseName.set(project.name)
                it.archiveVersion.set(project.version.toString())
                it.archiveClassifier.set("javadoc")
            }

            packAllDokka.dependsOn(packDokka)
        }
    }

    fun Project.configureSigning() {
        configure<SigningExtension> {
            useInMemoryPgpKeys(ktMpp.signingKey.orNull, ktMpp.signingPassword.orNull)
            configure<PublishingExtension> {
                sign(publications)
            }
        }
        configure<PublishingExtension> {
            val pubs = publications.withType(MavenPublication::class.java).map { "sign${it.name.capitalize()}Publication" }

            project.task<Sign>("signAllPublications") {
                dependsOn(*pubs.toTypedArray())
            }
        }
    }

    fun Project.configureUploadToBintray(vararg publicationNames: String) {
        val publishAllToBintrayTask = tasks.maybeCreate("publishAllToBintray").also {
            it.group = "publishing"
        }
        configure<BintrayExtension> {
            user = ktMpp.bintrayUser.get()
            key = ktMpp.bintrayKey.get()
            if (publicationNames.isEmpty()) {
                configure<PublishingExtension> {
                    setPublications(*publications.withType(MavenPublication::class.java).map { it.name }.toTypedArray())
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
        tasks.withType(BintrayUploadTask::class.java) {
            publishAllToBintrayTask.dependsOn(it)
        }
    }

    fun Project.configureUploadToMavenCentral() {
        if (ktMpp.mavenUsername.isPresent && ktMpp.mavenPassword.isPresent) {
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

    fun Project.createMavenPublications(name: String, vararg componentsStrings: String, docArtifact: String? = null) {
        val sourcesJar = tasks.create("sourcesJar", Jar::class) {
            it.archiveBaseName.set(project.name)
            it.archiveVersion.set(project.version.toString())
            it.archiveClassifier.set("sources")
        }

        configure<PublishingExtension> {
            publications.create<MavenPublication>(name) {
                groupId = project.group.toString()
                version = project.version.toString()

                for (component in componentsStrings) {
                    if (component in components.names) {
                        from(components[component])
                    } else {
                        warn("Missing component $component in ${project.name} for publication $name")
                    }
                }

                if (docArtifact != null && docArtifact in tasks.names) {
                    artifact(tasks.getByName(docArtifact)) {
                        it.classifier = "javadoc"
                    }
                } else if (docArtifact == null || !docArtifact.endsWith("KotlinMultiplatform")) {
                    log(
                            "No javadoc artifact for publication $name in project ${project.name}: " +
                                    "no such a task: $docArtifact"
                    )
                }

                artifact(sourcesJar)

                configurePom(project)
            }
        }
    }

    fun Project.configureMavenPublications(docArtifactBaseName: String) {
        configure<PublishingExtension> {
            publications.withType(MavenPublication::class.java) { pub ->
                pub.groupId = project.group.toString()
                pub.version = project.version.toString()

                val docArtifact = "${docArtifactBaseName}${name.capitalize()}"

                if (docArtifact in tasks.names) {
                    pub.artifact(tasks.getByName(docArtifact)) {
                        it.classifier = "javadoc"
                    }
                } else if (!docArtifact.endsWith("KotlinMultiplatform")) {
                    log("No javadoc artifact for publication $name in projeitct ${project.name}: " +
                            "no such a task: $docArtifact")
                }

                pub.configurePom(project)
            }
        }
    }

    fun MavenPublication.configurePom(project: Project) {
        pom { pom ->
            pom.name.set("${project.ktMpp.projectLongName.get()} -- Module `${project.name}`")
            pom.description.set(project.description)
            pom.url.set(project.ktMpp.projectHomepage.get())
            pom.licenses { licenses ->
                licenses.license {
                    it.name.set(project.ktMpp.projectLicense.get())
                    it.url.set(project.ktMpp.projectLicenseUrl.get())
                }
            }

            pom.developers { developers ->
                project.ktMpp.developers.get().forEach { developer ->
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

    fun Project.configureJsPackage() {
        if (project == rootProject) return

        configure<NpmPublishExtension> {
            defaultValuesFrom(project)
            token.set(ktMpp.npmToken.get())
            liftPackageJson {
                it.people = ktMpp.developers.get().map(Developer::toNpmPeople).toMutableList()
                it.homepage = ktMpp.projectHomepage.get()
                it.bugs = Bugs(ktMpp.issuesUrl.get(), ktMpp.issuesEmail.get())
                it.license = ktMpp.projectLicense.get()
                it.version = it.version?.substringBefore('+')
                liftPackageJsonToFixDependencies(it)
                if (ktMpp.npmOrganization.isPresent) {
                    liftPackageJsonToSetOrganization(ktMpp.npmOrganization.get(), it)
                    liftJsSources { _, _, line ->
                        liftJsSourcesToSetOrganization(ktMpp.npmOrganization.get(), line)
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
                            println("Results: ${result.resultType} (${result.testCount} tests, " +
                                    "${result.successfulTestCount} successes, ${result.failedTestCount} failures, " +
                                    "${result.skippedTestCount} skipped)")
                        }
                    })
            )
        }
    }
}
