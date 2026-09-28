package com.saggiodev.kastle.model.commands

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.saggiodev.kastle.service.GameState
import com.saggiodev.kastle.service.InventoryManager
import com.saggiodev.kastle.model.Items
import com.saggiodev.kastle.model.capabilities.matches
import com.saggiodev.kastle.model.nextaction.ConfirmDrop
import com.saggiodev.kastle.model.nextaction.NextAction
import com.saggiodev.kastle.error.GameRuntimeError
import com.saggiodev.kastle.error.KastleError

class DropCommand(
    private val matcher: String,
    private val inventoryManager: InventoryManager,
    private val state: GameState
) : GameCommand() {
    override fun execute(): Either<KastleError, NextAction> = either {
        val itemId = state.inventory.find { Items.getById(it)!!.matchers.matches(matcher) }
        ensureNotNull(itemId) { GameRuntimeError.ItemNotInInventory(matcher) }
        inventoryManager.removeItem(itemId)
        ConfirmDrop(Items.getStorableById(itemId)!!)
    }
}