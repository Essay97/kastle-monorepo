package com.saggiodev.kastle.cli.commands

import arrow.core.getOrElse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.saggiodev.kastle.service.InstallationManager

class List : CliktCommand() {

    override fun help(context: Context): String = "List all installed games with their file definition location"

    private val installationManager = InstallationManager().getOrElse {
        echo(it.description, err = true)
        throw ProgramResult(4)
    }

    override fun run() {
        val games = installationManager.getGames()

        if (games.isNotEmpty()) {
            games.forEach {
                echo(it.gameName)
            }
        } else {
            echo("No games found")
        }

    }
}