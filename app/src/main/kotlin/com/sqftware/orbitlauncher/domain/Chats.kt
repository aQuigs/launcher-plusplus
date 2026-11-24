package com.sqftware.orbitlauncher.domain

import java.net.URI
import java.net.URISyntaxException

/** The longest name a chat is kept under. */
const val MAX_CHAT_NAME = 30

/** A chat of the app [kind] the user calls [label], blank until they have named it, which [link] opens. */
data class ChatValue(override val kind: ChatApp, override val label: String, val link: String) : InputValue {
    fun named(name: String) = copy(label = cleanName(name, MAX_CHAT_NAME).ifEmpty { kind.noun })

    override fun encode() = encodeFields(label, link)
}

/** What a text tells of a chat. */
sealed interface FoundChat {
    /** All of it: [chat], under the name its link gives it, if any. */
    data class Whole(val chat: ChatValue) : FoundChat

    /** A Slack [channel] in the workspace on [subdomain], whose id, which the app opens it by, is still to be looked up. */
    data class InWorkspace(val subdomain: String, val channel: String) : FoundChat
}

/**
 * A chat app that a link opens straight to one of its chats. Each is a kind of input of its own, as no app opens
 * another's chats. None lets the launcher search its chats, so a chat is told by its link, which [howToLink] says where to
 * get. Its name here is stored with the chats kept for it, so it never changes.
 */
enum class ChatApp(val packages: List<String>, val noun: String, val howToLink: String) : InputKind {
    // The app takes the ids of the workspace and the channel: https://docs.slack.dev/interactivity/deep-linking
    Slack(listOf("com.Slack"), "channel", "Copy a channel's link in Slack, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
            if (uri.scheme == "slack" && uri.host == "channel") return slackChannel(uri.params["team"].orEmpty(), uri.params["id"].orEmpty())
            if (!uri.isWeb || path.size < 2) return null
            return when {
                uri.host == "app.slack.com" && path[0] == "client" -> slackChannel(path[1], path.getOrElse(2) { "" })
                uri.host.endsWith(SLACK_HOST) && path[0] == "archives" && SLACK_CHANNEL.matches(path[1]) ->
                    FoundChat.InWorkspace(uri.host.removeSuffix(SLACK_HOST), path[1])
                else -> null
            }
        }
    },

    // Undocumented; from the app's manifest (354.12), which takes a channel on discord.com. Links copied from its test
    // builds, and old ones, name other hosts.
    Discord(listOf("com.discord"), "channel", "Copy a channel's link in Discord, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
            if (!uri.isWeb || DISCORD_HOSTS.none { uri.host == it || uri.host.endsWith(".$it") }) return null
            val known = path.size >= 3 && path[0] == "channels" && (path[1] == "@me" || path[1].isNumber) && path[2].isNumber
            return if (known) chat("https://discord.com/channels/${path[1]}/${path[2]}") else null
        }
    },

    // https://learn.microsoft.com/en-us/microsoftteams/platform/concepts/build-and-test/deep-link-teams
    Teams(listOf("com.microsoft.teams"), "channel", "Copy a link to a channel or a message in Teams, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
            val known = uri.isWeb && uri.host in TEAMS_HOSTS && path.size >= 3 && path[0] == "l" && path[1] in TEAMS_PATHS
            return if (known) chat("$uri", name = path.getOrNull(3).takeIf { path[1] == "channel" }) else null
        }
    },

    // https://core.telegram.org/api/links
    Telegram(listOf("org.telegram.messenger", "org.telegram.messenger.web"), "chat", "Copy a chat's link in Telegram, then paste it here") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
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
                private -> chat("$uri")
                public -> chat("https://t.me/$first", name = first)
                else -> null
            }
        }
    },

    // https://faq.whatsapp.com/5913398998672934
    WhatsApp(listOf("com.whatsapp", "com.whatsapp.w4b"), "chat", "Type a phone number from its + and country code, or paste a group's invite link") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
            if (!uri.isWeb || path.isEmpty()) return null
            val known = when (uri.host) {
                "wa.me" -> path[0].isNumber
                "chat.whatsapp.com" -> true
                "api.whatsapp.com" -> path[0] == "send" && "phone" in uri.params
                "whatsapp.com", "www.whatsapp.com" -> path[0] == "channel" && path.size >= 2
                else -> false
            }
            return if (known) chat("$uri") else null
        }

        override fun ofNumber(number: String) = chat("https://wa.me/${number.removePrefix("+")}")
    },

    // As the app reads them: https://github.com/signalapp/Signal-Android
    Signal(listOf("org.thoughtcrime.securesms"), "chat", "Type a phone number from its + and country code, or paste a username's or group's link") {
        override fun read(uri: URI, path: List<String>): FoundChat? {
            val fragment = uri.fragment?.takeIf { uri.isWeb } ?: return null
            val known = when (uri.host) {
                "signal.me" -> SIGNAL_PERSON.matches(fragment)
                "signal.group" -> fragment.isNotEmpty()
                else -> false
            }
            return if (known) chat("$uri") else null
        }

        override fun ofNumber(number: String) = chat("https://signal.me/#p/$number")
    };

    /** The chat [uri], whose path has the segments [path], links to; null if it links to none. */
    protected abstract fun read(uri: URI, path: List<String>): FoundChat?

    /** The chat with the phone [number], "+" and digits, for an app whose chats have one. */
    protected open fun ofNumber(number: String): FoundChat? = null

    protected fun chat(link: String, name: String? = null) = FoundChat.Whole(ChatValue(this, cleanName(name.orEmpty(), MAX_CHAT_NAME), link))

    /** The chat [typed] holds a link to, or is the phone number of; null if neither. */
    fun find(typed: String): FoundChat? =
        typed.split(BLANKS).firstNotNullOfOrNull { word -> uriOf(word)?.let { read(it, it.segments) } }
            ?: phoneNumber(typed)?.let(::ofNumber)

    override fun decode(text: String): ChatValue? = decodeFields(text, 2)?.let { (label, link) -> ChatValue(this, label, link) }
}

/** The Slack [channel] of the workspace [team], or null if they are not the ids of one. */
fun slackChannel(team: String, channel: String): FoundChat.Whole? =
    if (SLACK_TEAM.matches(team) && SLACK_CHANNEL.matches(channel)) FoundChat.Whole(ChatValue(ChatApp.Slack, "", "slack://channel?team=$team&id=$channel"))
    else null

// With its scheme and host in lower case, and a web link secure, as the apps take them.
private fun uriOf(word: String): URI? = try {
    val bare = word.trim { it in WRAPPING }
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
