package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import com.saggiodev.kastle.service.GameState
import com.saggiodev.kastle.service.MovementManager
import com.saggiodev.kastle.model.Direction
import com.saggiodev.kastle.model.Rooms
import com.saggiodev.kastle.model.nextaction.ConfirmClose
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class CloseCommand(
    private val direction: Direction, private val movementManager: MovementManager, val state: GameState
) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = either {
        val destination = (when (direction) {
            Direction.NORTH -> movementManager.closeNorth()
            Direction.SOUTH -> movementManager.closeSouth()
            Direction.WEST -> movementManager.closeWest()
            Direction.EAST -> movementManager.closeEast()
        }).bind()
        ConfirmClose(Rooms.getById(state.currentRoom)!!, destination)
    }
}