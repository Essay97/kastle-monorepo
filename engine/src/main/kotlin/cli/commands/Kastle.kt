package com.saggiodev.kastle.cli.commands

import com.github.ajalt.clikt.core.NoOpCliktCommand
import kotlin.collections.List

class Kastle : NoOpCliktCommand() {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "ls" to listOf("list")
    )
}