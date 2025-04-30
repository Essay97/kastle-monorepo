package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import com.saggiodev.kastle.service.MovementManager
import com.saggiodev.kastle.model.Direction
import com.saggiodev.kastle.model.nextaction.DescribeCurrentRoom
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class GoCommand(private val direction: Direction, private val movementManager: MovementManager) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = either {
        (when (direction) {
            Direction.NORTH -> movementManager.moveNorth()
            Direction.SOUTH -> movementManager.moveSouth()
            Direction.WEST -> movementManager.moveWest()
            Direction.EAST -> movementManager.moveEast()
        }).bind()

        DescribeCurrentRoom
    }

}