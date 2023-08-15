package org.gradle.android

import org.gradle.api.JavaVersion

import java.nio.file.Paths

import static org.gradle.android.Versions.android

class SimpleAndroidApp {
    public static final String PLUGIN_VERSION_SYSTEM_PROPERTY = 'org.gradle.android.cache-fix.version'
    private static final String PLUGIN_GROUP_ID_SYSTEM_PROPERTY = "pluginGroupId"
    final File projectDir
    private final File cacheDir
    final VersionNumber androidVersion
    final VersionNumber kotlinVersion
    private final boolean dataBindingEnabled
    private final boolean kotlinEnabled
    private final boolean kaptWorkersEnabled
    private final String toolchainVersion
    private final JavaVersion sourceCompatibility
    private final boolean pluginsBlockEnabled
    private final boolean pluginAppliedInPluginBlock

    private SimpleAndroidApp(File projectDir, File cacheDir, VersionNumber androidVersion, VersionNumber kotlinVersion, boolean dataBindingEnabled, boolean kotlinEnabled, boolean kaptWorkersEnabled, String toolchainVersion, JavaVersion sourceCompatibility, boolean pluginsBlockEnabled, boolean pluginAppliedInPluginBlock) {
        this.dataBindingEnabled = dataBindingEnabled
        this.projectDir = projectDir
        this.cacheDir = cacheDir
        this.androidVersion = androidVersion
        this.kotlinVersion = kotlinVersion
        this.kotlinEnabled = kotlinEnabled
        this.kaptWorkersEnabled = kaptWorkersEnabled
        this.toolchainVersion = toolchainVersion
        this.sourceCompatibility = sourceCompatibility
        this.pluginsBlockEnabled = pluginsBlockEnabled
        this.pluginAppliedInPluginBlock = pluginAppliedInPluginBlock
    }

    def writeProject() {
        def app = 'app'
        def appPackage = 'org.gradle.android.example.app'
        def appActivity = 'AppActivity'

        def library = 'library'
        def libPackage = 'org.gradle.android.example.library'
        def libraryActivity = 'LibraryActivity'

        file("settings.gradle") << """
                import org.gradle.util.GradleVersion
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                        google()
                        maven {
                            url = "${localRepo}"
                        }
                    }
                }
                plugins {
                    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0' apply false
                }
                if (GradleVersion.current() >= GradleVersion.version('7.6')) {
                    apply plugin: 'org.gradle.toolchains.foojay-resolver-convention'
                }
                buildCache {
                    local {
                        directory = "${cacheDir.absolutePath.replace(File.separatorChar, '/' as char)}"
                    }
                }
            """.stripIndent()

        file("build.gradle") << """
                buildscript {
                    repositories {
                        google()
                        mavenCentral()
                        maven {
                            url = "${localRepo}"
                        }
                    }
                    dependencies {
                        classpath ('com.android.tools.build:gradle') { version { strictly '$androidVersion' } }
                        ${pluginBuildScriptClasspathConfiguration}
                        ${kotlinPluginDependencyIfEnabled}
                    }
                }
                ${pluginBlockConfiguration}
            """.stripIndent()
        if (kotlinEnabled) {
            writeKotlinClass(library, libPackage, libraryActivity)
            writeKotlinClass(app, appPackage, appActivity)
        }
        writeActivity(library, libPackage, libraryActivity)
        file("${library}/src/main/AndroidManifest.xml") << CodeSnippets.getXmlEmptyManifest()

        writeActivity(app, appPackage, appActivity)
        file("${app}/src/main/AndroidManifest.xml") << CodeSnippets.getXmlManifest(appActivity, libPackage, libraryActivity)

        file("${app}/src/main/res/values/strings.xml") << CodeSnippets.getXmlStrings()

        file('settings.gradle') << """
                include ':${app}'
                include ':${library}'
            """.stripIndent()

        file("${app}/build.gradle") << subprojectConfiguration("com.android.application", appPackage) << """
                android.defaultConfig.applicationId "org.gradle.android.test.app"
            """.stripIndent() << activityDependency() <<
            """
                dependencies {
                    implementation project(':${library}')
                }
            """.stripIndent()

        file("${library}/build.gradle") << subprojectConfiguration("com.android.library", libPackage) << activityDependency()

        file("gradle.properties") << """
                android.useAndroidX=true
                org.gradle.jvmargs=-Xmx1536m -Dkotlin.daemon.jvm.options=-Xmx768m,-Xms256m
                kapt.use.worker.api=${kaptWorkersEnabled}
                android.experimental.enableSourceSetPathsMap=true
                android.experimental.cacheCompileLibResources=true
                android.defaults.buildfeatures.renderscript=false
            """.stripIndent()

        configureAndroidSdkHome()
    }

