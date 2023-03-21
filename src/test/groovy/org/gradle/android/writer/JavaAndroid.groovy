package org.gradle.android.writer

class JavaAndroid {

    static String activity(String packageName, String resourceName) {
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

    static String androidTest(String packageName) {
        return """
                package ${packageName};

                public class JavaUserAndroidTest {
                }
            """.stripIndent()
    }


}
