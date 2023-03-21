package org.gradle.android.writer

class RenderScript {

    static String rs() {
        return """
                #pragma version(1)
                #pragma rs java_package_name(com.example.myapplication)

                static void addintAccum(int *accum, int val) {
                   *accum += val;
                }
        """.stripIndent()
    }
}
