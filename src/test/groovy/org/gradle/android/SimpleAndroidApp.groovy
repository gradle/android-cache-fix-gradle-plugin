package org.gradle.android

import org.gradle.api.JavaVersion

import java.nio.file.Paths

import static org.gradle.android.Versions.android

class SimpleAndroidApp {
    public static final ROOM_LIBRARY_VERSION = "2.4.1"
    public static final String PLUGIN_VERSION_SYSTEM_PROPERTY = 'org.gradle.android.cache-fix.version'
    private static final String PLUGIN_GROUP_ID_SYSTEM_PROPERTY = "pluginGroupId"
    final File projectDir
    private final File cacheDir
    final VersionNumber androidVersion
    final VersionNumber kotlinVersion
    private final boolean dataBindingEnabled
    private final boolean kotlinEnabled
    private final boolean kaptWorkersEnabled
    private final RoomConfiguration roomConfiguration
    private final String toolchainVersion
    // TODO fix this in builder
    private JavaVersion sourceCompatibility
    private final boolean pluginsBlockEnabled
    private final boolean pluginAppliedInPluginBlock

    private SimpleAndroidApp(File projectDir, File cacheDir, VersionNumber androidVersion, VersionNumber kotlinVersion, boolean dataBindingEnabled, boolean kotlinEnabled, boolean kaptWorkersEnabled, RoomConfiguration roomConfiguration, String toolchainVersion, JavaVersion sourceCompatibility, boolean pluginsBlockEnabled, boolean pluginAppliedInPluginBlock) {
        this.dataBindingEnabled = dataBindingEnabled
        this.projectDir = projectDir
        this.cacheDir = cacheDir
        this.androidVersion = androidVersion
        this.kotlinVersion = kotlinVersion
        this.kotlinEnabled = kotlinEnabled
        this.kaptWorkersEnabled = kaptWorkersEnabled
        this.roomConfiguration = roomConfiguration
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
        writeRoomSourcesIfEnabled(library, libPackage)
        file("${library}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                </manifest>
            """.stripIndent()

        writeActivity(app, appPackage, appActivity)
        writeRoomSourcesIfEnabled(app, appPackage)
        file("${app}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">

                    <application android:label="@string/app_name" >
                        <activity
                            android:name=".${appActivity}"
                            android:label="@string/app_name"
                            android:exported="false">
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
                org.gradle.jvmargs=-Xmx2048m -Dkotlin.daemon.jvm.options=-Xmx1500m
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
        """ : ""
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
                ${roomRuntimeLibraryIfEnabled}
                ${roomProcessorsIfEnabled}
                ${kotlinDependenciesIfEnabled}
            }

            android {
                namespace "$namespace"
                ndkVersion "20.0.5594570"
                compileSdkVersion 32
                dataBinding.enabled = $dataBindingEnabled
                ${sourceCompatibilityIfEnabled}
                defaultConfig {
                    minSdkVersion 28
                    targetSdkVersion 32

                    ${roomAnnotationProcessorArgumentIfEnabled}

                    lintOptions {
                        checkReleaseBuilds false
                    }
                }
            }

            ${roomExtensionIfEnabled}

            ${toolchainConfigurationIfEnabled}
        """.stripIndent()
    }

    private String getRoomRuntimeLibraryIfEnabled() {
        return (roomConfiguration != RoomConfiguration.NO_LIBRARY) ? """
                implementation "androidx.room:room-runtime:${ROOM_LIBRARY_VERSION}"
        """ : ""
    }

    private String getRoomExtensionIfEnabled() {
        return (roomConfiguration == RoomConfiguration.ROOM_EXTENSION) ? """
            room {
                schemaLocationDir = file("schemas")
            }
        """ : ""
    }

    private String getRoomAnnotationProcessorArgumentIfEnabled() {
        return (roomConfiguration == RoomConfiguration.PROCESSOR_ARG) ? """
                    javaCompileOptions {
                        annotationProcessorOptions {
                            arguments = ["room.schemaLocation":
                                 "\${projectDir}/schemas".toString()]
                        }
                    }
        """ : ""
    }

    private String getKotlinPluginsIfEnabled() {
        return kotlinEnabled ? """
            apply plugin: "kotlin-android"
            apply plugin: "kotlin-kapt"
        """ : ""
    }

    private String getKotlinDependenciesIfEnabled() {
        return kotlinEnabled ? """
            implementation "org.jetbrains.kotlin:kotlin-stdlib"
        """ : ""
    }

    private String getRoomProcessorsIfEnabled() {
        return roomConfiguration != RoomConfiguration.NO_LIBRARY ?
            kotlinEnabled ? """ kapt "androidx.room:room-compiler:${ROOM_LIBRARY_VERSION}" """
                : """ annotationProcessor "androidx.room:room-compiler:${ROOM_LIBRARY_VERSION}" """
            : """ """
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
        // We need to set the source compatibility when the kotlin plugin is applied:
        // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
        if (kotlinEnabled && sourceCompatibility == null)  {
            sourceCompatibility = JavaVersion.current()
        }
        return (sourceCompatibility != null) ? """
            compileOptions {
                sourceCompatibility JavaVersion.${sourceCompatibility.name()}
                targetCompatibility JavaVersion.${sourceCompatibility.name()}
            }
        """ : ""
    }

    private writeKotlinClass(String basedir, String packageName, String className) {
        file("${basedir}/src/main/kotlin/${packageName.replaceAll('\\.', '/')}/Foo.kt") << """
                package ${packageName}

                data class Foo(val lable: String)

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

        file("${basedir}/src/test/java/${packageName.replaceAll('\\.', '/')}/JavaUserTest.java") << """
                package ${packageName};

                public class JavaUserTest {
                }
            """.stripIndent()

        file("${basedir}/src/androidTest/java/${packageName.replaceAll('\\.', '/')}/JavaUserAndroidTest.java") << """
                package ${packageName};

                public class JavaUserAndroidTest {
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

        file("${basedir}/src/main/rs/${resourceName}.rs") << '''
                #pragma version(1)
                #pragma rs java_package_name(com.example.myapplication)

                static void addintAccum(int *accum, int val) {
                  *accum += val;
                }
            '''.stripIndent()
    }

    private void writeRoomSourcesIfEnabled(String basedir, String packageName) {
        if (roomConfiguration != RoomConfiguration.NO_LIBRARY) {
            file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/JavaUser.java") << """
                package ${packageName};

                import androidx.room.ColumnInfo;
                import androidx.room.Entity;
                import androidx.room.PrimaryKey;

                @Entity(tableName = "user")
                public class JavaUser {
                    @PrimaryKey
                    public int uid;

                    @ColumnInfo(name = "first_name")
                    public String firstName;

                    @ColumnInfo(name = "last_name")
                    public String lastName;

                    @ColumnInfo(name = "last_update")
                    public int lastUpdate;
                }
            """.stripIndent()

            file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/JavaUserDao.java") << """
                package ${packageName};

                import androidx.room.Dao;
                import androidx.room.Query;
                import androidx.room.Insert;
                import androidx.room.Delete;

                import java.util.List;

                @Dao
                public interface JavaUserDao {
                    @Query("SELECT * FROM user")
                    List<JavaUser> getAll();

                    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
                    List<JavaUser> loadAllByIds(int[] userIds);

                    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
                           "last_name LIKE :last LIMIT 1")
                    JavaUser findByName(String first, String last);

                    @Insert
                    void insertAll(JavaUser... users);

                    @Delete
                    void delete(JavaUser user);
                }
            """.stripIndent()

            file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/AppDatabase.java") << """
                package ${packageName};

