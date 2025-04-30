package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import com.saggiodev.kastle.model.Characters
import com.saggiodev.kastle.service.CommandManager
import com.saggiodev.kastle.service.GameState
import com.saggiodev.kastle.service.InformationManager
import com.saggiodev.kastle.model.Items
import com.saggiodev.kastle.model.Rooms
import com.saggiodev.kastle.model.nextaction.*

fun Session.handleNextAction(
    commandManager: CommandManager,
    informationManager: InformationManager,
    state: GameState,
) {
    section { textLine() }.run()
    when (commandManager.nextAction) {
        null -> {}
        DescribeCurrentRoom -> section {
            val room = Rooms.getById(state.currentRoom)!!
            textLine("You are in ${room.name.uppercase()}.")
            textLine(room.description)
        }.run()

        is DescribeInspectable -> section {
            textLine((commandManager.nextAction as DescribeInspectable).inspectable.description)
        }.run()

        EndGame -> section {}.run()
        ShowInventory -> section {
            val inventoryNames = state.inventory.map { Items.getById(it)?.name!! }
            if (inventoryNames.isEmpty()) {
                textLine("The inventory is empty")
            } else {
                inventoryNames.forEach {
                    textLine("    - ${it.uppercase()}")
                }
            }
        }.run()

        is ConfirmGrab -> section {
            textLine("${(commandManager.nextAction as ConfirmGrab).item.name.uppercase()} has been put in the inventory")
        }.run()

        is ConfirmDrop -> section {
            textLine("${(commandManager.nextAction as ConfirmDrop).item.name.uppercase()} has been dropped")
        }.run()

        DescribePlayer -> section {
            text("You are "); bold { textLine(informationManager.player.name.uppercase()) }
            textLine(informationManager.player.description)
        }.run()

        is ConfirmClose -> section {
            val action = commandManager.nextAction as ConfirmClose
            textLine(
                "The link between ${action.source.name.uppercase()} and ${action.destination.name.uppercase()}" + " has been closed"
            )
        }.run()

        is ConfirmOpen -> section {
            val action = commandManager.nextAction as ConfirmOpen
            textLine(
                "The link between ${action.source.name.uppercase()} and ${action.destination.name.uppercase()}" + " has been opened"
            )
        }.run()

        is ExecuteDialogue -> {
            val action = commandManager.nextAction as ExecuteDialogue
            val dialogue = action.dialogue
            val talkerName = Characters.getById(dialogue.talker)!!.name
            section { bold { textLine("${talkerName.uppercase()}:") } }.run()
            var choice = dialogue.first().fold(
                ifLeft = {
                    section {
                        textLine(it.question)
                        textLine()
                        question("${informationManager.player.name.uppercase()}'s response:")
                    }.run()
                    val result = choiceSelector(it.answers)
                    section { textLine() }.run()
                    result
                },
                ifRight = {
                    section { textLine(it.text); textLine() }.run()
                    if (it.reward != null) {
                        Rooms.getById(state.currentRoom)!!.addItem(it.reward!!)
                    }
                    -100 // Dummy value that should never be used
                }
            )
            while (dialogue.hasNext()) {
                section { bold { textLine("${talkerName.uppercase()}:") } }.run()
                choice = dialogue.next(choice).fold(
                    ifLeft = {
                        section {
                            textLine(it.question)
                            question("${informationManager.player.name.uppercase()}'s response:")
                        }.run()
                        choiceSelector(it.answers)
                    },
                    ifRight = {
                        section { textLine(it.text); textLine() }.run()
                        if (it.reward != null) {
                            Rooms.getById(state.currentRoom)!!.addItem(it.reward!!)
                        }
                        -100 // Dummy value that should never be used
                    }
                )
            }
        }
    }
    commandManager.resetAction()
}