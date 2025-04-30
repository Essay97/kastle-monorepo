package com.saggiodev.kastle.cli.commands

import arrow.core.getOrElse
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.saggiodev.kastle.cli.ingamecommands.getCommand
import com.saggiodev.kastle.cli.components.*
import com.saggiodev.kastle.service.InstallationManager
import com.saggiodev.kastle.service.ConfigurationManager
import com.saggiodev.kastle.model.commands.CommandFactory

class Play : CliktCommand() {

    override fun help(context: Context): String = "Start playing with the specified game"

    private val installationManager = InstallationManager().getOrElse {
        echo(it.description, err = true)
        throw ProgramResult(4)
    }
    private val configurationManager = ConfigurationManager()

    private val gameName by argument(help = "The name that will be used to reference and run the game")

    override fun run() {
        /*
         * Setup dependencies
         */
        val gameClass = installationManager.getByGameName(gameName).getOrElse {
            echo(it.description, err = true)
            throw ProgramResult(3)
        }.mainClass
        val (commands, interactables, movement, information, running, inventory, state) =
            configurationManager.getManagersForGameClass(gameClass).getOrElse {
                echo(it.description, err = true)
                throw ProgramResult(3)
            }

        val factory = CommandFactory(movement, running, interactables, inventory, state)

        /*
         * Start rendering the game
         */
        session {
            header(information.metadata, gameName)

            section {
                // Usually the preface carries its own newline at the end
                text(information.preface)
            }.run()

            do {
                if (!running.win) {
                    handleNextAction(commands, information, state)
                    nextTurn().fold(
                        ifLeft = { showError(it.description) },
                        ifRight = {
                            getCommand(it.first, it.second, factory)
                                .onLeft { error ->
                                    showError(error.description)
                                }
                                .onRight { command ->
                                    commands.submit(command)
                                    if (commands.error != null) {
                                        showWarning(commands.error!!.description)
                                    }
                                }
                        }
                    )
                } else {
                    section {
                        textLine(information.epilogue)
                        textLine()
                        cyan { bold { textLine("You won!") } }
                    }.run()
                    commands.submit(factory.createEndCommand())
                }
            } while (running.isRunning)


            // TODO add saving game logic here
            section {
                textLine("Closing the game, see you on the next adventure!")
            }.run()
        }
    }
}