package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import com.saggiodev.kastle.service.InteractableManager
import com.saggiodev.kastle.service.InventoryManager
import com.saggiodev.kastle.model.Items
import com.saggiodev.kastle.model.nextaction.ConfirmGrab
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.KastleError

class GrabCommand(
    private val matcher: String,
    private val inventoryManager: InventoryManager,
    private val interactableManager: InteractableManager
) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = either {
        val itemId = interactableManager.getForGrab(matcher).bind()
        inventoryManager.addItem(itemId).bind()
        ConfirmGrab(Items.getStorableById(itemId)!!)
    }
}