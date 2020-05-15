# Android cache fix Gradle plugin

![CI](https://github.com/gradle/android-cache-fix-gradle-plugin/workflows/CI/badge.svg?branch=master)

Some Android plugin versions have issues with Gradle's build cache feature. When applied to an Android project this plugin applies workarounds for these issues based on the Android plugin and Gradle versions.

* Supported Gradle versions: 5.4.1+
* Supported Android versions: 3.5.0, 3.5.1, 3.5.2, 3.5.3, 3.6.0, 3.6.1, 3.6.2, 4.0

## Applying the plugin

This plugin should be applied anywhere the `com.android.application` or `com.android.library` plugins are applied.  Typically,
this can just be injected from the root project's build.gradle (change '1.0.4' to the latest version of the cache fix plugin
[here](https://plugins.gradle.org/plugin/org.gradle.android.cache-fix)):

``` groovy
plugins {
    id "org.gradle.android.cache-fix" version "1.0.4" apply false
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
