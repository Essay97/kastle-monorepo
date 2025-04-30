package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.right
import com.saggiodev.kastle.service.MovementManager
import com.saggiodev.kastle.model.nextaction.DescribeCurrentRoom
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class WhereCommand(private val movementManager: MovementManager) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = DescribeCurrentRoom.right()
}