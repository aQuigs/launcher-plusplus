package com.sqftware.orbitlauncher.apps

import android.util.Log
import com.sqftware.orbitlauncher.domain.ChatApp
import com.sqftware.orbitlauncher.domain.ChatValue
import com.sqftware.orbitlauncher.domain.FoundChat
import com.sqftware.orbitlauncher.domain.slackChannel
import org.json.JSONException
import java.io.IOException
import java.net.URLEncoder

private const val TAG = "ChatFinder"
private const val ENDPOINT = "https://slack.com/api/auth.findTeam"
private const val TIMEOUT_MILLIS = 4_000

/** Finds the chat a typed or pasted text tells. */
interface ChatFinder {
    /** The chat of [app] that [typed] tells, not named yet unless its link names it; null if it tells none. */
    suspend fun find(app: ChatApp, typed: String): ChatValue?
}

/** Finds the id of a Slack workspace, which Slack's app opens a channel by, where the links it copies name a subdomain. */
interface SlackWorkspaces {
    /** The id of the workspace on [subdomain], or null if there is none or it cannot be found out. */
    suspend fun id(subdomain: String): String?
}

/** [ChatFinder] by the links themselves, and [slack] for the id Slack's lack: without it, [onNoWorkspace] hears its subdomain. */
class LinkChatFinder(private val slack: SlackWorkspaces, private val onNoWorkspace: (String) -> Unit) : ChatFinder {
    override suspend fun find(app: ChatApp, typed: String): ChatValue? = when (val found = app.find(typed)) {
        null -> null
        is FoundChat.Whole -> found.chat
        is FoundChat.InWorkspace -> {
            val whole = slack.id(found.subdomain)?.let { slackChannel(it, found.channel) }
            if (whole == null) onNoWorkspace(found.subdomain)
            whole?.chat
        }
    }
}

/** [SlackWorkspaces] by the call Slack's own sign-in page makes, which is undocumented but needs no token. */
class WebSlackWorkspaces : SlackWorkspaces {
    override suspend fun id(subdomain: String): String? = try {
        getJson("$ENDPOINT?domain=${URLEncoder.encode(subdomain, "UTF-8")}", TIMEOUT_MILLIS).optString("team_id").ifEmpty { null }
    } catch (e: IOException) {
        Log.w(TAG, "Cannot look up the Slack workspace", e)
        null
    } catch (e: JSONException) {
        Log.w(TAG, "Cannot read the Slack workspace found", e)
        null
    }
}
