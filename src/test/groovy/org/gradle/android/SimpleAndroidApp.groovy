package org.gradle.android

import org.gradle.util.VersionNumber

import static org.gradle.android.Versions.android

class SimpleAndroidApp {
    final File projectDir
    private final File cacheDir
    final VersionNumber androidVersion
    final VersionNumber kotlinVersion
    private final boolean dataBindingEnabled
    private final boolean kotlinEnabled
    private final boolean kaptWorkersEnabled

    private SimpleAndroidApp(File projectDir, File cacheDir, VersionNumber androidVersion, VersionNumber kotlinVersion, boolean dataBindingEnabled, boolean kotlinEnabled, boolean kaptWorkersEnabled) {
        this.dataBindingEnabled = dataBindingEnabled
        this.projectDir = projectDir
        this.cacheDir = cacheDir
        this.androidVersion = androidVersion
        this.kotlinVersion = kotlinVersion
        this.kotlinEnabled = kotlinEnabled
        this.kaptWorkersEnabled = kaptWorkersEnabled
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
                        ${kotlinPluginDependencyIfEnabled}
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
                org.gradle.jvmargs=-Xmx2048m
                kapt.use.worker.api=${kaptWorkersEnabled}
            """.stripIndent()

        configureAndroidSdkHome()
    }

    private String getKotlinPluginDependencyIfEnabled() {
        return kotlinEnabled ? """
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
        """ : ""
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
                ndkVersion "20.0.5594570"
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

        file("${basedir}/schemas/${packageName}.AppDatabase/1.json") << '''
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
        boolean kaptWorkersEnabled = true
        VersionNumber androidVersion = Versions.latestAndroidVersion()
        VersionNumber kotlinVersion = VersionNumber.parse("1.3.72")
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
            return new SimpleAndroidApp(projectDir, cacheDir, androidVersion, kotlinVersion, dataBindingEnabled, kotlinEnabled, kaptWorkersEnabled)
        }
    }
}
