package org.gradle.android

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Version of Android plugin that fixes the problem. Workaround not applied if current Android plugin version is the same or later.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@interface AndroidIssue {
    String introducedIn()
    String[] fixedIn() default []
    String link()
}
