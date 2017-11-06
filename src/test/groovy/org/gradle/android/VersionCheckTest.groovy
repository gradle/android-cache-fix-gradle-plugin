package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE

class VersionCheckTest extends Specification {
    @Rule TemporaryFolder temporaryFolder
    File cacheDir

    def setup() {
        cacheDir = temporaryFolder.newFolder()
        println "Using version ${Versions.PLUGIN_VERSION} of the plugin"
    }

    @Unroll
    def "works with #gradleVersion and Android plugin #androidVersion"() {
        def originalDir = temporaryFolder.newFolder()
        new AndroidProject(originalDir, cacheDir, androidVersion.toString()).writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        new AndroidProject(relocatedDir, cacheDir, androidVersion.toString()).writeProject()

        def gradleVerString = gradleVersion.version

        when:
        def result = withGradleVersion(gradleVerString)
            .withProjectDir(originalDir)
            .withArguments("assemble", "--build-cache")
            .build()

        then:
        !result.output.contains("not applying workarounds")

        when:
        result = withGradleVersion(gradleVerString)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "--build-cache")
            .build()

        then:
        verifyAll {
            result.task(":app:checkDebugManifest").outcome == FROM_CACHE
            result.task(":app:checkDebugManifest").outcome == FROM_CACHE
            result.task(":app:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":app:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":app:compileDebugAidl").outcome == FROM_CACHE
            result.task(":app:compileDebugJavaWithJavac").outcome == FROM_CACHE
            result.task(":app:compileDebugRenderscript").outcome == FROM_CACHE
            result.task(":app:compileDebugShaders").outcome == FROM_CACHE
            result.task(":app:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":app:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":app:compileReleaseJavaWithJavac").outcome == FROM_CACHE
            result.task(":app:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":app:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":app:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":app:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":app:createDebugCompatibleScreenManifests").outcome == FROM_CACHE
            result.task(":app:createReleaseCompatibleScreenManifests").outcome == FROM_CACHE
            result.task(":app:generateDebugBuildConfig").outcome == FROM_CACHE
            result.task(":app:generateDebugResValues").outcome == FROM_CACHE
            result.task(":app:generateReleaseBuildConfig").outcome == FROM_CACHE
            result.task(":app:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":app:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":app:javaPreCompileDebug").outcome == FROM_CACHE
            result.task(":app:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":app:mergeDebugAssets").outcome == FROM_CACHE
            result.task(":app:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeDebugResources").outcome == FROM_CACHE
            result.task(":app:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":app:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":app:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseResources").outcome == FROM_CACHE
            result.task(":app:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":app:preDebugBuild").outcome == FROM_CACHE
            result.task(":app:preReleaseBuild").outcome == FROM_CACHE
            result.task(":app:preReleaseBuild").outcome == FROM_CACHE
            result.task(":app:processDebugManifest").outcome == FROM_CACHE
            result.task(":app:processDebugResources").outcome == FROM_CACHE
            result.task(":app:processReleaseManifest").outcome == FROM_CACHE
            result.task(":app:processReleaseResources").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskDebug").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskRelease").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskRelease").outcome == FROM_CACHE
            // result.task(":app:transformDexArchiveWithDexMergerForDebug").outcome == FROM_CACHE
            result.task(":library:checkDebugManifest").outcome == FROM_CACHE
            result.task(":library:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":library:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":library:compileDebugAidl").outcome == FROM_CACHE
            result.task(":library:compileDebugJavaWithJavac").outcome == FROM_CACHE
            result.task(":library:compileDebugRenderscript").outcome == FROM_CACHE
            result.task(":library:compileDebugShaders").outcome == FROM_CACHE
            result.task(":library:compileDebugShaders").outcome == FROM_CACHE
            result.task(":library:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":library:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":library:compileReleaseJavaWithJavac").outcome == FROM_CACHE
            result.task(":library:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":library:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":library:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":library:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":library:generateDebugBuildConfig").outcome == FROM_CACHE
            result.task(":library:generateDebugResValues").outcome == FROM_CACHE
            result.task(":library:generateDebugResValues").outcome == FROM_CACHE
            result.task(":library:generateReleaseBuildConfig").outcome == FROM_CACHE
            result.task(":library:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":library:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":library:javaPreCompileDebug").outcome == FROM_CACHE
            result.task(":library:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":library:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":library:mergeDebugAssets").outcome == FROM_CACHE
            result.task(":library:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":library:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":library:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":library:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":library:packageDebugResources").outcome == FROM_CACHE
            result.task(":library:packageReleaseResources").outcome == FROM_CACHE
            result.task(":library:platformAttrExtractor").outcome == FROM_CACHE
            result.task(":library:processDebugManifest").outcome == FROM_CACHE
            result.task(":library:processDebugResources").outcome == FROM_CACHE
            result.task(":library:processReleaseManifest").outcome == FROM_CACHE
            result.task(":library:processReleaseManifest").outcome == FROM_CACHE
            result.task(":library:processReleaseResources").outcome == FROM_CACHE
        }

        where:
        // [gradleVersion, androidVersion] << GroovyCollections.combinations(Versions.SUPPORTED_GRADLE_VERSIONS, Versions.SUPPORTED_ANDROID_VERSIONS)
        gradleVersion = GradleVersion.version("4.1")
        androidVersion = VersionNumber.parse("3.0.0")
    }

