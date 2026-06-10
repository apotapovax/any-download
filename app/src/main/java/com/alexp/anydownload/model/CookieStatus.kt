package com.alexp.anydownload

data class CookieStatus(
    val isLoaded: Boolean,
    val cookieCount: Int = 0,
    val domains: List<String> = emptyList(),
    val importedAtMillis: Long? = null,
    val hasInstagram: Boolean = false,
    val hasFacebook: Boolean = false,
    val hasTwitter: Boolean = false,
) {
    val summary: String
        get() = when {
            !isLoaded -> "Not imported — required for private links"
            else -> buildString {
                append("$cookieCount cookies")
                val sites = buildList {
                    if (hasInstagram) add("Instagram")
                    if (hasFacebook) add("Facebook")
                    if (hasTwitter) add("X")
                }
                if (sites.isNotEmpty()) {
                    append(" · ")
                    append(sites.joinToString(", "))
                }
            }
        }
}
