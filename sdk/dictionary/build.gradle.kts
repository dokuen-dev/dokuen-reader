import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
    id("maven-publish")
}

val sdkVersion = providers.gradleProperty("SDK_VERSION").get()

configure<LibraryExtension> {
    namespace = "io.github.dokuendev.dokuenreader.dictionary"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Generate SDK version as a resource that can be used in AndroidManifest.xml
        manifestPlaceholders["sdkVersion"] = sdkVersion
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

// Ensure plugin-core AIDL files are copied before compiling dictionary AIDL
androidComponents {
    onVariants { variant ->
        tasks.matching { task -> task.name == "compile${variant.name.replaceFirstChar { it.uppercase() }}Aidl" }
            .configureEach {
                dependsOn(":plugin-core:copy${variant.name.replaceFirstChar { it.uppercase() }}AidlFiles")
            }
    }
}

dependencies {
    api(project(":plugin-core"))
    implementation("androidx.core:core-ktx:1.17.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
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
                artifactId = "dictionary"
                version = sdkVersion
            }
        }
    }
}
