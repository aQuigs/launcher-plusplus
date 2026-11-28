package com.sqftware.orbitlauncher.domain

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/** The longest name a linked chat or note is kept under. */
const val MAX_LINK_NAME = 30

/** What [link] opens in the app [kind], such as a chat or a note, which the user calls [label]: blank until they have named it. */
data class LinkValue(override val kind: LinkApp, override val label: String, val link: String) : InputValue {
    fun named(name: String) = copy(label = cleanName(name, MAX_LINK_NAME).ifEmpty { kind.noun })

    override fun encode() = encodeFields(label, link)
}

/** What a text tells of what it links to. */
sealed interface FoundLink {
    /** All of it: [value], under the name its link gives it, if any. */
    data class Whole(val value: LinkValue) : FoundLink

    /** A Slack [channel] in the workspace on [subdomain], whose id, which the app opens it by, is still to be looked up. */
    data class InWorkspace(val subdomain: String, val channel: String) : FoundLink
}

/**
 * An app that a link opens straight to a chat or a note of its, each a [noun]. Each is a kind of input of its own, as no
 * app opens another's. None lets the launcher search them, so one is told by its link, which [howToLink] says where to
 * get. Its name here is stored with those kept for it, and its [intentId] in icons' keys, so neither ever changes.
 */
enum class LinkApp(val packages: List<String>, val noun: String, val howToLink: String, val intentId: String = "chat") : InputKind {
    // The app takes the ids of the workspace and the channel: https://docs.slack.dev/interactivity/deep-linking
    Slack(listOf("com.Slack"), "channel", "Copy a channel's link in Slack, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            if (uri.scheme == "slack" && uri.host == "channel") return slackChannel(uri.params["team"].orEmpty(), uri.params["id"].orEmpty())
            if (!uri.isWeb || path.size < 2) return null
            return when {
                uri.host == "app.slack.com" && path[0] == "client" -> slackChannel(path[1], path.getOrElse(2) { "" })
                uri.host.endsWith(SLACK_HOST) && path[0] == "archives" && SLACK_CHANNEL.matches(path[1]) ->
                    FoundLink.InWorkspace(uri.host.removeSuffix(SLACK_HOST), path[1])
                else -> null
            }
        }
    },

    // Undocumented; from the app's manifest (354.12), which takes a channel on discord.com. Links copied from its test
    // builds, and old ones, name other hosts.
    Discord(listOf("com.discord"), "channel", "Copy a channel's link in Discord, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            if (!uri.isWeb || DISCORD_HOSTS.none { uri.host == it || uri.host.endsWith(".$it") }) return null
            val known = path.size >= 3 && path[0] == "channels" && (path[1] == "@me" || path[1].isNumber) && path[2].isNumber
            return if (known) found("https://discord.com/channels/${path[1]}/${path[2]}") else null
        }
    },

    // https://learn.microsoft.com/en-us/microsoftteams/platform/concepts/build-and-test/deep-link-teams
    Teams(listOf("com.microsoft.teams"), "channel", "Copy a link to a channel or a message in Teams, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            val known = uri.isWeb && uri.host in TEAMS_HOSTS && path.size >= 3 && path[0] == "l" && path[1] in TEAMS_PATHS
            return if (known) found("$uri", name = path.getOrNull(3).takeIf { path[1] == "channel" }) else null
        }
    },

    // https://core.telegram.org/api/links
    Telegram(listOf("org.telegram.messenger", "org.telegram.messenger.web"), "chat", "Copy a chat's link in Telegram, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            if (!uri.isWeb || uri.host !in TELEGRAM_HOSTS || path.isEmpty()) return null
            val first = path[0]
            // A private group is known by a link to one of its messages, or by an invite, which opens it for a member.
            val private = when (first) {
                "c" -> path.size >= 3
                "joinchat" -> path.size >= 2
                else -> first.startsWith("+") && first.length > 1
            }
            val public = TELEGRAM_NAME.matches(first) && first !in TELEGRAM_PAGES && path.drop(1).all { it.isNumber }
            return when {
                private -> found("$uri")
                public -> found("https://t.me/$first", name = first)
                else -> null
            }
        }
    },

    // https://faq.whatsapp.com/5913398998672934
    WhatsApp(listOf("com.whatsapp", "com.whatsapp.w4b"), "chat", "Type a phone number from its + and country code, or paste a group's invite link") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            if (!uri.isWeb || path.isEmpty()) return null
            val known = when (uri.host) {
                "wa.me" -> path[0].isNumber
                "chat.whatsapp.com" -> true
                "api.whatsapp.com" -> path[0] == "send" && "phone" in uri.params
                "whatsapp.com", "www.whatsapp.com" -> path[0] == "channel" && path.size >= 2
                else -> false
            }
            return if (known) found("$uri") else null
        }

        override fun ofNumber(number: String) = found("https://wa.me/${number.removePrefix("+")}")
    },

    // As the app reads them: https://github.com/signalapp/Signal-Android
    Signal(listOf("org.thoughtcrime.securesms"), "chat", "Type a phone number from its + and country code, or paste a username's or group's link") {
        override fun read(uri: URI, path: List<String>): FoundLink? {
            val fragment = uri.fragment?.takeIf { uri.isWeb } ?: return null
            val known = when (uri.host) {
                "signal.me" -> SIGNAL_PERSON.matches(fragment)
                "signal.group" -> fragment.isNotEmpty()
                else -> false
            }
            return if (known) found("$uri") else null
        }

        override fun ofNumber(number: String) = found("https://signal.me/#p/$number")
    },

    // https://obsidian.md/help/uri. Its Android app (1.15.1) opens no note by a path on the device.
    Obsidian(
        listOf("md.obsidian"),
        "note",
        "Copy a note's link in Obsidian, from More options, Copy path, as Obsidian URL, then paste it here",
        intentId = "note",
    ) {
        // The links it copies leave other marks unescaped, so a note's name can end its link with one.
        override val afterLink = "<>[]\""

        override fun read(uri: URI, path: List<String>): FoundLink? {
            if (uri.scheme != "obsidian") return null
            val note = when (uri.host) {
                "open" -> uri.params["file"]?.let { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }
                "vault" -> path.drop(1).joinToString("/")
                else -> null
            }
            return if (note.isNullOrEmpty()) null else found("$uri", name = note.substringAfterLast('/').removeSuffix(".md"))
        }
    };

    /** The marks that can come after a link in the middle of a text, rather than end it. */
    protected open val afterLink: String get() = WRAPPING

    /** What [uri], whose path has the segments [path], links to; null if nothing of this app's. */
    protected abstract fun read(uri: URI, path: List<String>): FoundLink?

    /** The chat with the phone [number], "+" and digits, for an app whose chats have one. */
    protected open fun ofNumber(number: String): FoundLink? = null

    protected fun found(link: String, name: String? = null) = FoundLink.Whole(LinkValue(this, cleanName(name.orEmpty(), MAX_LINK_NAME), link))

    /** What [typed] holds a link to, or is the phone number of; null if neither. */
    fun find(typed: String): FoundLink? =
        typed.split(BLANKS).firstNotNullOfOrNull { word -> uriOf(word, afterLink)?.let { read(it, it.segments) } }
            ?: phoneNumber(typed)?.let(::ofNumber)

    override fun decode(text: String): LinkValue? = decodeFields(text, 2)?.let { (label, link) -> LinkValue(this, label, link) }
}

