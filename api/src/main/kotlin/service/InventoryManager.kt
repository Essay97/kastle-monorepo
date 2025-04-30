package com.saggiodev.kastle.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.saggiodev.kastle.model.ItemId
import com.saggiodev.kastle.model.Items
import com.saggiodev.kastle.model.Rooms
import com.saggiodev.kastle.error.GameRuntimeError
import com.saggiodev.kastle.error.KastleError

class InventoryManager(private val state: GameState) {

    fun addItem(itemId: ItemId): Either<KastleError, Unit> = either {
        val room = Rooms.getById(state.currentRoom)!!
        val item = ensureNotNull(Items.getById(itemId)) {
            GameRuntimeError("Item with id ${itemId.value} could not be found")
        }
        // If item is correctly removed from the room...
        ensure(room.removeItem(itemId)) { GameRuntimeError.ItemNotInRoom(item.name) }
        // ...it is added to the inventory
        state.inventory.add(itemId)
    }

    fun removeItem(itemId: ItemId): Either<KastleError, Unit> = either {
        val room = Rooms.getById(state.currentRoom)!!
        val item = ensureNotNull(Items.getById(itemId)) {
            GameRuntimeError("Item with id ${itemId.value} could not be found")
        }
        // If item is correctly removed from the inventory...
        ensure(state.inventory.remove(itemId)) { GameRuntimeError.ItemNotInInventory(item.name) }
        // ...it is added back to the room (like the player dropped it)
        room.addItem(itemId).bind()
    }
}