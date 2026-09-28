package com.saggiodev.kastle.model.capabilities

interface Inspectable {
    val description: String
    val matchers: List<String>
}

/** Exact, case-insensitive matching with surrounding whitespace ignored. */
internal fun List<String>.matches(matcher: String): Boolean =
    any { it.trim().equals(matcher.trim(), ignoreCase = true) }
