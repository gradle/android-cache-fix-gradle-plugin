import com.gradle.enterprise.gradleplugin.testretry.retry
import groovy.json.JsonSlurper

plugins {
    id("groovy")
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("codenarc")
    id("com.gradle.plugin-publish") version "1.2.0"
    id("com.github.breadmoirai.github-release") version "2.4.1"
    id("org.gradle.wrapper-upgrade") version "0.11.2"
}

val releaseVersion = releaseVersion()
val releaseNotes = releaseNotes()
val isCI = (System.getenv("CI") ?: "false").toBoolean()

group = "org.gradle.android"
version = releaseVersion.get()
description = "A Gradle plugin to fix Android caching problems"

repositories {
    google()
    mavenCentral()
}

dependencies {
    val versions = mapOf(
        "agp" to "8.0.2",
        "sdkBuildTools" to "31.0.2",
        "spock" to "2.3-groovy-3.0",
    )

    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:${versions["agp"]}")
    compileOnly("com.android.tools:common:${versions["sdkBuildTools"]}")
    compileOnly("com.android.tools:sdk-common:${versions["sdkBuildTools"]}")
    implementation("com.google.guava:guava:32.0.1-jre")


    testImplementation(gradleTestKit())
    testImplementation("com.android.tools.build:gradle:${versions["agp"]}")
    testImplementation(platform("org.spockframework:spock-bom:${versions["spock"]}"))
    testImplementation("org.spockframework:spock-core") { exclude(group = "org.codehaus.groovy") }
    testImplementation("org.spockframework:spock-junit4") { exclude(group = "org.codehaus.groovy") }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}

wrapperUpgrade {
    gradle {
        create("android-cache-fix-gradle-plugin") {
            repo.set("gradle/android-cache-fix-gradle-plugin")
        }
    }
}

java {
    toolchain {
        // AGP 7+ only supports JDK 11+
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

// Main plugin publishing metadata
gradlePlugin {
    website.set("https://github.com/gradle/android-cache-fix-gradle-plugin")
    vcsUrl.set("https://github.com/gradle/android-cache-fix-gradle-plugin")

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
val localRepo = file("$buildDir/local-repo")

val isProdPortal = System.getProperty("gradle.portal.url") == null
// The legacy groupId gradle.plugin.* is only allowed when the plugin
// has already been published
val pluginGroupId = if (isCI && isProdPortal) "gradle.plugin.org.gradle.android" else project.group
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = pluginGroupId.toString()
        }
    }
    repositories {
        maven {
            url = localRepo.toURI()
        }
    }
}

// Configuration common to all test tasks
tasks.withType<Test>().configureEach {
    dependsOn(tasks.publish)
    workingDir = projectDir
    systemProperty("local.repo", projectDir.toPath().relativize(localRepo.toPath()).toString())
    systemProperty("pluginGroupId", pluginGroupId)
    systemProperty("org.gradle.android.cache-fix.version", version)
    useJUnitPlatform()
    retry {
        maxRetries.set(if (isCI) 1 else 0)
        maxFailures.set(20)
    }

    predictiveSelection {
        enabled.set(providers.gradleProperty("isPTSEnabled").map { it != "false" }.orElse(false))
    }
}

getSupportedVersions().keys.forEach { androidVersion ->
    val versionSpecificTest = tasks.register<Test>(androidTestTaskName(androidVersion)) {
        description = "Runs the multi-version tests for AGP $androidVersion"
        group = "verification"

        systemProperty("org.gradle.android.testVersion", androidVersion)
        project.providers.environmentVariable("ZULU_JDK").orNull?.let {
            systemProperty("org.gradle.android.java_zulu_path", it)
        }
        project.providers.environmentVariable("ZULU_ALT_JDK").orNull?.let {
            systemProperty("org.gradle.android.java_zulu_alt_path", it)
        }

        if (androidVersion >= "8.0.0") {
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
            })
        }
    }

    tasks.named("check").configure {
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
    failOnWarning.set(true)
    enableStricterValidation.set(true)
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
    token(System.getenv("ANDROID_CACHE_FIX_PLUGIN_GIT_TOKEN") ?: "")
    owner.set("gradle")
    repo.set("android-cache-fix-gradle-plugin")
    releaseName.set(releaseVersion)
    tagName.set(releaseVersion.map { "v$it" })
    prerelease.set(false)
    overwrite.set(false)
    generateReleaseNotes.set(false)
    body.set(releaseNotes)
    targetCommitish.set("main")
}

val createReleaseTag = tasks.register<CreateGitTag>("createReleaseTag") {
    // Ensure tag is created only after successful publishing
    mustRunAfter("publishPlugins")
    tagName.set(githubRelease.tagName.map { it.toString() })
}

tasks.named("githubRelease").configure {
    dependsOn(createReleaseTag)
}

tasks.withType<com.gradle.publish.PublishTask>().configureEach {
    notCompatibleWithConfigurationCache("$name task does not support configuration caching")
}

fun releaseVersion(): Provider<String> {
    val releaseVersionFile = layout.projectDirectory.file("release/version.txt")
    return providers.fileContents(releaseVersionFile).asText.map { it -> it.trim() }
}

fun releaseNotes(): Provider<String> {
    val releaseNotesFile = layout.projectDirectory.file("release/changes.md")
    return providers.fileContents(releaseNotesFile).asText.map { it.trim() }
}

@Suppress("UNCHECKED_CAST")
fun getSupportedVersions(): Map<String, Array<String>> {
    return (JsonSlurper()
        .parse(file("src/main/resources/versions.json")) as Map<String, Map<String, Array<String>>>)["supportedVersions"]!!
}