    private String getPluginBlockConfiguration() {
        return pluginsBlockEnabled ? """
                    plugins{
                        id 'org.gradle.android.cache-fix' version '$pluginVersion' $applyPluginInBlock
                    }
                """.stripIndent() : ""
    }

    private String getApplyPluginInBlock() {
        return pluginAppliedInPluginBlock ? "" : " apply false "
    }

    private String getPluginBuildScriptClasspathConfiguration() {
        return pluginsBlockEnabled ? "" : """
                classpath "${pluginGroupId}:android-cache-fix-gradle-plugin:${pluginVersion}"
            """.stripIndent()
    }

    static String getPluginVersion() {
        def pluginVersion = System.getProperty(PLUGIN_VERSION_SYSTEM_PROPERTY)
        if (pluginVersion == null) {
            throw new IllegalStateException("The '${PLUGIN_VERSION_SYSTEM_PROPERTY}' system property must be set in order to apply the plugin under test!")
        }
        return pluginVersion
    }

    static String getPluginGroupId() {
        return System.getProperty(PLUGIN_GROUP_ID_SYSTEM_PROPERTY)
    }

    static String getLocalRepo() {
        return Paths.get(System.getProperty("local.repo")).toUri()
    }

    private String getKotlinPluginDependencyIfEnabled() {
        return kotlinEnabled ? """
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
        """.stripIndent() : ""
    }

    private subprojectConfiguration(String androidPlugin, String namespace) {
        """
            apply plugin: "$androidPlugin"
            ${kotlinPluginsIfEnabled}
            apply plugin: "org.gradle.android.cache-fix"

            repositories {
                google()
                mavenCentral()
            }

            dependencies {
                ${kotlinDependenciesIfEnabled}
            }

            android {
                namespace "$namespace"
                ndkVersion "20.0.5594570"
                compileSdkVersion 33
                dataBinding.enabled = $dataBindingEnabled
                ${sourceCompatibilityIfEnabled}
                defaultConfig {
                    minSdkVersion 28
                    targetSdkVersion 33

                    lintOptions {
                        checkReleaseBuilds false
                    }
                }
            }

            ${toolchainConfigurationIfEnabled}
        """.stripIndent()
    }

    private String getKotlinPluginsIfEnabled() {
        return kotlinEnabled ? """
            apply plugin: "kotlin-android"
            ${processor}
        """ : ""
    }

    private String getProcessor() {
        return """
          apply plugin: "kotlin-kapt"
        """.stripIndent()
    }

    private String getKotlinDependenciesIfEnabled() {
        return kotlinEnabled ? """
            implementation "org.jetbrains.kotlin:kotlin-stdlib"
        """.stripIndent() : ""
    }

    private String getToolchainConfigurationIfEnabled() {
        return (toolchainVersion != null) ? """
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(${toolchainVersion}))
                }
            }

            tasks.withType(JavaCompile).configureEach {
                javaCompiler = javaToolchains.compilerFor(java.toolchain)
            }
        """ : ""
    }

    private String getSourceCompatibilityIfEnabled() {
        def currentSourceCompatibility = sourceCompatibility
        // We need to set the source compatibility when the Kotlin plugin is applied and using AGP 7.4+
        // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
        if (kotlinEnabled && currentSourceCompatibility == null) {
            currentSourceCompatibility = JavaVersion.current()
        }
        return (currentSourceCompatibility != null) ? """
            compileOptions {
                sourceCompatibility JavaVersion.${currentSourceCompatibility.name()}
                targetCompatibility JavaVersion.${currentSourceCompatibility.name()}
            }
        """ : ""
    }

