package org.gradle.android

import org.gradle.api.Project
import spock.lang.Specification

import static org.gradle.android.CompilerArgsProcessor.Skip
import static org.gradle.android.CompilerArgsProcessor.SkipNext

class CompilerArgsProcessorTest extends Specification {
    def task = null
    CompilerArgsProcessor processor

    def setup() {
        processor = new CompilerArgsProcessor(Stub(Project))
    }

    def "processes arguments by default"() {
        expect:
        processor.processArgs(task, []) == []
        processor.processArgs(task, ["alma", "bela", "-switch"]) == ["alma", "bela", "-switch"]
    }

    def "can skip arg"() {
        processor.addRule(Skip.matching("-s"))
        expect:
        processor.processArgs(task, []) == []
        processor.processArgs(task, ["alma", "bela", "-s", "-switch"]) == ["alma", "bela", "-switch"]
    }

    def "can skip multiple args"() {
        processor.addRule(SkipNext.matching("-s"))
        expect:
        processor.processArgs(task, []) == []
        processor.processArgs(task, ["alma", "bela", "-s", "file.txt", "-switch"]) == ["alma", "bela", "-switch"]
    }
}
