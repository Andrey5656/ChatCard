package com.andrey.chatcard

object Filter {
    private const val TLDS = "com|net|org|gg|io|co|me|uk|de|xyz|tv|ly|ru|info|biz|online|site|store|shop|club|fun|pro|dev|app|eu|fr|nl|pl|es|ca|au|sbs|top|link|live|world|space|tech|cc|ws"

    val link = Regex(
        "(?i)(?<![\\w.-])(?:https?://\\S+" +
            "|(?:[a-z0-9-]+\\.)+(?:$TLDS)(?![\\w-])(?::\\d{1,5})?(?:/\\S*)?" +
            "|\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})?(?![\\d.]))"
    )
    val linkPattern = link.toPattern()

    fun url(link: String): String =
        if (link.startsWith("http://", ignoreCase = true) || link.startsWith("https://", ignoreCase = true)) link else "https://$link"

    fun tooManyCaps(raw: String, minLetters: Int, maxPercent: Int): Boolean {
        val letters = raw.filter { it.isLetter() }
        if (letters.isEmpty() || letters.length < minLetters) return false
        return letters.count { it.isUpperCase() } * 100 > letters.length * maxPercent
    }

    fun normalize(raw: String): String = raw.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { raw.trim() }
}
