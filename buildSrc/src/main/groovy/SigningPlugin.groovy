import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.signing.SigningExtension

class SigningPlugin implements Plugin<Project> {

    void apply(Project project) {
        Boolean isCi = (System.getenv('CI') ?: 'false').toBoolean()
        if (isCi) {
            project.plugins.apply('signing')
            project.extensions.configure(SigningExtension) {
                it.required = isCi
                // Require publications to be signed on CI. Otherwise, publication will be signed only if keys are provided.
                it.useInMemoryPgpKeys(
                    project.providers.environmentVariable("PGP_SIGNING_KEY").orNull,
                    project.providers.environmentVariable("PGP_SIGNING_KEY_PASSPHRASE").orNull
                )

            }
        }
    }
}
