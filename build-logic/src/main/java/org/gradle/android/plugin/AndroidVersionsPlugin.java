package org.gradle.android.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import java.util.ArrayList;

public class AndroidVersionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        ObjectFactory objects = project.getObjects();
        var androidVersionsContainer = objects.domainObjectContainer(AndroidVersionDomainObject.class,
            name -> objects.newInstance(AndroidVersionDomainObject.class, name));
        project.getExtensions().add("androidVersions", androidVersionsContainer);
        var generateTask = project.getTasks().register("generateVersions", GenerateVersionsTask.class);
        generateTask.configure(t -> {
            t.getSupportedVersions().set(androidVersionsContainer);
            t.getVersion().set((String) project.getVersion());
        });

        var sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        var main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getOutput().dir(generateTask);

        // Generate a test task for each Android version and run the tests annotated with the MultiVersionTest category
        project.afterEvaluate(p -> {
            var tasks = new ArrayList<String>();
            tasks.add("test");
            androidVersionsContainer.forEach(v -> {
                var t = registerAndroidTestTask(p, v);
                tasks.add(t.getName());
            });
            project.getTasks().register("generateGithubActionsMatrix", GithubActionsMatrixTask.class).configure(t -> t.getTasks().set(tasks));
        });
    }

    private TaskProvider<Test> registerAndroidTestTask(Project project, AndroidVersionDomainObject androidVersion) {
        var versionSpecificTest = project.getTasks().register(androidTestTaskName(androidVersion.getName()), Test.class);
        versionSpecificTest.configure(t -> {
            t.setDescription("Runs the multi-version tests for AGP " + androidVersion.getName());
            t.setGroup("verification");
            t.systemProperty("org.gradle.android.testVersion", androidVersion.getName());
        });
        project.getTasks().named("check").configure(t -> t.dependsOn(versionSpecificTest));
        return versionSpecificTest;
    }

    private static String androidTestTaskName(String androidVersion) {
        return "testAndroid" + normalizeVersion(androidVersion);
    }

    private static String normalizeVersion(String version) {
        return version.replaceAll("[.\\-]", "_");
    }

}
