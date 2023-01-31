package org.gradle.android.workarounds.room

import org.gradle.android.VersionNumber
import org.gradle.android.workarounds.RoomSchemaLocationWorkaround

class KotlinVersion {

    static VersionNumber get() {
        def projectPropertiesStream = RoomSchemaLocationWorkaround.class.classLoader.getResourceAsStream("project.properties")
        if (projectPropertiesStream != null) {
            def projectProperties = new Properties()
            projectProperties.load(projectPropertiesStream)
            if (projectProperties.containsKey("project.version")) {
                return VersionNumber.parse(projectProperties.getProperty("project.version"))
            }
        }

        return VersionNumber.UNKNOWN
    }
}
