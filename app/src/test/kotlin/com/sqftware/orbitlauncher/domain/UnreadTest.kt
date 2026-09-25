package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnreadTest {
    private val mail = app("Mail")
    private val chat = app("Chat")

    private fun posted(app: AppEntry, summary: Boolean = false, ongoing: Boolean = false, number: Int = 0) =
        PostedNotification(app.packageName, isGroupSummary = summary, isOngoing = ongoing, number = number)

    @Test
    fun `each notification counts one for its package`() {
        assertEquals(
            UnreadCounts(mapOf(mail.packageName to 2, chat.packageName to 1)),
            unreadCounts(listOf(posted(mail), posted(chat), posted(mail))),
        )
    }

    @Test
    fun `a group summary is not counted on top of its members`() {
        assertEquals(
            UnreadCounts(mapOf(chat.packageName to 2)),
            unreadCounts(listOf(posted(chat, summary = true), posted(chat), posted(chat))),
        )
    }

    @Test
    fun `an ongoing notification is not unread`() {
        assertEquals(UnreadCounts(), unreadCounts(listOf(posted(mail, ongoing = true))))
    }

    @Test
    fun `a notification with a number counts that many and one without counts one`() {
        assertEquals(UnreadCounts(mapOf(mail.packageName to 8)), unreadCounts(listOf(posted(mail, number = 7), posted(mail, number = 1))))
        assertEquals(UnreadCounts(mapOf(mail.packageName to 1)), unreadCounts(listOf(posted(mail, number = -3))))
    }

    @Test
    fun `no notifications means no counts`() {
        assertEquals(UnreadCounts(), unreadCounts(emptyList()))
    }

    @Test
    fun `an app reads its own count and a folder sums its packages once each`() {
        val counts = UnreadCounts(mapOf(mail.packageName to 3, chat.packageName to 2))
        val mailAgain = mail.copy(label = "Mail again", activityName = "pkg.Mail.Other")

        assertEquals(3, counts[mail])
        assertEquals(0, counts[app("Maps")])
        assertEquals(5, counts.sum(listOf(mail, chat, mailAgain)))
        assertEquals(0, counts.sum(emptyList()))
    }

    @Test
    fun `a pinned shortcut counts none, alone or in a folder`() {
        val counts = UnreadCounts(mapOf(chat.packageName to 4))
        val pinnedChat = chat.copy(label = "Sam", shortcutId = "sam")

        assertEquals(0, counts[pinnedChat])
        assertEquals(0, counts.sum(listOf(pinnedChat)))
        assertEquals(4, counts.sum(listOf(pinnedChat, chat)))
    }

    @Test
    fun `a dismissed notification is kept, a tapped one clears its app and a withdrawn one changes nothing`() {
        val kept = Kept().afterRemoval("m1", posted(mail), Removal.Dismissed).afterRemoval("c1", posted(chat), Removal.Dismissed)
        assertEquals(UnreadCounts(mapOf(mail.packageName to 1, chat.packageName to 1)), kept.counts)

        val more = kept.afterRemoval("m2", posted(mail, number = 5), Removal.Dismissed)
        assertEquals(UnreadCounts(mapOf(mail.packageName to 6, chat.packageName to 1)), more.counts)
        assertEquals(UnreadCounts(mapOf(chat.packageName to 1)), more.afterRemoval("m3", posted(mail), Removal.Opened).counts)
        assertEquals(more, more.afterRemoval("m3", posted(mail), Removal.Withdrawn))
    }

    @Test
    fun `a dismissed group summary or ongoing notification is not kept`() {
        assertEquals(Kept(), Kept().afterRemoval("s", posted(chat, summary = true), Removal.Dismissed))
        assertEquals(Kept(), Kept().afterRemoval("o", posted(chat, ongoing = true), Removal.Dismissed))
    }

    @Test
    fun `a notification posted again replaces its kept count, and an opened app drops out`() {
        val kept = Kept().afterRemoval("m", posted(mail, number = 3), Removal.Dismissed).afterRemoval("c", posted(chat), Removal.Dismissed)

        assertEquals(UnreadCounts(mapOf(chat.packageName to 1)), kept.posted("m").counts)
        assertEquals(UnreadCounts(mapOf(mail.packageName to 3)), kept.opened(chat.packageName).counts)
        assertEquals(UnreadCounts(mapOf(mail.packageName to 4, chat.packageName to 1)), UnreadCounts(mapOf(mail.packageName to 1)) + kept.counts)
    }

    @Test
    fun `a group's members count as dismissed only after the user dismissed its summary`() {
        val removals = Removals()

        assertEquals(Removal.Withdrawn, removals.removed("g", isGroupSummary = false, RemovalCause.SummaryRemoved))
        assertEquals(Removal.Dismissed, removals.removed("g", isGroupSummary = true, RemovalCause.UserDismissed))
        assertEquals(Removal.Dismissed, removals.removed("g", isGroupSummary = false, RemovalCause.SummaryRemoved))
        assertEquals(Removal.Withdrawn, removals.removed("other", isGroupSummary = false, RemovalCause.SummaryRemoved))

        removals.posted("g")
        assertEquals(Removal.Withdrawn, removals.removed("g", isGroupSummary = false, RemovalCause.SummaryRemoved))
    }

    @Test
    fun `a tap opens, a swipe of a lone notification dismisses, and anything else withdraws`() {
        val removals = Removals()

        assertEquals(Removal.Opened, removals.removed("g", isGroupSummary = false, RemovalCause.Tapped))
        assertEquals(Removal.Dismissed, removals.removed("g", isGroupSummary = false, RemovalCause.UserDismissed))
        assertEquals(Removal.Withdrawn, removals.removed("g", isGroupSummary = false, RemovalCause.Other))
        assertEquals(Removal.Withdrawn, removals.removed("g", isGroupSummary = false, RemovalCause.SummaryRemoved))
    }

    @Test
    fun `a badge shows the count up to two digits`() {
        assertEquals("1", badgeText(1))
        assertEquals("99", badgeText(99))
        assertEquals("99+", badgeText(100))
        assertEquals("99+", badgeText(1234))
    }
}
