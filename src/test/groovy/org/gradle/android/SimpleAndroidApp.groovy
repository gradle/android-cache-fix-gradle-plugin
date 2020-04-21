package org.gradle.android

import org.gradle.util.VersionNumber

import static org.gradle.android.Versions.android

class SimpleAndroidApp {
    final File projectDir
    private final File cacheDir
    final VersionNumber androidVersion
    private final boolean dataBindingEnabled
    private final boolean kotlinEnabled

    private SimpleAndroidApp(File projectDir, File cacheDir, VersionNumber androidVersion, boolean dataBindingEnabled, boolean kotlinEnabled) {
        this.dataBindingEnabled = dataBindingEnabled
        this.projectDir = projectDir
        this.cacheDir = cacheDir
        this.androidVersion = androidVersion
        this.kotlinEnabled = kotlinEnabled
    }

    def writeProject() {
        def app = 'app'
        def appPackage = 'org.gradle.android.example.app'
        def appActivity = 'AppActivity'

        def library = 'library'
        def libPackage = 'org.gradle.android.example.library'
        def libraryActivity = 'LibraryActivity'

        file("settings.gradle") << """
                buildCache {
                    local(DirectoryBuildCache) {
                        directory = "${cacheDir.absolutePath.replace(File.separatorChar, '/' as char)}"
                    }
                }
            """.stripIndent()

        file("build.gradle") << """
                buildscript {
                    repositories {
                        google()
                        jcenter()
                        ivy {
                            url = "${System.getProperty("local.repo")}"
                        }
                    }
                    dependencies {
                        classpath ('com.android.tools.build:gradle:$androidVersion') { force = true }
                        classpath "org.gradle.android:android-cache-fix-gradle-plugin:${Versions.PLUGIN_VERSION}"
                        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71"
                    }
                }
            """.stripIndent()

        writeActivity(library, libPackage, libraryActivity)
        file("${library}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${libPackage}">
                </manifest>
            """.stripIndent()

        writeActivity(app, appPackage, appActivity)
        file("${app}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${appPackage}">

                    <application android:label="@string/app_name" >
                        <activity
                            android:name=".${appActivity}"
                            android:label="@string/app_name" >
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                        <activity
                            android:name="${libPackage}.${libraryActivity}">
                        </activity>
                    </application>

                </manifest>
            """.stripIndent()
        file("${app}/src/main/res/values/strings.xml") << '''<?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="app_name">Android Gradle</string>
                </resources>'''.stripIndent()

        file('settings.gradle') << """
                include ':${app}'
                include ':${library}'
            """.stripIndent()

        file("${app}/build.gradle") << subprojectConfiguration("com.android.application") << """
                android.defaultConfig.applicationId "org.gradle.android.test.app"
            """.stripIndent() << activityDependency() <<
            """
                dependencies {
                    implementation project(':${library}')
                }
            """.stripIndent()

        file("${library}/build.gradle") << subprojectConfiguration("com.android.library") << activityDependency()

        file("gradle.properties") << """
                android.useAndroidX=true
            """.stripIndent()

        configureAndroidSdkHome()
    }

    private subprojectConfiguration(String androidPlugin) {
        """
            apply plugin: "$androidPlugin"
            ${kotlinPluginsIfEnabled}
            apply plugin: "org.gradle.android.cache-fix"

            repositories {
                google()
                jcenter()
            }

            dependencies {
                def room_version = "2.2.5"

                implementation "androidx.room:room-runtime:\$room_version"
                annotationProcessor "androidx.room:room-compiler:\$room_version"
                ${kotlinDependenciesIfEnabled}
            }

            android {
                compileSdkVersion 28
                buildToolsVersion "29.0.3"
                dataBinding.enabled = $dataBindingEnabled
                defaultConfig {
                    minSdkVersion 28
                    targetSdkVersion 28

                    javaCompileOptions {
                        annotationProcessorOptions {
                            arguments = ["room.schemaLocation":
                                 "\${projectDir}/schemas".toString()]
                        }
                    }
                }
            }

            ${renderscriptConfiguration}
        """.stripIndent()
    }

    private String getKotlinPluginsIfEnabled() {
        return kotlinEnabled ? """
            apply plugin: "kotlin-android"
            apply plugin: "kotlin-kapt"
        """ : ""
    }

    private String getKotlinDependenciesIfEnabled() {
        return kotlinEnabled ? """
            kapt "androidx.room:room-compiler:\$room_version"
            implementation "org.jetbrains.kotlin:kotlin-stdlib"
        """ : ""
    }

    /**
     * The following is the result of a descent into madness when trying to reproduce
     * https://issuetracker.google.com/issues/140602655 across all android versions.  The affected property on
     * MergeNativeLibsTask only has a value if there are native libraries produced by this project or if the
     * project compiles RenderScript with support mode enabled.  The property can be directly changed on some versions
     * of android but not others.  Building native libraries as part of the project has external infrastructure
     * requirements in that versions of cmake and ndk must be installed on the system or the test will fail. Adding
     * RenderScript compilation to the project is the simplest way to trigger the problem, but support mode does not
     * work reliably on all versions of android with MacOS Catalina (because of 32-bit support).  Thus, the following
     * hack was born.  We enable support mode in order to trick AGP into setting up the affected property on
     * MergeNativeLibsTask, then we disable it before execution (in order to avoid runtime errors related to 32-bit
     * support) and we provide a dummy library for the downstream task to "merge".  This reliably triggers the
     * cache relocation problem on all android versions.
     */
    private static String getRenderscriptConfiguration() {
        return '''
            android {
                defaultConfig {
                    renderscriptTargetApi 18
                    renderscriptSupportModeEnabled true
                }
            }
            tasks.withType(com.android.build.gradle.tasks.RenderscriptCompile).configureEach {
                doFirst {
                    supportMode = false
                }
                doLast {
                    new File(libOutputDir.get().asFile, "test.so") << ''
                    supportMode = true
                }
            }
        '''.stripIndent()
    }

    private writeActivity(String basedir, String packageName, String className) {
        String resourceName = className.toLowerCase()

        file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/HelloActivity.java") << """
                package ${packageName};

                import org.joda.time.LocalTime;

                import android.app.Activity;
                import android.os.Bundle;
                import android.widget.TextView;

                public class HelloActivity extends Activity {

                    @Override
                    public void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.${resourceName}_layout);
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                        LocalTime currentTime = new LocalTime();
                        TextView textView = (TextView) findViewById(R.id.text_view);
                        textView.setText("The current local time is: " + currentTime);
                    }
                }
            """.stripIndent()

        if (kotlinEnabled) {
            file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/Utility.kt") << """
                    package ${packageName};

