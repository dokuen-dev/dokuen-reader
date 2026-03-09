package io.github.dokuendev.dokuenreader.plugin.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Internal base class for all Dokuen plugin services.
 * Handles the security handshake and IPC boilerplate.
 * Plugin authors should not extend this directly; use domain-specific services instead
 * (e.g., OcrPluginService).
 */
abstract class BasePluginService : Service() {

    // The official Dokuen release signature hash
    private val dokuenSignatureHash =
        "09:BE:27:77:4A:95:1E:F3:38:1C:40:19:EA:74:C1:58:9D:A3:12:A2:C9:79:CB:AE:03:FB:08:5B:3F:E4:20:57"

    private val verifiedUids = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    final override fun onBind(intent: Intent?): IBinder? {
        return getBinder()
    }

    protected fun verifyAndRegisterCallingApp(): Boolean {
        if (!PluginSecurity.verifyCallingApp(this, dokuenSignatureHash)) {
            return false
        }
        verifiedUids.add(Binder.getCallingUid())
        return true
    }

    protected fun isCallingAppRegistered(): Boolean {
        return verifiedUids.contains(Binder.getCallingUid())
    }

    /**
     * Returns the domain-specific Binder implementation.
     */
    protected abstract fun getBinder(): IBinder
}
