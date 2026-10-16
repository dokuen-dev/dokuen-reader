import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

// Every module is published to Maven Central with the configuration below. The publishing plugin
// reads the group and the POM metadata shared by all modules from gradle.properties, and each
// module's name and description from the gradle.properties in the module's directory. The artifact
// ID is the module's project name.
val sdkVersion = providers.gradleProperty("SDK_VERSION").get()

// Maven Central requires signed artifacts, but publishing to the local Maven repository should work
// without the release signing key. So the publications are signed only when a key is configured
// (as a key ring file or in memory). The Central Portal rejects an upload that is not signed.
val signingKeyConfigured = providers.gradleProperty("signing.keyId").isPresent ||
        providers.gradleProperty("signingInMemoryKey").isPresent

subprojects {
    pluginManager.withPlugin("com.android.library") {
        apply(plugin = "com.vanniktech.maven.publish")

        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            if (signingKeyConfigured) {
                signAllPublications()
            }
            configure(
                AndroidSingleVariantLibrary(
                    javadocJar = JavadocJar.Javadoc(),
                    sourcesJar = SourcesJar.Sources(),
                    variant = "release",
                )
            )
            coordinates(version = sdkVersion)
        }
    }
}
