package com.saggiodev.kastle.example

import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.dto.GameConfiguration
import com.saggiodev.kastle.dto.LinkState
import com.saggiodev.kastle.model.LinkBehavior
import com.saggiodev.kastle.service.GameProvider

class TreasureIsland : GameProvider {
    override fun provideConfiguration(): GameConfiguration = game("r-start") {
        // Create a room with "r-start" ID
        room("r-start") {
            name = "Initial room"
            description = "This is the first room that the player sees"
            north("r-next") {
                behavior = LinkBehavior.CONSTANT
                state = LinkState.OPEN
            }
        }
        room("r-next") {
            name = "Another room"
            description = "This is a room that the player can move to"
        }

        winIf {
            playerEnters = "r-next"
        }

        player {
            name = "Enrico"
            description = "Enrico is the author of Kastle and also an awesome hero for this game"
        }
    }
}