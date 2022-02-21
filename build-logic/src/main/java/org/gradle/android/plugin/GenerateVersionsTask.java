package org.gradle.android.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;

@CacheableTask
public abstract class GenerateVersionsTask extends DefaultTask {

    @Nested
    abstract ListProperty<AndroidVersionDomainObject> getSupportedVersions();

    @Input
    abstract Property<String> getVersion();

    @OutputDirectory
    final Provider<Directory> destination;

    public GenerateVersionsTask() {
        destination = getProject().getLayout().getBuildDirectory().dir("generated-resources/main");
    }

    public Provider<Directory> getDestination() {
        return destination;
    }

    @TaskAction
    void generate() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        var versionsMap = getSupportedVersions().get()
            .stream().collect(Collectors.toMap(AndroidVersionDomainObject::getName, v -> v.getGradleVersions().get()));
        Files.write(destination.get().file("versions.json").getAsFile().toPath(),
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(
                Map.of("version", getVersion().get(), "supportedVersions", versionsMap)));
    }

}
