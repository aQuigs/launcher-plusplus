package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatsTest {
    private val slack = app("Slack", "com.Slack")
    private val discord = app("Discord", "com.discord")
    private val general = ChatValue(ChatApp.Slack, "general", "slack://channel?team=T1&id=C1")

    private fun ChatApp.linkIn(typed: String) = (find(typed) as? FoundChat.Whole)?.chat?.link

    @Test
    fun `a Slack link opens its channel by the workspace's id, which a copied one is to be looked up for`() {
        assertEquals(general.link, ChatApp.Slack.linkIn("https://app.slack.com/client/T1/C1"))
        assertEquals(general.link, ChatApp.Slack.linkIn(general.link))
        listOf("see https://acme.slack.com/archives/C1/p1700000000000000?thread_ts=1&cid=C1 there", "Acme.slack.com/archives/C1").forEach {
            assertEquals(FoundChat.InWorkspace("acme", "C1"), ChatApp.Slack.find(it))
        }
        assertEquals(general.copy(label = ""), slackChannel("T1", "C1")?.chat)
    }

    @Test
    fun `a Slack link with anything but ids in it is none`() {
        listOf(
            "https://app.slack.com/client/T1/later",
            "https://acme.slack.com/archives/C1%20x",
            "https://app.slack.com/client/T1%26id%3DC2/C1",
            "slack://channel?team=T1&id=C1%26team%3DT2",
            "slack://channel?team=&id=",
        ).forEach { assertNull(it, ChatApp.Slack.find(it)) }
        assertNull(slackChannel("null", "C1"))
    }

    @Test
    fun `a Discord link names the one host its app takes, and no message`() {
        listOf("https://discord.com/channels/1/2", "https://ptb.discord.com/channels/1/2/3", "http://discordapp.com/channels/1/2").forEach {
            assertEquals("https://discord.com/channels/1/2", ChatApp.Discord.linkIn(it))
        }
        assertEquals("https://discord.com/channels/@me/2", ChatApp.Discord.linkIn("https://discord.com/channels/@me/2"))
        assertNull(ChatApp.Discord.linkIn("https://notdiscord.com/channels/1/2"))
        assertNull(ChatApp.Discord.linkIn("https://discord.com/invite/abc"))
    }

    @Test
    fun `a link is found however it is capitalized, and in the middle of a text`() {
        listOf("(https://t.me/durov)", "see HTTPS://T.me/durov.", "<t.me/durov/123>").forEach { assertEquals("https://t.me/durov", ChatApp.Telegram.linkIn(it)) }
        assertNull(ChatApp.Telegram.linkIn("https://t.me/proxy?server=1.2.3.4"))
        assertNull(ChatApp.Telegram.linkIn("https://t.me/addstickers/Cats"))
    }

    @Test
    fun `a link that names its chat names it, and the others are left as they are`() {
        val teams = "https://teams.microsoft.com/l/channel/19%3Aabc%40thread.tacv2/Road%20Map?groupId=1&tenantId=2"
        assertEquals(FoundChat.Whole(ChatValue(ChatApp.Teams, "Road Map", teams)), ChatApp.Teams.find(teams))
        assertEquals(FoundChat.Whole(ChatValue(ChatApp.Telegram, "durov", "https://t.me/durov")), ChatApp.Telegram.find("t.me/durov"))

        listOf(
            ChatApp.Teams to "https://teams.microsoft.com/l/chat/19:abc@thread.v2/conversations",
            ChatApp.Telegram to "https://t.me/c/123/4",
            ChatApp.Telegram to "https://t.me/+AbC",
            ChatApp.WhatsApp to "https://chat.whatsapp.com/AbC",
            ChatApp.WhatsApp to "https://api.whatsapp.com/send?phone=16505550100",
            ChatApp.Signal to "https://signal.me/#eu/AbC",
            ChatApp.Signal to "https://signal.group/#AbC",
        ).forEach { (app, link) -> assertEquals(FoundChat.Whole(ChatValue(app, "", link)), app.find(link)) }
    }

    @Test
    fun `a phone number tells a chat of an app whose chats have one`() {
        assertEquals("https://wa.me/16505550100", ChatApp.WhatsApp.linkIn("+1 (650) 555-0100"))
        assertEquals("https://signal.me/#p/+16505550100", ChatApp.Signal.linkIn("+1 650 555 0100"))
        assertEquals("https://wa.me/442079460958", ChatApp.WhatsApp.linkIn("‪+44 (0)20 7946‑0958‬"))
        assertNull(ChatApp.WhatsApp.linkIn("555 0100"))
        assertNull(ChatApp.Slack.find("+16505550100"))
    }

    @Test
    fun `no app takes another's link, nor anything that is none`() {
        val links = listOf(general.link, "https://discord.com/channels/1/2", "https://t.me/durov", "https://wa.me/1650", "https://signal.me/#eu/A")

        links.forEach { link -> assertEquals(link, 1, ChatApp.entries.count { it.find(link) != null }) }
        listOf("", "general", "https://", "slack://open", "https://slack.com/", "https://t.me", "a%zz.slack.com/archives/C1").forEach { typed ->
            assertTrue(typed, ChatApp.entries.all { it.find(typed) == null })
        }
    }

    @Test
    fun `an icon of a chat is named after it, opens its link, and resolves by its key`() {
        val (_, now, eachTime) = openChoices(slack)
        val icon = openedAs(slack, now, general.named(" a, b |\tc "))!!

        assertEquals("a, b | c", icon.label)
        assertEquals("Slack channel", openedAs(slack, eachTime)!!.label)
        assertEquals(IntentRequest("com.Slack", general.link), (icon.kind as EntryKind.Intent).request())
        assertEquals(icon, AppsByKey(listOf(slack))[icon.key])
    }

    @Test
    fun `apps of a pair never share a chat`() {
        assertEquals(3, openChoices(discord, openedAs(slack, openChoices(slack).last())).size)
        assertNull(openedAs(discord, openChoices(discord)[1], general))
    }

    @Test
    fun `a chat is named something even when nothing seen is typed`() {
        assertEquals("channel", general.named("​").label)
    }
}
