package org.gradle.android

import org.gradle.api.Project
import org.gradle.api.Task
import spock.lang.Specification

import static org.gradle.android.CompilerArgsProcessor.AnnotationProcessorOverride
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
        processor.processArgs([]) == []
        processor.processArgs(["alma", "bela", "-switch"]) == ["alma", "bela", "-switch"]
    }

    def "can skip arg"() {
        processor.addRule(Skip.matching("-s"))
        expect:
        processor.processArgs([]) == []
        processor.processArgs(["alma", "bela", "-s", "-switch"]) == ["alma", "bela", "-switch"]
    }

    def "can skip multiple args"() {
        processor.addRule(SkipNext.matching("-s"))
        expect:
        processor.processArgs([]) == []
        processor.processArgs(["alma", "bela", "-s", "file.txt", "-switch"]) == ["alma", "bela", "-switch"]
    }

    def "can override annotation processor parameters"() {
        def task = Mock(Task)
        def result = ""
        def rule = AnnotationProcessorOverride.of("alma") { Task task1, String path ->
            assert task == task1
            result = path
        }
        def args = ["alma", "bela", "-Aalma=alma.txt", "-Abela=123"]
        processor.addRule(rule)

        expect:
        processor.processArgs([]) == []
        processor.processArgs(args) == ["alma", "bela", "-Abela=123"]

        when:
        rule.configureTask(task, args)
        then:
        result == "alma.txt"
    }
}
