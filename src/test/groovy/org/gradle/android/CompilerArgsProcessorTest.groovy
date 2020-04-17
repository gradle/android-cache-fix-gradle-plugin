package org.gradle.android

import org.gradle.android.workarounds.CompilerArgsProcessor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.compile.JavaCompile
import spock.lang.Specification

import static org.gradle.android.workarounds.CompilerArgsProcessor.AnnotationProcessorOverride
import static org.gradle.android.workarounds.CompilerArgsProcessor.Skip
import static org.gradle.android.workarounds.CompilerArgsProcessor.SkipNext

class CompilerArgsProcessorTest extends Specification {
    def inputs = Stub(JavaCompile)
    CompilerArgsProcessor processor

    def setup() {
        processor = new CompilerArgsProcessor(Stub(Project))
    }

    def "processes arguments by default"() {
        expect:
        processor.processArgs([], inputs) == []
        processor.processArgs(["alma", "bela", "-switch"], inputs) == ["alma", "bela", "-switch"]
    }

    def "can skip arg"() {
        processor.addRule(Skip.matching("-s"))
        expect:
        processor.processArgs([], inputs) == []
        processor.processArgs(["alma", "bela", "-s", "-switch"], inputs) == ["alma", "bela", "-switch"]
    }

    def "can skip multiple args"() {
        processor.addRule(SkipNext.matching("-s"))
        expect:
        processor.processArgs([], inputs) == []
        processor.processArgs(["alma", "bela", "-s", "file.txt", "-switch"], inputs) == ["alma", "bela", "-switch"]
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
        processor.processArgs([], inputs) == []
        processor.processArgs(args, inputs) == ["alma", "bela", "-Abela=123"]

        when:
        rule.configureTask(task, args)
        then:
        result == "alma.txt"
    }
}
