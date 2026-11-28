package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkAppsTest {
    private val slack = app("Slack", "com.Slack")
    private val discord = app("Discord", "com.discord")
    private val general = LinkValue(LinkApp.Slack, "general", "slack://channel?team=T1&id=C1")

    private fun LinkApp.linkIn(typed: String) = (find(typed) as? FoundLink.Whole)?.value?.link

    @Test
    fun `a Slack link opens its channel by the workspace's id, which a copied one is to be looked up for`() {
        assertEquals(general.link, LinkApp.Slack.linkIn("https://app.slack.com/client/T1/C1"))
        assertEquals(general.link, LinkApp.Slack.linkIn(general.link))
        listOf("see https://acme.slack.com/archives/C1/p1700000000000000?thread_ts=1&cid=C1 there", "Acme.slack.com/archives/C1").forEach {
            assertEquals(FoundLink.InWorkspace("acme", "C1"), LinkApp.Slack.find(it))
        }
        assertEquals(general.copy(label = ""), slackChannel("T1", "C1")?.value)
    }

    @Test
    fun `a Slack link with anything but ids in it is none`() {
        listOf(
            "https://app.slack.com/client/T1/later",
            "https://acme.slack.com/archives/C1%20x",
            "https://app.slack.com/client/T1%26id%3DC2/C1",
            "slack://channel?team=T1&id=C1%26team%3DT2",
            "slack://channel?team=&id=",
        ).forEach { assertNull(it, LinkApp.Slack.find(it)) }
        assertNull(slackChannel("null", "C1"))
    }

    @Test
    fun `a Discord link names the one host its app takes, and no message`() {
        listOf("https://discord.com/channels/1/2", "https://ptb.discord.com/channels/1/2/3", "http://discordapp.com/channels/1/2").forEach {
            assertEquals("https://discord.com/channels/1/2", LinkApp.Discord.linkIn(it))
        }
        assertEquals("https://discord.com/channels/@me/2", LinkApp.Discord.linkIn("https://discord.com/channels/@me/2"))
        assertNull(LinkApp.Discord.linkIn("https://notdiscord.com/channels/1/2"))
        assertNull(LinkApp.Discord.linkIn("https://discord.com/invite/abc"))
    }

    @Test
    fun `a link is found however it is capitalized, and in the middle of a text`() {
        listOf("(https://t.me/durov)", "see HTTPS://T.me/durov.", "<t.me/durov/123>").forEach { assertEquals("https://t.me/durov", LinkApp.Telegram.linkIn(it)) }
        assertNull(LinkApp.Telegram.linkIn("https://t.me/proxy?server=1.2.3.4"))
        assertNull(LinkApp.Telegram.linkIn("https://t.me/addstickers/Cats"))
    }

    @Test
    fun `a link that names its chat names it, and the others are left as they are`() {
        val teams = "https://teams.microsoft.com/l/channel/19%3Aabc%40thread.tacv2/Road%20Map?groupId=1&tenantId=2"
        assertEquals(FoundLink.Whole(LinkValue(LinkApp.Teams, "Road Map", teams)), LinkApp.Teams.find(teams))
        assertEquals(FoundLink.Whole(LinkValue(LinkApp.Telegram, "durov", "https://t.me/durov")), LinkApp.Telegram.find("t.me/durov"))

        listOf(
            LinkApp.Teams to "https://teams.microsoft.com/l/chat/19:abc@thread.v2/conversations",
            LinkApp.Telegram to "https://t.me/c/123/4",
            LinkApp.Telegram to "https://t.me/+AbC",
            LinkApp.WhatsApp to "https://chat.whatsapp.com/AbC",
            LinkApp.WhatsApp to "https://api.whatsapp.com/send?phone=16505550100",
            LinkApp.Signal to "https://signal.me/#eu/AbC",
            LinkApp.Signal to "https://signal.group/#AbC",
        ).forEach { (app, link) -> assertEquals(FoundLink.Whole(LinkValue(app, "", link)), app.find(link)) }
    }

    @Test
    fun `an Obsidian link is named after its note, whatever marks the name ends with`() {
        mapOf(
            "obsidian://open?vault=My%20vault&file=Work%2FTo%20do.md" to "To do",
            "obsidian://open?vault=V&file=Costs%20(2026)" to "Costs (2026)",
            "obsidian://open?vault=V&file=What%20now!" to "What now!",
            "obsidian://open?vault=V&file=1+1" to "1+1",
            "obsidian://open?vault=V&file=Work%2FPlan.md%23To%2FDo" to "Plan",
        ).forEach { (link, name) -> assertEquals(FoundLink.Whole(LinkValue(LinkApp.Obsidian, name, link)), LinkApp.Obsidian.find(link)) }

        listOf("obsidian://open?vault=V", "obsidian://open?vault=V&file=", "obsidian://open?vault=V&file=My note", "obsidian://new?vault=V&name=A", "obsidian://open?path=%2FV%2FA")
            .forEach { assertNull(it, LinkApp.Obsidian.find(it)) }
    }

    @Test
    fun `a phone number tells a chat of an app whose chats have one`() {
        assertEquals("https://wa.me/16505550100", LinkApp.WhatsApp.linkIn("+1 (650) 555-0100"))
        assertEquals("https://signal.me/#p/+16505550100", LinkApp.Signal.linkIn("+1 650 555 0100"))
        assertEquals("https://wa.me/442079460958", LinkApp.WhatsApp.linkIn("‪+44 (0)20 7946‑0958‬"))
        assertNull(LinkApp.WhatsApp.linkIn("555 0100"))
        assertNull(LinkApp.Slack.find("+16505550100"))
    }

    @Test
    fun `no app takes another's link, nor anything that is none`() {
        val links = listOf(general.link, "https://discord.com/channels/1/2", "https://t.me/durov", "https://wa.me/1650", "https://signal.me/#eu/A", "obsidian://open?vault=V&file=A")

        links.forEach { link -> assertEquals(link, 1, LinkApp.entries.count { it.find(link) != null }) }
        listOf("", "general", "https://", "slack://open", "https://slack.com/", "https://t.me", "a%zz.slack.com/archives/C1").forEach { typed ->
            assertTrue(typed, LinkApp.entries.all { it.find(typed) == null })
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
