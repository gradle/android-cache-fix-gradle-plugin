import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionProfile.FAST
import com.gradle.develocity.agent.gradle.test.PredictiveTestSelectionProfile.STANDARD
import groovy.json.JsonSlurper

// Upgrade transitive dependencies in plugin classpath
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        constraints {
            // Dependency of 'com.github.breadmoirai.github-release:2.5.2'
            classpath(libs.okio) // CVE-2023-3635
        }
    }
}

plugins {
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("codenarc")
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.github.release)
    alias(libs.plugins.gradle.wrapperUpgrade)
}

val releaseVersion = releaseVersion()
val releaseNotes = releaseNotes()
val isCI = providers.environmentVariable("CI").isPresent

group = "org.gradle.android"
version = releaseVersion.get()
description = "A Gradle plugin to fix Android caching problems"

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.common)
    compileOnly(libs.android.sdkCommon)
    implementation(libs.guava)

    testImplementation(gradleTestKit())
    testImplementation(libs.android.gradlePlugin)
    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core) { exclude(group = "org.codehaus.groovy") }
    testImplementation(libs.spock.junit4) { exclude(group = "org.codehaus.groovy") }
    testImplementation(libs.junit.jupiter.api)
}

wrapperUpgrade {
    gradle {
        create("android-cache-fix-gradle-plugin") {
            repo = "gradle/android-cache-fix-gradle-plugin"
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Main plugin publishing metadata
gradlePlugin {
    website = "https://github.com/gradle/android-cache-fix-gradle-plugin"
    vcsUrl = "https://github.com/gradle/android-cache-fix-gradle-plugin"

    plugins {
        create("androidCacheFixPlugin") {
            id = "org.gradle.android.cache-fix"
            displayName = "Gradle Android cache fix plugin"
            description = releaseNotes.get()
            implementationClass = "org.gradle.android.AndroidCacheFixPlugin"
            tags.addAll("android", "cache", "fix")
        }
    }
}

// A local repo we publish our library to for testing in order to workaround limitations
// in the TestKit plugin classpath.
val localRepo = layout.buildDirectory.dir("local-repo")

val isProdPortal = providers.systemProperty("gradle.portal.url").orNull == null
// The legacy groupId gradle.plugin.* is only allowed when the plugin
// has already been published
val pluginGroupId: String = if (isCI && isProdPortal) "gradle.plugin.org.gradle.android" else project.group.toString()
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = pluginGroupId
            pom {
                name.set("Android Cache Fix Gradle Plugin")
                description.set("A Gradle plugin to workaround cache issues in Android builds")
                url.set("https://github.com/gradle/android-cache-fix-gradle-plugin")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("The Gradle team")
                        organization.set("Gradle Inc.")
                        organizationUrl.set("https://gradle.com")
                    }
                }
                scm {
                    developerConnection.set("scm:git:https://github.com/gradle/android-cache-fix-gradle-plugin.git")
                    url.set("https://github.com/gradle/android-cache-fix-gradle-plugin")
                }
            }
        }
    }
    repositories {
        maven {
            url = localRepo.get().asFile.toURI()
        }
    }
}

tasks.withType<Jar>().configureEach {
    into(".") {
        from(layout.projectDirectory.file("LICENSE"))
        from(layout.projectDirectory.dir("release/distribution"))
    }
}

// Configuration common to all test tasks
tasks.withType<Test>().configureEach {
    dependsOn(tasks.publish)
    workingDir = projectDir
    systemProperty("local.repo", projectDir.toPath().relativize(localRepo.get().asFile.toPath()).toString())
    systemProperty("pluginGroupId", pluginGroupId)
    systemProperty("org.gradle.android.cache-fix.version", version)
    useJUnitPlatform()
    develocity.testRetry {
        maxRetries = if (isCI) 1 else 0
        maxFailures = 20
    }
}

