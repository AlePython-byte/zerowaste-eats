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

    private val QUOTES = setOf('"', '\'')
}