    private writeKotlinClass(String basedir, String packageName, String className) {
        file("${basedir}/src/main/kotlin/${packageName.replaceAll('\\.', '/')}/Foo.kt") <<
            CodeSnippets.getKotlinDataClass(packageName)
    }

    private writeActivity(String basedir, String packageName, String className) {
        String resourceName = className.toLowerCase()

        file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/HelloActivity.java") <<
            CodeSnippets.getJavaActivity(packageName, resourceName)

        file("${basedir}/src/test/java/${packageName.replaceAll('\\.', '/')}/JavaUserTest.java") <<
            CodeSnippets.getJavaSimpleTest(packageName)

        file("${basedir}/src/androidTest/java/${packageName.replaceAll('\\.', '/')}/JavaUserAndroidTest.java") <<
            CodeSnippets.getJavaAndroidTest(packageName)

        file("${basedir}/src/main/res/layout/${resourceName}_layout.xml") << CodeSnippets.getXmlGenericLayout()

        file("${basedir}/src/main/rs/${resourceName}.rs") << CodeSnippets.getRs()
    }

    private static String activityDependency() {
        """
            dependencies {
                implementation 'joda-time:joda-time:2.7'
            }
        """.stripIndent()
    }

    private void configureAndroidSdkHome() {
        def env = System.getenv("ANDROID_HOME")
        if (!env) {
            def androidSdkHome = new File("${System.getProperty("user.home")}/Library/Android/sdk")
            file('local.properties').text = "sdk.dir=${androidSdkHome.absolutePath.replace(File.separatorChar, '/' as char)}"
        }
    }

    def file(String path) {
        def file = new File(projectDir, path)
        file.parentFile.mkdirs()
        return file
    }

    static Builder builder(File projectDir, File cacheDir) {
        return new Builder(projectDir, cacheDir)
    }

    static class Builder {
        boolean dataBindingEnabled = true
        boolean kotlinEnabled = true
        boolean kaptWorkersEnabled = true
        boolean pluginsBlockEnabled = false
        boolean pluginAppliedInPluginBlock = false

        VersionNumber androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        VersionNumber kotlinVersion = TestVersions.latestSupportedKotlinVersion()

        String toolchainVersion
        JavaVersion sourceCompatibility

        File projectDir
        File cacheDir

        Builder(File projectDir, File cacheDir) {
            this.projectDir = projectDir
            this.cacheDir = cacheDir
        }

        Builder withKotlinDisabled() {
            this.kotlinEnabled = false
            return this
        }

        Builder withKotlinVersion(VersionNumber kotlinVersion) {
            this.kotlinVersion = kotlinVersion
            return this
        }

        Builder withKaptWorkersDisabled() {
            this.kaptWorkersEnabled = false
            return this
        }

        Builder withAndroidVersion(VersionNumber androidVersion) {
            this.androidVersion = androidVersion
            return this
        }

        Builder withAndroidVersion(String androidVersion) {
            return withAndroidVersion(android(androidVersion))
        }

        Builder withProjectDir(File projectDir) {
            this.projectDir = projectDir
            return this
        }

        Builder withToolchainVersion(String toolchainVersion) {
            this.toolchainVersion = toolchainVersion
            return this
        }

        Builder withSourceCompatibility(JavaVersion sourceCompatibility) {
            this.sourceCompatibility = sourceCompatibility
            return this
        }

        Builder withPluginsBlockEnabled() {
            this.pluginsBlockEnabled = true
            return this
        }

        Builder withPluginsBlockEnabledApplyingThePlugin() {
            this.pluginsBlockEnabled = true
            this.pluginAppliedInPluginBlock = true
            return this
        }

        Builder withDatabindingDisabled() {
            this.dataBindingEnabled = false
            return this
        }

        SimpleAndroidApp build() {
            return new SimpleAndroidApp(projectDir, cacheDir, androidVersion, kotlinVersion, dataBindingEnabled, kotlinEnabled, kaptWorkersEnabled, toolchainVersion, sourceCompatibility, pluginsBlockEnabled, pluginAppliedInPluginBlock)
        }
    }
}
