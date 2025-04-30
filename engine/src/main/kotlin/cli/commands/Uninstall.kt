package com.saggiodev.kastle.cli.commands

import arrow.core.getOrElse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.saggiodev.kastle.service.InstallationManager

class Uninstall : CliktCommand() {

    override fun help(context: Context): String = "Uninstall an installed game"

    private val gameName by argument()

    private val configManager = InstallationManager().getOrElse {
        echo(it.description, err = true)
        throw ProgramResult(4)
    }

    override fun run() {
        configManager.uninstallGame(gameName).getOrElse {
            echo(it.description, err = true)
            throw ProgramResult(6)
        }
        echo("$gameName uninstalled correctly.")
    }
}