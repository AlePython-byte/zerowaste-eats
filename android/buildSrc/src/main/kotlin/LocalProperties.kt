import java.net.URI
import java.net.URISyntaxException

/** Reads values written in local.properties, which people edit by hand. */
object LocalProperties {

    /**
     * [raw] without surrounding whitespace or one pair of matching quotes. A .properties file
     * keeps quotes as part of the value, so `KEY="value"` would otherwise reach the app as
     * `"value"`, quotes included.
     */
    fun clean(raw: String): String {
        val trimmed = raw.trim()
        val quoted = trimmed.length >= 2 &&
            trimmed.first() in QUOTES &&
            trimmed.last() == trimmed.first()
        return if (quoted) trimmed.substring(1, trimmed.length - 1).trim() else trimmed
    }

    /**
     * Returns [value] when it is empty (no endpoint: the app offers demo data only) or an
     * http(s) URL with a host, ending in "/" when [trailingSlash] asks for it. Otherwise it
     * throws, which stops the build, so a broken endpoint never reaches a phone. The message
     * names the property but never repeats its value.
     */
    fun requireEndpoint(name: String, value: String, trailingSlash: Boolean = false): String {
        if (value.isEmpty()) return value
        require(isHttpUrl(value)) {
            "$name in local.properties must be an http:// or https:// URL with a host, " +
                "or empty to use demo data only"
        }
        require(!trailingSlash || value.endsWith("/")) {
            "$name in local.properties must end in /"
        }
        return value
    }

    private fun isHttpUrl(value: String): Boolean {
        val uri = try {
            URI(value)
        } catch (_: URISyntaxException) {
            return false
        }
        return uri.scheme?.lowercase() in HTTP_SCHEMES && !uri.host.isNullOrBlank()
    }

    private val QUOTES = setOf('"', '\'')
    private val HTTP_SCHEMES = setOf("http", "https")
}
