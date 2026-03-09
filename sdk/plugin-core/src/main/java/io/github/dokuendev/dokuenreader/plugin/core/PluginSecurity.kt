package io.github.dokuendev.dokuenreader.plugin.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import java.security.MessageDigest

/**
 * Security utilities for plugin authentication.
 *
 * Plugins should use verifyCallingApp() in their AIDL stub methods (e.g., initialize) to ensure
 * only the official Dokuen app can connect.
 *
 * Note: Signature verification is automatically skipped in debug builds to simplify development
 * and testing. Verification is only enforced in release builds.
 */
object PluginSecurity {

    private const val DOKUEN_PACKAGE_PREFIX = "io.github.dokuendev."

    /**
     * Verifies that the calling app is an official Dokuen release.
     * 
     * In debug builds, this verification is automatically skipped and always returns true
     * to simplify development and testing. In release builds, full signature verification
     * is performed.
     * 
     * @param context The service context
     * @param expectedSignatureHash SHA-256 hash of Dokuen's release signing certificate
     * @return true if the caller is verified Dokuen (or if running in debug mode), false otherwise
     */
    fun verifyCallingApp(context: Context, expectedSignatureHash: String): Boolean {
        // Skip verification in debug builds to simplify development and testing
        val isDebugBuild = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebugBuild) {
            return true
        }

        val callingUid = Binder.getCallingUid()
        val packageManager = context.packageManager

        // 1. Check if the calling package is Dokuen
        // We check a prefix rather than the full package name (io.github.dokuendev.dokuenreader)
        // to future-proof the plugin to seamlessly support future Dokuen family apps
        // (e.g. Dokuen Chinese Reader) without requiring a plugin update.
        val callingPackages = packageManager.getPackagesForUid(callingUid)
        val callingPackage = callingPackages?.firstOrNull { it.startsWith(DOKUEN_PACKAGE_PREFIX) }
        if (callingPackage == null) {
            return false
        }

        // 2. Verify the cryptographic signature
        return try {
            val packageInfo =
                packageManager.getPackageInfo(
                    callingPackage,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )

            val signatures =
                packageInfo.signingInfo?.apkContentsSigners

            signatures?.any { signature ->
                hashSignature(signature.toByteArray()) == expectedSignatureHash
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes SHA-256 hash of a signature.
     */
    private fun hashSignature(signature: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(signature)
        return hash.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Gets the SHA-256 hash of the current app's signing certificate.
     * Plugin authors can use this during development to get their own signature hash.
     */
    fun getOwnSignatureHash(context: Context): String? {
        return try {
            val packageInfo =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )

            val signature =
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()

            signature?.let { hashSignature(it.toByteArray()) }
        } catch (_: Exception) {
            null
        }
    }
}
