package com.saggiodev.kastle.cli.components

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.runtime.Session
import com.saggiodev.kastle.cli.ingamecommands.gameCommandCompletions
import com.saggiodev.kastle.cli.ingamecommands.gameCommands
import com.saggiodev.kastle.error.PlayerError

fun Session.nextTurn(): Either<PlayerError, Pair<String, String?>> {
    var parsedCommand: Either<PlayerError, Pair<String, String?>> = PlayerError("Generic player error").left()

    section {
        question("What do you want to do?")
        prompt()
        input(gameCommandCompletions)

    }.runUntilInputEntered {
        onInputEntered {
            parsedCommand = parseGameCommand(input)
        }
    }

    return parsedCommand
}

fun parseGameCommand(input: String): Either<PlayerError, Pair<String, String?>> = either {
    val trimmedInput = input.trim()
    // Find the matching command
    val command = gameCommands.find { trimmedInput.startsWith(it) }

    ensureNotNull(command) { PlayerError.UnknownGameCommand(input) }

    // Extract the argument (if any)
    val argument = trimmedInput.removePrefix(command).trim()
    // Return the command and argument pair
    Pair(command, argument.ifBlank { null })
}