    @Ignore("Using different Gradle versions breaks TestKit")
    def "does not apply workarounds with Gradle 4.4"() {
        def projectDir = temporaryFolder.newFolder()
        new AndroidProject(projectDir, cacheDir, "3.0.0").writeProject()
        expect:
        def result = withGradleVersion("4.4-20171105235948+0000")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .build()
        result.output.contains("Gradle 4.4 is not supported by Android cache fix plugin, not applying workarounds.")
    }

    def "does not apply workarounds with Android 3.1.0-alpha01"() {
        def projectDir = temporaryFolder.newFolder()
        new AndroidProject(projectDir, cacheDir, "3.1.0-alpha01").writeProject()
        expect:
        def result = withGradleVersion("4.1")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .build()
        result.output.contains("Android plugin 3.1.0 is not supported by Android cache fix plugin, not applying workarounds.")
    }

    def withGradleVersion(String gradleVersion) {
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withDebug(true)
    }

    static class AndroidProject {
        final File projectDir
        private final File cacheDir
        final String androidVersion

        AndroidProject(File projectDir, File cacheDir, String androidVersion) {
            this.projectDir = projectDir
            this.cacheDir = cacheDir
            this.androidVersion = androidVersion
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
            """

            file("build.gradle") << """
                buildscript {
                    repositories {
                        google()
                        jcenter()
                        mavenLocal()
                    }
                    dependencies {
                        classpath 'com.android.tools.build:gradle:$androidVersion'
                        classpath "org.gradle.android:android-cache-fix-gradle-plugin:${Versions.PLUGIN_VERSION}"
                    }
                }
            """

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
            """

            file("${app}/build.gradle") << subprojectConfiguration("com.android.application") << """
                android.defaultConfig.applicationId "org.gradle.android.test.app"
            """.stripIndent() << activityDependency() <<
            """
                dependencies {
                    implementation project(':${library}')
                }
            """.stripIndent()

            file("${library}/build.gradle") << subprojectConfiguration("com.android.library") << activityDependency()

            configureAndroidSdkHome()
        }

        private static subprojectConfiguration(String androidPlugin) {
            """
                apply plugin: "$androidPlugin"
                
                apply plugin: "org.gradle.android.cache-fix"

                repositories {
                    google()
                    jcenter()
                }
    
                android {
                    compileSdkVersion 26
                    buildToolsVersion "26.0.2"
                }
            """.stripIndent()
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
    }
}
