package io.github.dokuendev.dokuenreader.plugin.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SdkVersionTest {

    @Test
    fun parsesMajorMinorPatch() {
        assertEquals(SdkVersion(0, 7, 0), SdkVersion.parse("0.7.0"))
        assertEquals(SdkVersion(1, 12, 3), SdkVersion.parse(" 1.12.3 "))
    }

    @Test
    fun rejectsMalformedVersions() {
        assertNull(SdkVersion.parse(null))
        assertNull(SdkVersion.parse(""))
        assertNull(SdkVersion.parse("0.7"))
        assertNull(SdkVersion.parse("0.7.0.1"))
        assertNull(SdkVersion.parse("0.x.0"))
        assertNull(SdkVersion.parse("0.-1.0"))
    }

    @Test
    fun comparesNumericallyByComponent() {
        assertTrue(SdkVersion(0, 7, 0) >= SdkVersion(0, 7, 0))
        assertTrue(SdkVersion(0, 10, 0) > SdkVersion(0, 9, 9))
        assertTrue(SdkVersion(1, 0, 0) > SdkVersion(0, 99, 99))
        assertTrue(SdkVersion(0, 6, 9) < SdkVersion(0, 7, 0))
    }

    @Test
    fun toStringIsDotSeparated() {
        assertEquals("0.7.0", SdkVersion(0, 7, 0).toString())
    }

    @Test
    fun advertisedSdkVersionIsValid() {
        assertNotNull(SdkVersion.parse(PluginSdkInfo.VERSION))
    }
}