tasks.test {
    develocity.predictiveTestSelection {
        profile = STANDARD
    }
}

val latestVersion = providers.gradleProperty("org.gradle.android.latestKnownAgpVersion")
val testedVersions = readTestedVersions()

check(latestVersion.get() in testedVersions) {
    "The project must be updated to support AGP $latestVersion. Please add it to tested versions."
}

val test by testing.suites.existing(JvmTestSuite::class)
testedVersions.keys.forEach { androidVersion ->
    val versionSpecificTest = tasks.register<Test>(androidTestTaskName(androidVersion)) {
        testClassesDirs = files(test.map { it.sources.output.classesDirs })
        classpath = files(test.map { it.sources.runtimeClasspath })
        description = "Runs the multi-version tests for AGP $androidVersion"
        group = "verification"

        systemProperty("org.gradle.android.testVersion", androidVersion)
        project.providers.environmentVariable("ZULU_JDK").orNull?.let {
            systemProperty("org.gradle.android.java_zulu_path", it)
        }
        project.providers.environmentVariable("ZULU_ALT_JDK").orNull?.let {
            systemProperty("org.gradle.android.java_zulu_alt_path", it)
        }

        if (androidVersion < "7.1.0") {
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(11)
                }
            }
        }

        develocity.predictiveTestSelection {
            profile = FAST
        }
    }

    tasks.check {
        dependsOn(versionSpecificTest)
    }
}

fun androidTestTaskName(androidVersion: String): String {
    return "testAndroid${normalizeVersion(androidVersion)}"
}

fun normalizeVersion(version: String): String {
    return version.replace("[.\\-]".toRegex(), "_")
}

// A basic sanity check to run before running all test tasks
tasks.register("sanityCheck") {
    dependsOn(tasks.withType<CodeNarc>(), tasks.validatePlugins)
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning = true
    enableStricterValidation = true
}

signing {
    // Require publications to be signed when :publishPlugins task is included in the TaskGraph
    setRequired({ gradle.taskGraph.hasTask(":publishPlugins") })

    useInMemoryPgpKeys(
        providers.environmentVariable("PGP_SIGNING_KEY").orNull,
        providers.environmentVariable("PGP_SIGNING_KEY_PASSPHRASE").orNull
    )
}

githubRelease {
    token(providers.environmentVariable("ANDROID_CACHE_FIX_PLUGIN_GIT_TOKEN").orNull)
    owner = "gradle"
    repo = "android-cache-fix-gradle-plugin"
    releaseName = releaseVersion
    tagName = releaseVersion.map { "v$it" }
    prerelease = false
    overwrite = false
    generateReleaseNotes = false
    body = releaseNotes
    targetCommitish = "main"
}

val createReleaseTag = tasks.register<CreateGitTag>("createReleaseTag") {
    // Ensure tag is created only after successful publishing
    mustRunAfter(tasks.publishPlugins)
    tagName = githubRelease.tagName.map { it.toString() }
}

tasks.githubRelease {
    dependsOn(createReleaseTag)
}

tasks.withType<com.gradle.publish.PublishTask>().configureEach {
    notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/21283")
}

fun releaseVersion(): Provider<String> {
    val releaseVersionFile = layout.projectDirectory.file("release/version.txt")
    return providers.fileContents(releaseVersionFile).asText.map { it.trim() }
}

fun releaseNotes(): Provider<String> {
    val releaseNotesFile = layout.projectDirectory.file("release/changes.md")
    return providers.fileContents(releaseNotesFile).asText.map { it.trim() }
}

@Suppress("UNCHECKED_CAST")
fun readTestedVersions(): Map<String, Array<String>> {
    val versionFile = providers.fileContents(layout.projectDirectory.file("src/test/resources/versions.json"))
    val json = JsonSlurper().parse(versionFile.asBytes.get()) as Map<String, Map<String, Array<String>>>
    return json.getValue("testedVersions")
}
