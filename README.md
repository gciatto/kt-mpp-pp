# Kt-MPP++

A Gradle plugin simplifying the configuration of multiplatform (JVM and NodeJs) Kotlin projects.
In particular, the plugin simplifies the creation of multi-project builds where subprojects may target Kt-MPP, JVM-only, or JS-only.

### Minimal configuration

The root project's `build.gradle(.kts)` should contain at least:
```kotlin
plugins {
    id("io.github.gciatto.kt-mpp-pp") version X.Y.Z
}

kotlinMultiplatform {
    developer("Giovanni Ciatto", "giovanni.ciatto@gmail.com", "http://about.me/gciatto")
    
    // rootProject is Kt-MPP by default

    // in case of multiple subProjects:
    jvmOnlyProjects("jvmOnlySuproject1", "jvmSubproject2", ...)
    jsOnlyProjects("jsOnlySuproject1", "jsSubproject2", ...)
    otherProjects("nonKtSubproject", "nonJvmProject", "nonJsProject", ...)
}
```

The root project's `grale.properties` should contain the following properties:
```properties
io.github.gciatto.kt-npm-publish.verbose=false

projectLongName=Project Long Name (defaults to project name)
projectDescription=Project description
githubToken= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_githubToken
githubOwner=username
githubRepo=https://github.com/org/project
bintrayUser=username
bintrayKey= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_bintrayKey
bintrayRepo=bintray-repo-name
bintrayUserOrg=bintray-org-name
projectHomepage=CUSTOM_PROJECT_HOMEPAGE
projectLicense=Apache-2.0  # (default)
projectLicenseUrl=https://www.apache.org/licenses/LICENSE-2.0 # (default)
mavenRepo=https://oss.sonatype.org/service/local/staging/deploy/maven2/ #(default)
mavenUsername=username
mavenPassword= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_mavenPassword
scmUrl=https://github.com/org/project
scmConnection=git@github.com:org/password.git
signingKey= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_signingKey
signingPassword= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_signingPassword
npmToken= # leave this empty, and setup an env. var. named ORG_GRADLE_PROJECT_npmToken
issuesUrl=https://github.com/org/project/issues
issuesEmail=name.surname@example.com

# optionally
npmOrganization=org
```