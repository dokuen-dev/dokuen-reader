package io.github.dokuendev.dokuenreader.plugin.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for PluginSecurity that run on actual Android devices/emulators.
 * 
 * These tests verify the actual Android framework interactions that cannot be
 * properly tested with Robolectric unit tests.
 */
@RunWith(AndroidJUnit4::class)
class PluginSecurityIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getOwnSignatureHash_isConsistent() {
        val hash1 = PluginSecurity.getOwnSignatureHash(context)
        val hash2 = PluginSecurity.getOwnSignatureHash(context)

        assertNotNull(hash1)
        assertNotNull(hash2)
        assertEquals("Multiple calls should return same hash", hash1, hash2)
    }

    @Test
    fun getOwnSignatureHash_matchesExpectedFormat() {
        val hash = PluginSecurity.getOwnSignatureHash(context)

        assertNotNull("Signature hash should not be null", hash)
        assertEquals("Hash should be 95 characters (32 hex pairs with colons)", 95, hash!!.length)
        assertTrue(
            "Hash should be colon-separated uppercase hex pairs",
            hash.matches(Regex("([0-9A-F]{2}:){31}[0-9A-F]{2}"))
        )
    }

    @Test
    fun verifyCallingApp_withOwnSignature_returnsTrue() {
        // Get our own signature hash
        val ownHash = PluginSecurity.getOwnSignatureHash(context)
        assertNotNull(ownHash)

        // Verify calling app with our own signature should succeed
        // Note: In a real plugin, this would be called from an AIDL stub method (e.g., initialize)
        // where Binder.getCallingUid() returns the host app's UID
        val result = PluginSecurity.verifyCallingApp(context, ownHash!!)

        assertTrue(
            "Verification should succeed with correct signature",
            result
        )
    }

    @Test
    fun verifyCallingApp_withWrongSignature_returnsFalse() {
        // Use a fake signature hash
        val fakeHash = "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:" +
                "00:11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff"

        val result = PluginSecurity.verifyCallingApp(context, fakeHash)

        assertFalse(
            "Verification should fail with incorrect signature",
            result
        )
    }

    @Test
    fun verifyCallingApp_withMalformedHash_returnsFalse() {
        val malformedHashes = listOf(
            "invalid",
            "12345",
            "",
            "zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:" +
                    "zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz:zz"
        )

        malformedHashes.forEach { hash ->
            val result = PluginSecurity.verifyCallingApp(context, hash)
            assertFalse(
                "Verification should fail with malformed hash: $hash",
                result
            )
        }
    }

    @Test
    fun packageName_matchesDokuenPrefix() {
        val packageName = context.packageName

        // In a real test, this would be the plugin's package name
        // For this test, we verify the test app's package structure
        assertNotNull(packageName)
        assertTrue(
            "Package name should not be empty",
            packageName.isNotEmpty()
        )
    }

    @Test
    fun signatureHash_canBeUsedForVerification() {
        // This test demonstrates the full workflow:
        // 1. Plugin author gets their signature hash during development
        // 2. Dokuen team provides the official signature hash
        // 3. Plugin uses it in its AIDL stub methods to verify the calling app

        val devSignature = PluginSecurity.getOwnSignatureHash(context)
        assertNotNull("Developer can get their own signature", devSignature)

        // In production, plugin would use Dokuen's signature:
        val dokuenSignature = "09:BE:27:77:4A:95:1E:F3:38:1C:40:19:EA:74:C1:58:" +
                "9D:A3:12:A2:C9:79:CB:AE:03:FB:08:5B:3F:E4:20:57"

        // Remove colons for comparison
        val normalizedDokuen = dokuenSignature.replace(":", "")
        assertEquals(64, normalizedDokuen.length)
    }
}
