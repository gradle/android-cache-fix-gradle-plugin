# Android cache fix Gradle plugin

![CI](https://github.com/gradle/android-cache-fix-gradle-plugin/workflows/CI/badge.svg?branch=master)

Some Android plugin versions have issues with Gradle's build cache feature. When applied to an Android project this plugin applies workarounds for these issues based on the Android plugin and Gradle versions.

* Supported Gradle versions: 5.4.1+
* Supported Android versions: 3.5.4, 3.6.4, 4.0.1, 4.1.0-rc3, 4.2.0-alpha12
* Supported Kotlin versions: 1.3.70+

We only test against the latest patch versions of each minor version of Android Gradle Plugin.  This means that although it may work perfectly well with an older patch version (say 3.6.2), we do not test against these older patch versions, so the latest patch version is the only version from that minor release that we technically support.

## Should I use this?
Take a look at the [list of issues](#list-of-issues) especially [unresolved issues](#unresolved-issues). If any of these apply to your project, you can use this plugin to solve them.

## How can I discover issues?
You can discover issues by using the task input comparison tool in Gradle Enterprise. More information about [how to diagnose cache misses here](https://docs.gradle.com/enterprise/tutorials/task-inputs-comparison/). You can compare the inputs of a build that seeds the build cache - typically CI - with a build that consumes from the build cache like a local developer build.
If you discover an issue related to the Android Gradle Plugin, please file an issue in the [Android Bug Tracker](https://source.android.com/setup/contribute/report-bugs). You can also file an [issue here](https://github.com/gradle/android-cache-fix-gradle-plugin/issues) and we can see if a workaround is possible.

## Applying the plugin

This plugin should be applied anywhere the `com.android.application` or `com.android.library` plugins are applied.  Typically,
this can just be injected from the root project's build.gradle (change '1.0.13' to the latest version of the cache fix plugin
[here](https://plugins.gradle.org/plugin/org.gradle.android.cache-fix)):

``` groovy
plugins {
    id "org.gradle.android.cache-fix" version "1.0.13" apply false
}

subprojects {
    apply plugin: "org.gradle.android.cache-fix"
}
```

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

The following caching issues are fixed by the cache fix plugin but unresolved in any current or upcoming preview release of the Android Gradle Plugin as of 21.08.2020.

Please star them if you are experiencing them in your project.

* CompileLibraryResourcesTask is not relocatable: https://issuetracker.google.com/issues/155218379
* DexFileDependenciesTask is not cacheable: https://issuetracker.google.com/160138798
* MergeResources is not relocatable: https://issuetracker.google.com/issues/141301405
* Room annotation processor causes cache misses, doesn't declare outputs, overlapping outputs, etc: https://issuetracker.google.com/issues/132245929
