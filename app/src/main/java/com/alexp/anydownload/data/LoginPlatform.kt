package com.alexp.anydownload.data

enum class LoginPlatform(
    val label: String,
    val loginUrl: String,
    val cookieDomains: List<String>,
) {
    INSTAGRAM(
        label = "Instagram",
        loginUrl = "https://www.instagram.com/accounts/login/",
        cookieDomains = listOf("instagram.com"),
    ),
    FACEBOOK(
        label = "Facebook",
        loginUrl = "https://www.facebook.com/login/",
        cookieDomains = listOf("facebook.com", "fb.com"),
    ),
    TWITTER(
        label = "X / Twitter",
        loginUrl = "https://x.com/i/flow/login",
        cookieDomains = listOf("twitter.com", "x.com"),
    ),
    ;

    fun cookieUrls(): List<String> = cookieDomains.flatMap { domain ->
        listOf("https://www.$domain", "https://$domain", "https://m.$domain")
    }

    fun isLoggedIn(cookieManager: android.webkit.CookieManager): Boolean {
        val combined = cookieUrls()
            .mapNotNull { url -> cookieManager.getCookie(url) }
            .joinToString("; ")

        return when (this) {
            INSTAGRAM -> combined.contains("sessionid=")
            FACEBOOK -> combined.contains("c_user=")
            TWITTER -> combined.contains("auth_token=") || combined.contains("twid=")
        }
    }
}
