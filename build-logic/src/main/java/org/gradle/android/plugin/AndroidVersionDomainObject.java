package org.gradle.android.plugin;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;
import java.io.Serializable;

public abstract class AndroidVersionDomainObject implements Serializable {
    final String name;

    @Inject
    public AndroidVersionDomainObject(String name) {
        this.name = name;
    }

    @Input
    public String getName() {
        return name;
    }

    @Input
    public abstract ListProperty<String> getGradleVersions();
}
