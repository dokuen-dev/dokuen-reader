package io.github.dokuendev.dokuenreader.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageRequestSourceTest {

    @Test
    fun codesAreStable() {
        assertEquals(0, ImageRequestSource.SCREENSHOT.code)
        assertEquals(1, ImageRequestSource.CLIPBOARD.code)
    }

    @Test
    fun fromCodeResolvesKnownCodesAndRejectsOthers() {
        assertEquals(ImageRequestSource.SCREENSHOT, ImageRequestSource.fromCode(0))
        assertEquals(ImageRequestSource.CLIPBOARD, ImageRequestSource.fromCode(1))
        assertNull(ImageRequestSource.fromCode(2))
        assertNull(ImageRequestSource.fromCode(-1))
    }
}
