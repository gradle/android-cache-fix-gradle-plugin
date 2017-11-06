package org.gradle.android

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Version of Gradle that fixes the problem. Workaround not applied if current Gradle version is the same or later.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@interface FixedInGradle {
    String version()
}
