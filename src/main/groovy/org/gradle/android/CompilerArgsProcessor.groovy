package org.gradle.android

import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.android.CompilerArgsProcessor.Rule
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskInputs
import org.gradle.internal.BiAction

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class CompilerArgsProcessor {
    private final List<Rule> rules
    private final Project project
    private boolean applied

    CompilerArgsProcessor(Project project) {
        this.project = project
        this.rules = [new Rule(Pattern.compile(".*")) {
            @Override
            void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs) {
                processedArgs.add(match.group())
            }
        }] as List<Rule>
    }

    void addRule(Rule rule) {
        ensureApplied()
        rules.add(0, rule)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void ensureApplied() {
        if (applied) {
            return
        }
        applied = true

        project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
            project.gradle.taskGraph.beforeTask {
                if (task == it) {
                    def processedArgs = processArgs(task.options.compilerArgs, task.inputs)
                    overrideProperty(task, processedArgs)
                }
            }
        }
    }

    List<String> processArgs(List<String> args, TaskInputs inputs) {
        def processedArgs = []
        def remainingArgs = args.iterator()
        while (remainingArgs.hasNext()) {
            def arg = remainingArgs.next()
            for (Rule rule : rules) {
                def matcher = rule.pattern.matcher(arg)
                if (matcher.matches()) {
                    rule.process(matcher, processedArgs, remainingArgs, inputs)
                    break
                }
            }
        }
        return processedArgs
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static void overrideProperty(AndroidJavaCompile task, List processedArgs) {
        task.inputs.property "options.compilerArgs", ""
        task.inputs.property "options.compilerArgs.filtered", ""
        task.inputs.property "options.compilerArgs.workaround", processedArgs
    }

    static class AnnotationProcessorOverride extends Rule {
        private final BiAction<? super Task, String> action

        AnnotationProcessorOverride(String property, BiAction<? super Task, String> action) {
            super(Pattern.compile("-A${Pattern.quote(property)}=(.*)"))
            this.action = action
        }

        static AnnotationProcessorOverride of(String property, BiAction<? super Task, String> action) {
            return new AnnotationProcessorOverride(property, action)
        }

        void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs) {
            // Skip the arg
        }

        void configureAndroidJavaCompile(AndroidJavaCompile task) {
            configureTask(task, task.options.compilerArgs)
        }

        void configureTask(Task task, List<String> args) {
            for (String arg : (args)) {
                def matcher = pattern.matcher(arg)
                if (matcher.matches()) {
                    def path = matcher.group(1)
                    action.execute(task, path)
                    break
                }
            }
        }
    }

    static class InputDirectory extends Rule {
        private final String argumentName

        InputDirectory(String argumentName) {
            super(Pattern.compile("-A" + Pattern.quote(argumentName) + "=(.*)"))
            this.argumentName = argumentName
        }

        static InputDirectory withAnnotationProcessorArgument(String argumentName) {
            return new InputDirectory(argumentName)
        }

        @Override
        void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs) {
            inputs.dir(match.group(1))
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName(argumentName)
        }
    }

    static class Skip extends Rule {
        Skip(Pattern pattern) {
            super(pattern)
        }

        static Skip matching(String pattern) {
            return new Skip(Pattern.compile(pattern))
        }

        @Override
        void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs) {
        }
    }

    static class SkipNext extends Rule {
        SkipNext(Pattern pattern) {
            super(pattern)
        }

        static SkipNext matching(String pattern) {
            return new SkipNext(Pattern.compile(pattern))
        }

        @Override
        void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs) {
            if (remainingArgs.hasNext()) {
                remainingArgs.next()
            }
        }
    }

    static abstract class Rule {
        final Pattern pattern

        Rule(Pattern pattern) {
            this.pattern = pattern
        }

        abstract void process(Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs, TaskInputs inputs)

        @Override
        String toString() {
            return "${getClass().simpleName}[${pattern.pattern()}]"
        }
    }
}
