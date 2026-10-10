import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
    id("maven-publish")
}

val sdkVersion = providers.gradleProperty("SDK_VERSION").get()

// Writes the SDK version into a Kotlin source file, so the version is available to the code
// without publishing a public BuildConfig class in the SDK.
abstract class GenerateSdkVersionTask : DefaultTask() {
    @get:Input
    abstract val versionName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile.resolve("io/github/dokuendev/dokuenreader/plugin/core")
        dir.mkdirs()
        dir.resolve("GeneratedSdkVersion.kt").writeText(
            """
            package io.github.dokuendev.dokuenreader.plugin.core

            internal const val GENERATED_SDK_VERSION = "${versionName.get()}"
            """.trimIndent() + "\n"
        )
    }
}

configure<LibraryExtension> {
    namespace = "io.github.dokuendev.dokuenreader.plugin.core"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        aidl = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// Copy AIDL files to the location where dependent modules expect them
androidComponents {
    onVariants { variant ->
        val generateSdkVersion = tasks.register<GenerateSdkVersionTask>(
            "generate${variant.name.replaceFirstChar { it.uppercase() }}SdkVersion"
        ) {
            versionName.set(sdkVersion)
        }
        variant.sources.kotlin?.addGeneratedSourceDirectory(generateSdkVersion, GenerateSdkVersionTask::outputDir)

        val copyAidlTask =
            tasks.register("copy${variant.name.replaceFirstChar { it.uppercase() }}AidlFiles", Copy::class.java) {
                from("src/main/aidl")
                into("build/intermediates/aidl_parcelable/${variant.name}/compile${variant.name.replaceFirstChar { it.uppercase() }}Aidl/out")
            }

        tasks.matching { task -> task.name == "compile${variant.name.replaceFirstChar { it.uppercase() }}Aidl" }
            .configureEach {
                finalizedBy(copyAidlTask)
            }

        tasks.matching { task -> task.name == "bundle${variant.name.replaceFirstChar { it.uppercase() }}Aar" }
            .configureEach {
                dependsOn(copyAidlTask)
            }

        tasks.matching { task -> task.name == "bundle${variant.name.replaceFirstChar { it.uppercase() }}LocalLintAar" }
            .configureEach {
                dependsOn(copyAidlTask)
            }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("org.mockito:mockito-core:5.22.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.github.dokuendev.dokuenreader"
                artifactId = "plugin-core"
                version = sdkVersion
            }
        }
    }
}
