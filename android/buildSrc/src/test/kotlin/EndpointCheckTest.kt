import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointCheckTest {

    private fun rejects(value: String, trailingSlash: Boolean = false): IllegalArgumentException =
        assertThrows(IllegalArgumentException::class.java) {
            LocalProperties.requireEndpoint("SUPABASE_URL", value, trailingSlash)
        }

    @Test
    fun httpsAndHttpUrls_pass() {
        assertEquals(
            "https://demo.supabase.co",
            LocalProperties.requireEndpoint("SUPABASE_URL", "https://demo.supabase.co")
        )
        assertEquals(
            "http://10.0.2.2:3000/api/v1/",
            LocalProperties.requireEndpoint("API_BASE_URL_DEBUG", "http://10.0.2.2:3000/api/v1/", true)
        )
    }

    @Test
    fun anEmptyValue_passes_becauseItMeansDemoDataOnly() {
        assertEquals("", LocalProperties.requireEndpoint("API_BASE_URL_RELEASE", "", true))
    }

    @Test
    fun aQuotedUrl_stopsTheBuild() {
        rejects("\"https://demo.supabase.co\"")
    }

    @Test
    fun aUrlWithoutScheme_stopsTheBuild() {
        rejects("demo.supabase.co")
    }

    @Test
    fun anotherScheme_stopsTheBuild() {
        rejects("ftp://demo.supabase.co")
    }

    @Test
    fun aUrlWithoutHost_stopsTheBuild() {
        rejects("https://")
        rejects("https:demo.supabase.co")
    }

    @Test
    fun spacesInsideTheUrl_stopTheBuild() {
        rejects("https://demo supabase.co")
    }

    @Test
    fun aMissingTrailingSlash_stopsTheBuild_onlyWhenRequired() {
        rejects("http://10.0.2.2:3000/api/v1", trailingSlash = true)
        LocalProperties.requireEndpoint("SUPABASE_URL", "https://demo.supabase.co")
    }

    @Test
    fun theMessage_namesTheProperty_butNeverItsValue() {
        val message = rejects("not-a-url-value").message.orEmpty()

        assertTrue(message.contains("SUPABASE_URL"))
        assertFalse(message.contains("not-a-url-value"))
    }
}
