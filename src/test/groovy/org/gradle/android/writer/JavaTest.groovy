package org.gradle.android.writer

class JavaTest {

    static String simpleTest(String packageName){
        return """
                package ${packageName};

                public class JavaUserTest {
                }
            """.stripIndent()
    }
}