/** The Slack [channel] of the workspace [team], or null if they are not the ids of one. */
fun slackChannel(team: String, channel: String): FoundLink.Whole? =
    if (SLACK_TEAM.matches(team) && SLACK_CHANNEL.matches(channel)) FoundLink.Whole(LinkValue(LinkApp.Slack, "", "slack://channel?team=$team&id=$channel"))
    else null

// With its scheme and host in lower case, and a web link secure, as the apps take them.
private fun uriOf(word: String, afterLink: String): URI? = try {
    val bare = word.trimStart { it in WRAPPING }.trimEnd { it in afterLink }
    val scheme = bare.substringBefore("://", "https").lowercase()
    val rest = bare.substringAfter("://")
    val host = rest.takeWhile { it !in "/?#" }
    URI("${if (scheme == "http") "https" else scheme}://${host.lowercase()}${rest.drop(host.length)}").takeIf { it.host != null }
} catch (_: URISyntaxException) {
    null
}

// Written as people and other apps do: with a trunk zero, any kind of dash, or marks that cannot be seen.
private fun phoneNumber(typed: String): String? =
    typed.replace("(0)", "").filter { it.isLetterOrDigit() || it == '+' }.takeIf(PHONE::matches)

private val URI.isWeb get() = scheme == "https"

private val URI.segments: List<String> get() = path.orEmpty().split('/').filter(String::isNotEmpty)

private val URI.params: Map<String, String>
    get() = rawQuery.orEmpty().split('&').filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }

private val String.isNumber get() = isNotEmpty() && all { it in '0'..'9' }

private const val SLACK_HOST = ".slack.com"

// As Slack numbers a workspace or an organization, and a channel, a direct message or a group one.
private val SLACK_TEAM = Regex("[TE][A-Z0-9]+")

private val SLACK_CHANNEL = Regex("[CDG][A-Z0-9]+")

private val DISCORD_HOSTS = listOf("discord.com", "discordapp.com")

private val TEAMS_HOSTS = listOf("teams.microsoft.com", "teams.live.com", "teams.cloud.microsoft")

private val TEAMS_PATHS = listOf("channel", "chat", "message")

private val TELEGRAM_HOSTS = listOf("t.me", "telegram.me", "telegram.dog")

private val TELEGRAM_NAME = Regex("[A-Za-z][A-Za-z0-9_]{3,}")

// Pages that are no chat, under names a chat could have.
private val TELEGRAM_PAGES = listOf("proxy", "socks", "share", "login", "confirmphone", "joinchat")

private val SIGNAL_PERSON = Regex("(p|eu)/.+")

private val BLANKS = Regex("\\s+")

private val PHONE = Regex("\\+[1-9][0-9]{6,14}")

// What a link in the middle of a text can have around it.
private const val WRAPPING = "<>()[]\"'.,;:!"
