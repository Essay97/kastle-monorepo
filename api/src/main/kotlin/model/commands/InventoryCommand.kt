package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.right
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.model.nextaction.ShowInventory
import com.saggiodev.kastle.error.KastleError

class InventoryCommand : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = ShowInventory.right()
}