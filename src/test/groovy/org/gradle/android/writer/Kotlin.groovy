package org.gradle.android.writer

class Kotlin {

    static String simpleDataClass(String packageName) {
        return """
                package ${packageName}

                data class Foo(val lable: String)

            """.stripIndent()
    }
}
