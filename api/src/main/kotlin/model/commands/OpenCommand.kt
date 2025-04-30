package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import com.saggiodev.kastle.service.GameState
import com.saggiodev.kastle.service.MovementManager
import com.saggiodev.kastle.model.Direction
import com.saggiodev.kastle.model.Rooms
import com.saggiodev.kastle.model.nextaction.ConfirmOpen
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class OpenCommand(private val direction: Direction, private val movementManager: MovementManager, private val state: GameState) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = either {
        val destination = (when (direction) {
            Direction.NORTH -> movementManager.openNorth()
            Direction.SOUTH -> movementManager.openSouth()
            Direction.WEST -> movementManager.openWest()
            Direction.EAST -> movementManager.openEast()
        }).bind()
        ConfirmOpen(Rooms.getById(state.currentRoom)!!, destination)
    }
}