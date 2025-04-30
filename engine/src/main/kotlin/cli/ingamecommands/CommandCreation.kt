package com.saggiodev.kastle.cli.ingamecommands

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.saggiodev.kastle.model.Direction
import com.saggiodev.kastle.model.commands.CommandFactory
import com.saggiodev.kastle.model.commands.GameCommand
import com.saggiodev.kastle.error.PlayerError

fun getCommand(command: String, argument: String?, factory: CommandFactory):
        Either<PlayerError, GameCommand> =
    when (command) {
        "go" -> either {
            ensureNotNull(argument) { PlayerError.NoGoDirection }
            val direction = directionFromArgument(argument).bind()
            factory.createGoCommand(direction)
        }

        "end" -> either {
            ensure(argument == null) { PlayerError.CommandRequiresNoArguments("end", argument!!) }
            factory.createEndCommand()
        }

        "inspect" -> either {
            ensureNotNull(argument) { PlayerError.NoInspectableSpecified }
            factory.createInspectCommand(argument)
        }

        "inventory" -> either {
            ensure(argument == null) { PlayerError.CommandRequiresNoArguments("inventory", argument!!) }
            factory.createInventoryCommand()
        }

        "grab" -> either {
            ensureNotNull(argument) { PlayerError.NoStorableSpecified }
            factory.createGrabCommand(argument)
        }

        "drop" -> either {
            ensureNotNull(argument) { PlayerError.NoDroppableSpecified }
            factory.createDropCommand(argument)
        }

        "where" -> either {
            ensure(argument == null) { PlayerError.CommandRequiresNoArguments("where", argument!!) }
            factory.createWhereCommand()
        }

        "who" -> either {
            ensure(argument == null) { PlayerError.CommandRequiresNoArguments("who", argument!!) }
            factory.createWhoCommand()
        }

        "open" -> either {
            ensureNotNull(argument) { PlayerError.NoOpenDirection }
            val direction = directionFromArgument(argument).bind()
            factory.createOpenCommand(direction)
        }

        "close" -> either {
            ensureNotNull(argument) { PlayerError.NoCloseDirection }
            val direction = directionFromArgument(argument).bind()
            factory.createCloseCommand(direction)
        }

        "talk" -> either {
            ensureNotNull(argument) { PlayerError.NoTalkerSpecified }
            factory.createTalkCommand(argument)
        }

        else -> PlayerError.UnknownGameCommand("$command $argument").left()
    }

private fun directionFromArgument(argument: String): Either<PlayerError.UnknownGoDirection, Direction> =
    when (argument) {
        "north" -> Direction.NORTH.right()
        "south" -> Direction.SOUTH.right()
        "east" -> Direction.EAST.right()
        "west" -> Direction.WEST.right()
        else -> PlayerError.UnknownGoDirection(argument).left()
    }
