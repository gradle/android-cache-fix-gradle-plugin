package org.gradle.android

class CodeSnippets {

    static String getJavaActivity(String packageName, String resourceName) {
        return """
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
    }

    static String getJavaAndroidTest(String packageName) {
        return """
                package ${packageName};

                public class JavaUserAndroidTest {
                }
            """.stripIndent()
    }

    static String getJavaSimpleTest(String packageName) {
        return """
                package ${packageName};
                import org.junit.Test;

                public class JavaUserTest {

                    @Test
                    public void test() {

                    }
                }
            """.stripIndent()
    }

    static String getKotlinDataClass(String packageName) {
        return """
                package ${packageName}

                data class Foo(val lable: String)

            """.stripIndent()
    }

    static String getRs() {
        return """
                #pragma version(1)
                #pragma rs java_package_name(com.example.myapplication)

                static void addintAccum(int *accum, int val) {
                   *accum += val;
                }
        """.stripIndent()
    }

    static String getXmlGenericLayout() {
        return '''<?xml version="1.0" encoding="utf-8"?>
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

    static String getXmlManifest(String appActivity, String libPackage, String libraryActivity) {
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

    static String getXmlEmptyManifest() {
        return '''<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                </manifest>
        '''.stripIndent()
    }

    static String getXmlStrings() {
        return '''<?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="app_name">Android Gradle</string>
                </resources>
        '''.stripIndent()
    }
}
