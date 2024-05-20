plugins {
    id("com.gradle.develocity") version "3.17.4"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.0.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val isCI = providers.environmentVariable("CI").isPresent

develocity {
    server = "https://ge.solutions-team.gradle.com"
    buildScan {
        uploadInBackground = !isCI
        publishing.onlyIf { it.isAuthenticated }
        obfuscation {
            ipAddresses { addresses -> addresses.map { "0.0.0.0" } }
        }
    }
}

buildCache {
    local {
        isEnabled = true
    }

    remote(develocity.buildCache) {
        isEnabled = true
        val accessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY")
        isPush = isCI && !accessKey.isNullOrEmpty()
    }
}

rootProject.name = "android-cache-fix-gradle-plugin"
