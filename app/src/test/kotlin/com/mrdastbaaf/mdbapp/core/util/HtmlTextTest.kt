package com.mrdastbaaf.mdbapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The description field is HTML from the WordPress editor. The first build
 * rendered it verbatim and the course page showed `<p style="text-align: right">`
 * to the user, so these cases exist to stop that from coming back.
 */
class HtmlTextTest {

    @Test
    fun `strips paragraph tags and keeps the text`() {
        assertEquals(
            "دوره آموزش تابلو فرش",
            HtmlText.strip("""<p style="text-align: right">دوره آموزش تابلو فرش</p>"""),
        )
    }

    @Test
    fun `turns block tags into line breaks`() {
        assertEquals(
            "خط اول\nخط دوم",
            HtmlText.strip("<p>خط اول</p><p>خط دوم</p>"),
        )
    }

    @Test
    fun `handles self closing and plain br`() {
        assertEquals("الف\nب", HtmlText.strip("الف<br/>ب"))
        assertEquals("الف\nب", HtmlText.strip("الف<br>ب"))
    }

    @Test
    fun `decodes the entities WordPress emits`() {
        assertEquals("علی & رضا", HtmlText.strip("علی &amp; رضا"))
        assertEquals("«نیم\u200Cفاصله»", HtmlText.strip("&laquo;نیم&zwnj;فاصله&raquo;"))
        assertEquals("۵ > ۳", HtmlText.strip("۵ &gt; ۳"))
    }

    @Test
    fun `decodes numeric entities`() {
        assertEquals("A", HtmlText.strip("&#65;"))
        assertEquals("A", HtmlText.strip("&#x41;"))
    }

    @Test
    fun `collapses whitespace and blank lines`() {
        assertEquals("یک دو", HtmlText.strip("یک    دو"))
        assertEquals("یک\n\nدو", HtmlText.strip("<p>یک</p><p></p><p></p><p>دو</p>"))
    }

    @Test
    fun `empty and null input become an empty string`() {
        assertEquals("", HtmlText.strip(null))
        assertEquals("", HtmlText.strip(""))
        assertEquals("", HtmlText.strip("   "))
        assertEquals("", HtmlText.strip("<p></p>"))
    }
}
