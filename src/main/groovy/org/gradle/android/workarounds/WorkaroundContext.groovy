package org.gradle.android.workarounds

import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class WorkaroundContext {
    final Project project
    final CompilerArgsProcessor compilerArgsProcessor

    WorkaroundContext(Project project, CompilerArgsProcessor compilerArgsProcessor) {
        this.project = project
        this.compilerArgsProcessor = compilerArgsProcessor
    }
}
