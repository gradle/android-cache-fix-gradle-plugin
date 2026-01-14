import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * This Gradle script captures the system properties available to each Test task, hashes the properties' values,
 * and adds these as custom values.
 */

project.extensions.configure<DevelocityConfiguration>() {
    buildScan {
        val api = buildScan
        allprojects {
            tasks.withType<Test>().configureEach {
                doFirst {
                    systemProperties.forEach { (k, v) -> Capture.addbuildScanValue(api, "${identityPath}#sysProps-${k}", v) }
                }
            }
        }
    }
}

class Capture {
    companion object {
        val MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256")

        fun addbuildScanValue(api: BuildScanConfiguration, key: String, value: Any?) {
            api.value(key, hash(value))
        }

        private fun hash(value: Any?): String? {
            if (value == null) {
                return null
            } else {
                val str = java.lang.String.valueOf(value)
                val encodedHash = MESSAGE_DIGEST.digest(str.toByteArray())
                val hexString = StringBuilder()
                for (i in 0 until (encodedHash.size / 4)) {
                    val hex = java.lang.Integer.toHexString(0xff and encodedHash[i].toInt())
                    if (hex.length == 1) {
                        hexString.append("0")
                    }
                    hexString.append(hex)
                }
                hexString.append("...")
                return hexString.toString()
            }
        }
    }
}

