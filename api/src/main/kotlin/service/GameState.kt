package com.saggiodev.kastle.service

import com.saggiodev.kastle.model.ItemId
import com.saggiodev.kastle.model.RoomId

data class GameState(var currentRoom: RoomId) {
    val inventory =  mutableListOf<ItemId>()
}