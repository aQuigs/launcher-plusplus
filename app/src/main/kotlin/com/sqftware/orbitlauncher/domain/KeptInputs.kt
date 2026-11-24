package com.sqftware.orbitlauncher.domain

/** How many values a prompt offers as recent. */
const val RECENT_INPUTS = 8

/**
 * The values of one kind a prompt offers before anything is typed: the user's [favorites], oldest first, and the other
 * ones last picked in [recent], latest first. No value is in both.
 */
data class KeptInputs<T : InputValue>(val favorites: List<T> = emptyList(), val recent: List<T> = emptyList()) {
    fun isFavorite(value: T) = value in favorites

    /** These with [value] picked: it goes to the top of the recent ones, unless it is a favorite. */
    fun picked(value: T) = if (isFavorite(value)) this else copy(recent = recent.withLatest(value))

    /**
     * These with [value] made a favorite, or no longer one. It then tops the recent ones, so a star cleared by mistake is
     * a tap away from being set again.
     */
    fun toggledFavorite(value: T) =
        if (isFavorite(value)) KeptInputs(favorites - value, recent.withLatest(value))
        else KeptInputs(favorites + value, recent - value)

    /** These with [value] gone from the recent ones. */
    fun forgotten(value: T) = copy(recent = recent - value)
}

private fun <T : InputValue> List<T>.withLatest(value: T) = (listOf(value) + (this - value)).take(RECENT_INPUTS)
