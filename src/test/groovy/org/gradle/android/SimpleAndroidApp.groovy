package org.gradle.android

class SimpleAndroidApp {
    final File projectDir
    private final File cacheDir
    final String androidVersion
    private final boolean applyBeforeAndroidPlugin

    SimpleAndroidApp(File projectDir, File cacheDir, String androidVersion, boolean applyBeforeAndroidPlugin = false) {
        this.applyBeforeAndroidPlugin = applyBeforeAndroidPlugin
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

    private subprojectConfiguration(String androidPlugin) {
        def applyPlugins
        def applyAndroid = "apply plugin: \"$androidPlugin\""
        if (applyBeforeAndroidPlugin) {
            applyPlugins = """
                    apply plugin: "org.gradle.android.cache-fix"
                    $applyAndroid
                """
        } else {
            applyPlugins = """
                    $applyAndroid
                    apply plugin: "org.gradle.android.cache-fix"
                """
        }
        applyPlugins + """
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
