package com.saggiodev.kastle.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.saggiodev.kastle.error.GameDefinitionError

class WinningConditions private constructor(
    val playerOwns: ItemId? = null,
    val playerEnters: RoomId? = null
) {
    companion object {
        operator fun invoke(playerOwns: ItemId?, playerEnters: RoomId?):
                Either<GameDefinitionError.IncoherentWinningCondition, WinningConditions> = either {
            if (playerOwns != null) {
                ensureNotNull(Items.getById(playerOwns)) { GameDefinitionError.IncoherentItemWin(playerOwns.value) }
            }
            if (playerEnters != null) {
                ensureNotNull(Rooms.getById(playerEnters)) { GameDefinitionError.IncoherentRoomWin(playerEnters.value) }
            }
            WinningConditions(playerOwns, playerEnters)
        }
    }
}