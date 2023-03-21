package org.gradle.android.writer

class XmlResources {
    static String genericLayout() {
        return """
        <?xml version="1.0" encoding="utf-8"?>
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
        """.stripIndent()
    }

    static String manifest(String appActivity, String libPackage, String libraryActivity) {
        return """<?xml version="1.0" encoding="utf-8"?>
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
    }

    static String emptyManifest() {
        return """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                </manifest>
        """.stripIndent()
    }

    static String strings() {
        return """<?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="app_name">Android Gradle</string>
                </resources>
        """.stripIndent()
    }
}
