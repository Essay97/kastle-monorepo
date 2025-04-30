package com.saggiodev.kastle.cli.commands

import arrow.core.getOrElse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.saggiodev.kastle.service.InstallationManager
import kotlin.io.path.nameWithoutExtension

class Install : CliktCommand() {

    override fun help(context: Context): String = "Install a new game"

    private val gameClass by argument()
    private val gamePath by argument().path(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )

    private val gameName by option("-n", "--name")

    private val installationManager: InstallationManager = InstallationManager().getOrElse {
        echo(it.description, err = true)
        throw ProgramResult(4)
    }

    override fun run() {
        installationManager.installGame(gameName ?: gamePath.nameWithoutExtension, gamePath, gameClass).getOrElse {
            echo(it.description, err = true)
            throw ProgramResult(2)
        }
        echo("$gameName installed correctly.")
    }
}