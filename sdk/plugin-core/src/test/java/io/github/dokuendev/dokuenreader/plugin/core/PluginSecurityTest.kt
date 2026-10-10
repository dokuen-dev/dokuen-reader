package io.github.dokuendev.dokuenreader.plugin.core

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBinder
import org.robolectric.shadows.ShadowSigningInfo
import java.security.MessageDigest

/**
 * Tests [PluginSecurity.verifyCallingApp] against a faked calling app. Robolectric's application
 * is not debuggable, so the signature check runs; on a device the test APK is a debug build,
 * for which the check is skipped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PluginSecurityTest {

    private lateinit var app: Application

    private val dokuenSignature = Signature(byteArrayOf(1, 2, 3, 4))
    private val otherSignature = Signature(byteArrayOf(5, 6, 7, 8))

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
    }

    /** Makes the calling app [uid], which holds [packages], each signed with [signature]. */
    private fun callerIs(uid: Int, packages: List<String>, signature: Signature) {
        val packageManager = shadowOf(app.packageManager)
        for (name in packages) {
            val signingInfo = Shadow.newInstanceOf(SigningInfo::class.java)
            Shadow.extract<ShadowSigningInfo>(signingInfo).setSignatures(arrayOf(signature))
            packageManager.installPackage(
                PackageInfo().apply {
                    packageName = name
                    this.signingInfo = signingInfo
                    applicationInfo = ApplicationInfo().apply {
                        packageName = name
                        this.uid = uid
                    }
                }
            )
        }
        packageManager.setPackagesForUid(uid, *packages.toTypedArray())
        ShadowBinder.setCallingUid(uid)
    }

    /** The colon-separated, uppercase SHA-256 hash of [signature], as Dokuen's hash is written. */
    private fun hashOf(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString(":") { "%02X".format(it) }

    @Test
    fun acceptsDokuenPackageWithExpectedSignature() {
        callerIs(10_001, listOf("io.github.dokuendev.dokuenreader"), dokuenSignature)

        assertTrue(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }

    @Test
    fun rejectsDokuenPackageWithOtherSignature() {
        callerIs(10_001, listOf("io.github.dokuendev.dokuenreader"), otherSignature)

        assertFalse(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }

    @Test
    fun rejectsMalformedExpectedHashes() {
        callerIs(10_001, listOf("io.github.dokuendev.dokuenreader"), dokuenSignature)

        val malformed = listOf("", "invalid", "12345", List(32) { "zz" }.joinToString(":"))
        for (hash in malformed) {
            assertFalse("Accepted malformed hash: $hash", PluginSecurity.verifyCallingApp(app, hash))
        }
    }

    @Test
    fun rejectsExpectedSignatureOutsideDokuenPackages() {
        callerIs(10_001, listOf("com.example.reader"), dokuenSignature)

        assertFalse(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }

    @Test
    fun findsDokuenPackageAmongPackagesSharingTheUid() {
        callerIs(10_001, listOf("com.example.shared", "io.github.dokuendev.dokuenreader"), dokuenSignature)

        assertTrue(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }

    @Test
    fun rejectsCallerWithoutPackages() {
        ShadowBinder.setCallingUid(10_002)

        assertFalse(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }

    @Test
    fun debugBuildSkipsTheCheck() {
        callerIs(10_001, listOf("com.example.reader"), otherSignature)
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE

        assertTrue(PluginSecurity.verifyCallingApp(app, hashOf(dokuenSignature)))
    }
}
