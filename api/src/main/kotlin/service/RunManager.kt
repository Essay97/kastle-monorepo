package com.saggiodev.kastle.service

import com.saggiodev.kastle.model.WinningConditions

class RunManager(
    private val winningConditions: WinningConditions?,
    private val state: GameState,
) {
    var isRunning = true
    var win = false
        private set
        get() {
            val winsForRoom =
                winningConditions?.playerEnters != null && winningConditions.playerEnters == state.currentRoom
            val winsForItem =
                winningConditions?.playerOwns != null && winningConditions.playerOwns in state.inventory
            return winsForRoom || winsForItem
        }
}