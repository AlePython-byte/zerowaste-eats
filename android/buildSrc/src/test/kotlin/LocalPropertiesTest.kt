import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPropertiesTest {

    @Test
    fun aPlainValue_staysAsIs() {
        assertEquals("https://demo.supabase.co", LocalProperties.clean("https://demo.supabase.co"))
    }

    @Test
    fun surroundingWhitespace_isRemoved() {
        assertEquals("https://demo.supabase.co", LocalProperties.clean("  https://demo.supabase.co \t"))
    }

    @Test
    fun doubleQuotes_areRemoved() {
        assertEquals("https://demo.supabase.co", LocalProperties.clean("\"https://demo.supabase.co\""))
    }

    @Test
    fun singleQuotes_areRemoved() {
        assertEquals("https://demo.supabase.co", LocalProperties.clean("'https://demo.supabase.co'"))
    }

    @Test
    fun whitespaceAroundAndInsideTheQuotes_isRemoved() {
        assertEquals("abc", LocalProperties.clean("  \" abc \"  "))
    }

    @Test
    fun onlyOnePairOfQuotes_isRemoved() {
        assertEquals("\"abc\"", LocalProperties.clean("\"\"abc\"\""))
    }

    @Test
    fun unmatchedQuotes_areKept() {
        assertEquals("\"abc", LocalProperties.clean("\"abc"))
        assertEquals("abc'", LocalProperties.clean("abc'"))
        assertEquals("\"abc'", LocalProperties.clean("\"abc'"))
    }

    @Test
    fun quotesInsideTheValue_areKept() {
        assertEquals("a\"b", LocalProperties.clean("a\"b"))
    }

    @Test
    fun emptyValues_stayEmpty() {
        assertEquals("", LocalProperties.clean(""))
        assertEquals("", LocalProperties.clean("   "))
        assertEquals("", LocalProperties.clean("\"\""))
        assertEquals("\"", LocalProperties.clean("\""))
    }
}
