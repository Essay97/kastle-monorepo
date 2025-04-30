package com.saggiodev.kastle.cli.commands

import arrow.core.getOrElse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.saggiodev.kastle.service.InstallationManager

class Info : CliktCommand() {

    override fun help(context: Context): String = "Get information about a specific game"

    private val game by argument()

    private val installationManager = InstallationManager().getOrElse {
        echo(it.description, err = true)
        throw ProgramResult(4)
    }

    override fun run() {
        val gameInfo = installationManager.getByGameName(game).getOrElse {
            echo(it.description, err = true)
            throw ProgramResult(6)
        }
        echo(gameInfo.gameName)
        echo("Game file: ${gameInfo.fileName}")
        echo("Main class: ${gameInfo.mainClass}")
    }
}