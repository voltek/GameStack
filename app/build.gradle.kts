import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.gamestack"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.gamestack"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "IGDB_CLIENT_ID",
            "\"${localProperties.getProperty("IGDB_CLIENT_ID", "")}\""
        )
        buildConfigField(
            "String",
            "IGDB_CLIENT_SECRET",
            "\"${localProperties.getProperty("IGDB_CLIENT_SECRET", "")}\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric needs real resources and the merged manifest; without
            // this, screen tests fail to inflate the theme.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.google.hilt.android)
    ksp(libs.google.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.cashapp.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    // Screen tests only — see CLAUDE.md, Testing Stack.
    testImplementation(composeBom)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Not androidTest-only: this is what puts ComponentActivity in the merged
    // debug manifest, which is where the JVM screen tests get their activity.
    // Removing it fails every screen test with "Unable to resolve activity".
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Best-effort: commits.yml is the guarantee, so a machine without git on the
// daemon's PATH still builds, with a warning instead of the hooks.
abstract class InstallGitHooks @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:Internal
    abstract val repositoryDir: DirectoryProperty

    @TaskAction
    fun install() {
        val hooksPath = ".githooks"
        try {
            val current = ByteArrayOutputStream()
            execOperations.exec {
                workingDir = repositoryDir.get().asFile
                commandLine("git", "config", "--get", "core.hooksPath")
                standardOutput = current
                isIgnoreExitValue = true
            }
            val previous = current.toString().trim()
            if (previous == hooksPath) return
            val ownHooks = repositoryDir.get().asFile.resolve(".git/hooks")
                .listFiles { file -> !file.name.endsWith(".sample") }.orEmpty()
            val replaced = previous.ifEmpty { ownHooks.joinToString { ".git/hooks/${it.name}" } }
            if (replaced.isNotEmpty()) {
                logger.warn("core.hooksPath set to '$hooksPath'; these hooks stop running: $replaced")
            }
            execOperations.exec {
                workingDir = repositoryDir.get().asFile
                commandLine("git", "config", "core.hooksPath", hooksPath)
            }
        } catch (e: Exception) {
            logger.warn("Git hooks not installed (${e.message}); commits.yml still checks every PR.")
        }
    }
}

// A hook binds only once installed; installing it on every build means no
// clone depends on someone remembering a setup step.
val installGitHooks by tasks.registering(InstallGitHooks::class) {
    val gitDir = rootProject.layout.projectDirectory.file(".git")
    onlyIf("the project is a git checkout") { gitDir.asFile.exists() }
    repositoryDir.set(rootProject.layout.projectDirectory)
}

tasks.named("preBuild") { dependsOn(installGitHooks) }
