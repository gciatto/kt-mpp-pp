package io.github.gciatto.kt.mpp

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

class KtMppPlusPlusExtension(objects: ObjectFactory) {
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
}