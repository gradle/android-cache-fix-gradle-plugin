package org.gradle.android.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

public abstract class GithubActionsMatrixTask extends DefaultTask {
    @Input
    abstract ListProperty<String> getTasks();

    public GithubActionsMatrixTask() {
    }

    @TaskAction
    void generate() throws IOException {
        getLogger().lifecycle("::set-output name=matrix::" + new ObjectMapper().writeValueAsString(getTasks().get()));
    }
}