                    class Utility { }
                """.stripIndent()
        }

        file("${basedir}/src/main/res/layout/${resourceName}_layout.xml") << '''<?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:orientation="vertical"
                    android:layout_width="fill_parent"
                    android:layout_height="fill_parent"
                    >
                <TextView
                    android:id="@+id/text_view"
                    android:layout_width="fill_parent"
                    android:layout_height="wrap_content"
                    />
                </LinearLayout>
            '''.stripIndent()

        file("${basedir}/src/main/rs/${resourceName}.rs") << '''
                #pragma version(1)
                #pragma rs java_package_name(com.example.myapplication)

                static void addintAccum(int *accum, int val) {
                  *accum += val;
                }
            '''.stripIndent()
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
        VersionNumber androidVersion = Versions.latestAndroidVersion()
        File projectDir
        File cacheDir

        Builder(File projectDir, File cacheDir) {
            this.projectDir = projectDir
            this.cacheDir = cacheDir
        }

        Builder withoutDataBindingEnabled() {
            this.dataBindingEnabled = false
            return this
        }

        Builder withoutKotlinEnabled() {
            this.kotlinEnabled = false
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

        Builder withCacheDir(File cacheDir) {
            this.cacheDir = cacheDir
            return this
        }

        SimpleAndroidApp build() {
            return new SimpleAndroidApp(projectDir, cacheDir, androidVersion, dataBindingEnabled, kotlinEnabled)
        }
    }
}
