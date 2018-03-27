# Android cache fix Gradle plugin

[![Build Status](https://travis-ci.org/gradle/android-cache-fix-gradle-plugin.svg?branch=master)](https://travis-ci.org/gradle/android-cache-fix-gradle-plugin)

Some Android plugin versions have issues with Gradle's build cache feature. When applied to an Android project this plugin applies workarounds for these issues based on the Android plugin and Gradle versions.

* Supported Gradle versions: 4.1+
* Supported Android versions: 3.0.0, 3.0.1, 3.1.0, 3.2.0-alpha01

**Note:** With Android 3.1.x and 3.2.x the cache-fix plugin is only required if you are using Android's data binding feature.

## List of issues

You can take a look at the list of issues that the plugin fixes by looking at the code of [`AndroidCacheFixPlugin`](https://github.com/gradle/android-cache-fix-gradle-plugin/blob/master/src/main/groovy/org/gradle/android/AndroidCacheFixPlugin.groovy) itself. It contains a number of `Workaround` implementations annotated with `@AndroidIssue`. The Javadoc has a short description of the problem, and the annotation gives information about when the problem was introduced, what is the first version of the Android plugin that fixes it, and there's a link to the issue on Android's issue tracker:

```groovy
/**
 * Fix {@link org.gradle.api.tasks.compile.CompileOptions#getBootClasspath()} introducing relocatability problems for {@link AndroidJavaCompile}.
 */
@AndroidIssue(introducedIn = "3.0.0", fixedIn = "3.1.0-alpha06", link = "https://issuetracker.google.com/issues/68392933")
static class AndroidJavaCompile_BootClasspath_Workaround implements Workaround {
    // ...
}
```
