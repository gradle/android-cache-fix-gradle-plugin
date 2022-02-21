import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Produces no cacheable output")
abstract class CreateGitTag extends DefaultTask {

    @Inject
    abstract ObjectFactory getObjects()

    @Inject
    abstract ExecOperations getExecOperations()

    @Input
    abstract Property<String> getTagName()

    @Input
    @Optional
    final abstract Property<Boolean> overwriteExisting = objects.property(Boolean).convention(false)

    @TaskAction
    def tag() {
        logger.info("Tagging HEAD as ${tagName.get()}")
        execOperations.exec { execSpec ->
            def args = ["git", "tag"]
            if (overwriteExisting.get()) {
                args.add("-f")
            }
            args.add(tagName.get())
            execSpec.commandLine(args)
        }
        execOperations.exec { execSpec ->
            def args = ["git", "push", "origin"]
            if (overwriteExisting.get()) {
                args.add("-f")
            }
            args.add("--tags")
            execSpec.commandLine(args)
        }
    }

}
