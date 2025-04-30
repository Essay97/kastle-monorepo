package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import com.saggiodev.kastle.service.InteractableManager
import com.saggiodev.kastle.model.nextaction.DescribeInspectable
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class InspectCommand(
    private val matcher: String,
    private val interactableManager: InteractableManager
) : GameCommand() {

    override fun execute(): Either<KastleError, NextAction> = either {
        DescribeInspectable(interactableManager.getForInspection(matcher).bind())
    }
}