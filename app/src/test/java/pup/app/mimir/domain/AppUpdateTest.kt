package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    private fun v(text: String) = requireNotNull(AppVersion.parse(text))

    @Test
    fun parsesTagsAndVersionNames() {
        assertEquals(listOf(1, 2, 0), v("v1.2.0").parts)
        assertEquals(listOf(1, 1, 0), v("1.1.0").parts)
        assertEquals(listOf(2, 0, 1), v("2.0.1-beta1").parts)
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun comparesPartsAsNumbers() {
        assertTrue(v("1.10.0") > v("1.9.0"))
        assertTrue(v("1.2.0") > v("1.1.9"))
        assertEquals(0, v("1.2").compareTo(v("1.2.0")))
    }

    @Test
    fun offersOnlyNewerVersionsThatWereNotSkipped() {
        assertTrue(UpdatePolicy.shouldOffer(v("1.1.0"), v("1.2.0"), skippedVersion = null))
        assertFalse(UpdatePolicy.shouldOffer(v("1.2.0"), v("1.2.0"), skippedVersion = null))
        assertFalse(UpdatePolicy.shouldOffer(v("1.3.0"), v("1.2.0"), skippedVersion = null))
        assertFalse(UpdatePolicy.shouldOffer(v("1.1.0"), v("1.2.0"), skippedVersion = "1.2.0"))
        // Skipping 1.2.0 doesn't hide 1.3.0.
        assertTrue(UpdatePolicy.shouldOffer(v("1.1.0"), v("1.3.0"), skippedVersion = "1.2.0"))
        assertFalse(UpdatePolicy.shouldOffer(null, v("1.2.0"), skippedVersion = null))
    }

    @Test
    fun highlightsKeepUserFacingBulletsAndDropThePrList() {
        val body = """
            > [!IMPORTANT]
            > **Uninstall Mimir 1.0 before installing 1.1.0.**

            ## What's New
            * **NSP to NSZ compressor** in block mode (closes #13)
            * **Grid/list view toggle** on the home screen

            ## Improvements
            - Tools are sorted A to Z

            ## What's Changed
            * Add a Licenses page by @ItsRetroPup in https://github.com/ItsRetroPup/Mimir/pull/19

            **Full Changelog**: https://github.com/ItsRetroPup/Mimir/compare/v1.0.0...v1.1.0
        """.trimIndent().replace("\n", "\r\n")

        assertEquals(
            listOf(
                "NSP to NSZ compressor in block mode",
                "Grid/list view toggle on the home screen",
                "Tools are sorted A to Z",
            ),
            ReleaseNotes.highlights(body),
        )
    }

    @Test
    fun highlightsShowLinkTextOnly() {
        assertEquals(listOf("See the guide"), ReleaseNotes.highlights("* See [the guide](https://example.com)"))
    }
}
