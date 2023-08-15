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
abstract class CreateGitTag @Inject constructor(
    private val objects: ObjectFactory,
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    val tagName: Property<String> = objects.property(String::class.java)

    @get:Input
    @get:Optional
    val overwriteExisting: Property<Boolean> = objects.property(Boolean::class.java).apply {
        value(false)
    }

    @TaskAction
    fun applyArgbash() {
        logger.info("Tagging HEAD as ${tagName.get()}")
        execOperations.exec {
            val args = mutableListOf("git", "tag")
            if (overwriteExisting.get()) {
                args.add("-f")
            }
            args.add(tagName.get())
            commandLine(args)
        }
        execOperations.exec {
            val args = mutableListOf("git", "push", "origin")
            if (overwriteExisting.get()) {
                args.add("-f")
            }
            args.add("--tags")
            commandLine(args)
        }
    }
}
