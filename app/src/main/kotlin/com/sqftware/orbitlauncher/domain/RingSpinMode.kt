package com.sqftware.orbitlauncher.domain

/** What spins the ring like a fidget spinner, as the user picks it in the launcher's menu. */
enum class RingSpinMode(val label: String) {
    Off("Off"),

    /** A drag round the ring after a brief press, so a page swipe across the ring still turns the page. */
    AfterPress("After a press"),

    /** Any drag round the ring, a page swipe across its top or bottom included. */
    AnyDrag("Any drag"),
}
