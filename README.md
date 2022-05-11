> _This repository is maintained by the Gradle Enterprise Solutions team, as one of several publicly available repositories:_
> - _[Gradle Enterprise Build Configuration Samples][ge-build-config-samples]_
> - _[Gradle Enterprise Build Optimization Experiments][ge-build-optimization-experiments]_
> - _[Gradle Enterprise Build Validation Scripts][ge-build-validation-scripts]_
> - _[Common Custom User Data Maven Extension][ccud-maven-extension]_
> - _[Common Custom User Data Gradle Plugin][ccud-gradle-plugin]_
> - _[Android Cache Fix Gradle Plugin][android-cache-fix-plugin] (this repository)_
> - _[Wrapper Upgrade Gradle Plugin][wrapper-upgrade-gradle-plugin]_

# Android Cache Fix Gradle Plugin

[![Verify Build](https://github.com/gradle/android-cache-fix-gradle-plugin/actions/workflows/build-verification.yml/badge.svg?branch=main)](https://github.com/gradle/android-cache-fix-gradle-plugin/actions/workflows/build-verification.yml)
[![Plugin Portal](https://img.shields.io/maven-metadata/v?metadataUrl=https://plugins.gradle.org/m2/gradle/plugin/org/gradle/android/android-cache-fix-gradle-plugin/maven-metadata.xml&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/org.gradle.android.cache-fix)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.solutions-team.gradle.com/scans)


Some Android plugin versions have issues with Gradle's build cache feature. When applied to an Android project this plugin applies workarounds for these issues based on the Android plugin and Gradle versions. For other versions, please see [older versions.](#older-android-gradle-plugin-versions)

* Supported Gradle versions: 7.0+
* Supported Android Gradle Plugin versions: 7.0.4, 7.1.3, 7.2.0, 7.3.0-alpha09
* Supported Kotlin versions: 1.4.32+

We only test against the latest patch versions of each minor version of Android Gradle Plugin.  This means that although it may work perfectly well with an older patch version (say 7.0.1), we do not test against these older patch versions, so the latest patch version is the only version from that minor release that we technically support.

The Android cache fix plugin is compatible with the [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html#header) when used in conjunction with Android Gradle Plugin 4.2.0 and above.  Using the configuration cache with earlier versions of the Android Gradle Plugin is not supported.

## Should I use this?
Take a look at the [list of issues](#list-of-issues) especially [unresolved issues](#unresolved-issues). If any of these apply to your project, you can use this plugin to solve them.

## How can I discover issues?
You can discover issues by using the task input comparison tool in Gradle Enterprise. More information about [how to diagnose cache misses here](https://docs.gradle.com/enterprise/tutorials/task-inputs-comparison/). You can compare the inputs of a build that seeds the build cache - typically CI - with a build that consumes from the build cache like a local developer build.
If you discover an issue related to the Android Gradle Plugin, please file an issue in the [Android Bug Tracker](https://source.android.com/setup/contribute/report-bugs). You can also file an [issue here](https://github.com/gradle/android-cache-fix-gradle-plugin/issues) and we can see if a workaround is possible.

## Applying the plugin

This plugin should be applied anywhere the `com.android.application` or `com.android.library` plugins are applied. We recommend adding the plugin to your project's [conventions plugin](https://docs.gradle.org/current/samples/sample_convention_plugins.html).

<details open>
<summary>Groovy</summary>
<br>

```groovy
// in build.grade for convention plugin build
dependencies {
    // ...
    implementation("org.gradle.android.cache-fix:org.gradle.android.cache-fix.gradle.plugin:2.5.3")
    // ...
}

// in com.myconventions.build.gradle
plugins {
    id 'com.android.application' // or 'com.android.library'
    // Add this next line to your existing convention plugin.
    id 'org.gradle.android.cache-fix'
}
```

</details>
<details>
<summary>Kotlin</summary>
<br>

```kotlin
// in build.grade.kts for convention plugin build
dependencies {
  // ...
  implementation("org.gradle.android.cache-fix:org.gradle.android.cache-fix.gradle.plugin:2.5.3")
  // ...
}

// in com.myconventions.build.gradle.kts
plugins {
    id("com.android.application") // or "com.android.library"
  // Add this next line to your existing convention plugin.
    id("org.gradle.android.cache-fix")
}
```
</details>

If you are not using convention plugins and would like a quick way of testing the plugin you can alternatively place it in the root project's build.gradle (change '2.5.3' to the latest version of the cache fix plugin
[here](https://plugins.gradle.org/plugin/org.gradle.android.cache-fix)). We discourage this approach because it uses [cross project configuration](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html#sec:convention_plugins_vs_cross_configuration).

<details open>
<summary>Groovy</summary>
<br>

```groovy
plugins {
    id "org.gradle.android.cache-fix" version "2.5.3" apply false
}

subprojects {
    plugins.withType(com.android.build.gradle.api.AndroidBasePlugin) {
        project.apply plugin: "org.gradle.android.cache-fix"
    }
}
```
</details>
<details>
<summary>Kotlin</summary>
<br>

```kotlin
plugins {
    id("org.gradle.android.cache-fix") version "2.5.3" apply false
}

subprojects {
    plugins.withType<com.android.build.gradle.api.AndroidBasePlugin>() {
        apply(plugin = "org.gradle.android.cache-fix")
    }
}
```
</details>

Note that if you are currently exporting schemas with the Room annotation processor, you will need to change how you specify the output directory according to the instructions [here](https://github.com/gradle/android-cache-fix-gradle-plugin#roomschemalocationworkaround).

## List of issues

You can take a look at the list of issues that the plugin fixes by looking at the classes in  [`org.gradle.android.workarounds`](https://github.com/gradle/android-cache-fix-gradle-plugin/blob/master/src/main/groovy/org/gradle/android/workarounds). It contains a number of `Workaround` implementations annotated with `@AndroidIssue`. The Javadoc has a short description of the problem, and the annotation gives information about when the problem was introduced, what is the first version of the Android plugin that fixes it, and there's a link to the issue on Android's issue tracker:

```groovy
/**
 * Fix {@link org.gradle.api.tasks.compile.CompileOptions#getBootClasspath()} introducing relocatability problems for {@link AndroidJavaCompile}.
 */
@AndroidIssue(introducedIn = "3.0.0", fixedIn = "3.1.0-alpha06", link = "https://issuetracker.google.com/issues/68392933")
static class AndroidJavaCompile_BootClasspath_Workaround implements Workaround {
    // ...
}
```

### Unresolved Issues

The following caching issues are fixed by the cache fix plugin but unresolved in any current or upcoming preview release of the Android Gradle Plugin as of 2021-11-29.

Please star them if you are experiencing them in your project.

* MergeResources is not relocatable: https://issuetracker.google.com/issues/141301405
* Room annotation processor causes cache misses, doesn't declare outputs, overlapping outputs, etc: https://issuetracker.google.com/issues/132245929

## Implementation Notes

### RoomSchemaLocationWorkaround

Most of the workarounds in this plugin should apply to your project without any changes.  However, one workaround
requires some extra configuration.  The Room schema location workaround allows you to specify an output directory for
Room schema exports without breaking caching for your Java and/or Kapt tasks where the annotation processor has been configured.
There are various issues with how this annotation processor works (see https://issuetracker.google.com/issues/132245929
and https://issuetracker.google.com/issues/139438151) which this workaround attempts to mitigate.  However, in order to
do so in a manageable way, it imposes some restrictions:

* The schema export directory must be configured via the "room" project extension instead of as an explicit annotation
processor argument.  If an explicit annotation processor argument is provided, an exception will be thrown, instructing
the user to configure it via the extension:

<details open>
<summary>Groovy</summary>
<br>

```groovy
room {
    schemaLocationDir = file("roomSchemas")
}
```
</details>
<details>
<summary>Kotlin</summary>
<br>

```kotlin
room {
    schemaLocationDir.set(file("roomSchemas"))
}
```
</details>

* There can only be a single schema export directory for the project - you cannot configure variant-specific export
directories.  Schemas exported from different variants will be merged in the directory specified in the "room" extension.

### MergeNativeLibs, StripDebugSymbols, MergeJavaResources, MergeSourceSetFolders, BundleLibraryClassesJar, DataBindingMergeDependencyArtifacts, LibraryJniLibs and ZipMerging Workarounds

It has been observed that caching the `MergeNativeLibsTask`, `StripDebugSymbols`, `MergeJavaResources`, `MergeSourceSetFolders`, `BundleLibraryClassesJar`, `DataBindingMergeDependencyArtifacts`, `LibraryJniLibs` and  `ZipMergingTask` tasks rarely provide any significant positive avoidance savings.  In fact, they frequently provide negative savings, especially when fetched from a remote cache node.  As such, these workarounds actually disable caching for these tasks.

### Older Android Gradle Plugin Versions

Use Android Cache Fix Plugin 2.4.6 when using an older Android Gradle Plugin version.

* Supported Gradle versions: 5.4.1+
* Supported Android Gradle Plugin versions: 3.5.4, 3.6.4, 4.0.1, 4.1.3, 4.2.2
* Supported Kotlin versions: 1.3.70+

## License

The  Android Cache Fix Gradle plugin is open-source software released under the [Apache 2.0 License][apache-license].

[ge-build-config-samples]: https://github.com/gradle/gradle-enterprise-build-config-samples
[ge-build-optimization-experiments]: https://github.com/gradle/gradle-enterprise-build-optimization-experiments
[ge-build-validation-scripts]: https://github.com/gradle/gradle-enterprise-build-validation-scripts
[ccud-gradle-plugin]: https://github.com/gradle/common-custom-user-data-gradle-plugin
[ccud-maven-extension]: https://github.com/gradle/common-custom-user-data-maven-extension
[android-cache-fix-plugin]: https://github.com/gradle/android-cache-fix-gradle-plugin
[wrapper-upgrade-gradle-plugin]: https://github.com/gradle/wrapper-upgrade-gradle-plugin
[gradle-enterprise]: https://gradle.com/enterprise
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0.html