                import androidx.room.Database;
                import androidx.room.Room;
                import androidx.room.RoomDatabase;
                import androidx.room.migration.Migration;
                import androidx.sqlite.db.SupportSQLiteDatabase;
                import android.content.Context;

                @Database(entities = {JavaUser.class}, version = 2, exportSchema = true)
                public abstract class AppDatabase extends RoomDatabase {
                    private static AppDatabase INSTANCE;
                    private static final Object sLock = new Object();

                    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
                        @Override
                        public void migrate(SupportSQLiteDatabase database) {
                            database.execSQL("ALTER TABLE Users "
                                    + " ADD COLUMN last_update INTEGER");
                        }
                    };

                    public abstract JavaUserDao javaUserDao();

                    public static AppDatabase getInstance(Context context) {
                        synchronized (sLock) {
                            if (INSTANCE == null) {
                                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "Sample.db")
                                        .addMigrations(MIGRATION_1_2)
                                        .build();
                            }
                            return INSTANCE;
                        }
                    }
                }
            """.stripIndent()

            file("${basedir}/schemas/${packageName}.AppDatabase/1.json") << legacySchemaContents
        }
    }

    static String getLegacySchemaContents() {
        return '''
                {
                  "formatVersion": 1,
                  "database": {
                    "version": 1,
                    "identityHash": "ce7bbbf6ddf39482eddc7248f4f61e8a",
                    "entities": [
                      {
                        "tableName": "user",
                        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`uid` INTEGER NOT NULL, `first_name` TEXT, `last_name` TEXT, PRIMARY KEY(`uid`))",
                        "fields": [
                          {
                            "fieldPath": "uid",
                            "columnName": "uid",
                            "affinity": "INTEGER",
                            "notNull": true
                          },
                          {
                            "fieldPath": "firstName",
                            "columnName": "first_name",
                            "affinity": "TEXT",
                            "notNull": false
                          },
                          {
                            "fieldPath": "lastName",
                            "columnName": "last_name",
                            "affinity": "TEXT",
                            "notNull": false
                          }
                        ],
                        "primaryKey": {
                          "columnNames": [
                            "uid"
                          ],
                          "autoGenerate": false
                        },
                        "indices": [],
                        "foreignKeys": []
                      }
                    ],
                    "views": [],
                    "setupQueries": [
                      "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                      "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ce7bbbf6ddf39482eddc7248f4f61e8a')"
                    ]
                  }
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

    enum RoomConfiguration {
        NONE, ROOM_EXTENSION, PROCESSOR_ARG, NO_LIBRARY
    }

    static class Builder {
        boolean dataBindingEnabled = true
        boolean kotlinEnabled = true
        boolean kaptWorkersEnabled = true
        boolean pluginsBlockEnabled = false
        boolean pluginAppliedInPluginBlock = false
        RoomConfiguration roomConfiguration = RoomConfiguration.ROOM_EXTENSION

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

        Builder withDataBindingDisabled() {
            this.dataBindingEnabled = false
            return this
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

        Builder withRoomProcessingArgumentConfigured() {
            this.roomConfiguration = RoomConfiguration.PROCESSOR_ARG
            return this
        }

        Builder withNoRoomConfiguration() {
            this.roomConfiguration = RoomConfiguration.NONE
            return this
        }

        Builder withNoRoomLibrary() {
            this.roomConfiguration = RoomConfiguration.NO_LIBRARY
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

        SimpleAndroidApp build() {
            return new SimpleAndroidApp(projectDir, cacheDir, androidVersion, kotlinVersion, dataBindingEnabled, kotlinEnabled, kaptWorkersEnabled, roomConfiguration, toolchainVersion, sourceCompatibility, pluginsBlockEnabled, pluginAppliedInPluginBlock)
        }
    }
}
