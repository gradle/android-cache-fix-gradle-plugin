package org.gradle.android

import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Project
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
            void configure(AndroidJavaCompile task, Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs) {
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
                    def processedArgs = processArgs(task, task.options.compilerArgs)
                    overrideProperty(task, processedArgs)
                }
            }
        }
    }

    List<String> processArgs(AndroidJavaCompile task, List<String> args) {
        def processedArgs = []
        def remainingArgs = args.iterator()
        while (remainingArgs.hasNext()) {
            def arg = remainingArgs.next()
            for (Rule rule : rules) {
                def matcher = rule.pattern.matcher(arg)
                if (matcher.matches()) {
                    rule.configure(task, matcher, processedArgs, remainingArgs)
                    break
                }
            }
        }
        return processedArgs
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static void overrideProperty(AndroidJavaCompile task, List processedArgs) {
        task.inputs.property "options.compilerArgs", ""
        task.inputs.property "options.compilerArgs.workaround", processedArgs
    }

    static class AnnotationProcessorOverride extends Rule {
        private final BiAction<? super AndroidJavaCompile, String> action

        AnnotationProcessorOverride(String property, BiAction<? super AndroidJavaCompile, String> action) {
            super(Pattern.compile("-A${Pattern.quote(property)}=(.*)"))
            this.action = action
        }

        static AnnotationProcessorOverride of(String property, BiAction<? super AndroidJavaCompile, String> action) {
            return new AnnotationProcessorOverride(property, action)
        }

        void configure(AndroidJavaCompile task, Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs) {
            // Skip the arg
            action.execute(task, match.group(1))
        }
    }

    static class Skip extends Rule {
        Skip(String arg) {
            super(arg)
        }

        Skip(Pattern pattern) {
            super(pattern)
        }

        static Skip matching(String pattern) {
            return new Skip(Pattern.compile(pattern))
        }

        @Override
        void configure(AndroidJavaCompile task, Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs) {
        }
    }

    static class SkipNext extends Rule {
        SkipNext(String arg) {
            super(arg)
        }

        SkipNext(Pattern pattern) {
            super(pattern)
        }

        static SkipNext matching(String pattern) {
            return new SkipNext(Pattern.compile(pattern))
        }

        @Override
        void configure(AndroidJavaCompile task, Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs) {
            if (remainingArgs.hasNext()) {
                remainingArgs.next()
            }
        }
    }

    static abstract class Rule {
        final Pattern pattern

        Rule(String arg) {
            this(Pattern.compile(Pattern.quote(arg)))
        }

        Rule(Pattern pattern) {
            this.pattern = pattern
        }

        abstract void configure(AndroidJavaCompile task, Matcher match, Collection<String> processedArgs, Iterator<String> remainingArgs)

        @Override
        String toString() {
            return "${getClass().simpleName}[${pattern.pattern()}]"
        }
    }
}
