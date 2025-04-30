package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.right
import com.saggiodev.kastle.service.RunManager
import com.saggiodev.kastle.model.nextaction.EndGame
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class EndCommand(private val runManager: RunManager) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> {
        runManager.isRunning = false
        return EndGame.right()
    }

}