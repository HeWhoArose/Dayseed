import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.secrets.gradle.plugin)
}

android {
    namespace = "com.example.dayseed"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.dayseed.wkqmfl"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        val resolvedGitSha: String = providers.gradleProperty("dayseedGitSha")
            .orElse(providers.environmentVariable("DAYSEED_GIT_SHA"))
            .orElse(providers.environmentVariable("GITHUB_SHA"))
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(7)
            ?: runCatching {
                val gitDir = File(rootDir, ".git")
                if (gitDir.exists()) {
                    val proc = ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
                        .directory(rootDir)
                        .redirectErrorStream(true)
                        .start()
                    val output = proc.inputStream.bufferedReader().readText().trim()
                    if (proc.waitFor() == 0 && output.matches(Regex("^[0-9a-fA-F]{7,40}$"))) {
                        output.take(7)
                    } else {
                        "unknown"
                    }
                } else {
                    "unknown"
                }
            }.getOrDefault("unknown")

        buildConfigField("String", "GIT_COMMIT_SHA", "\"$resolvedGitSha\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://maven-central.storage-download.googleapis.com/maven2"
                )
                // Windows-only test-infrastructure fix (documented in docs/28; NOT product code):
                // Robolectric sandbox directories embed "<ClassName>_<testMethodName>" under
                // java.io.tmpdir. Long method names (e.g. t1_26_to_31_v2ToV3...) push the SQLite
                // file path past the legacy Windows MAX_PATH limit of 260 characters, which
                // surfaces as SQLiteCantOpenDatabaseException "check directory permissions".
                // Redirect java.io.tmpdir to a short directory on Windows. Override the default
                // with -ProbolectricTempDir=<path> (Gradle property) or the DAYSEED_ROBOLECTRIC_TMP
                // environment variable.
                if ((System.getProperty("os.name") ?: "").contains("windows", ignoreCase = true)) {
                    val tmpDir = providers.gradleProperty("robolectricTempDir")
                        .orElse(providers.environmentVariable("DAYSEED_ROBOLECTRIC_TMP")
                            .orElse(providers.provider { "C:\\rt" }))
                        .get()
                    File(tmpDir).mkdirs()
                    it.systemProperty("java.io.tmpdir", tmpDir)
                }
            }
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
}
