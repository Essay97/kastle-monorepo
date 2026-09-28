package com.saggiodev.kastle

import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.error.GameRuntimeError
import com.saggiodev.kastle.model.Direction
import com.saggiodev.kastle.model.nextaction.*
import com.saggiodev.kastle.service.CommandManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.*

class WinningAndCommandsTest : RegistryTest() {
    @ParameterizedTest
    @CsvSource(
        "true,false,true,false,true", // room only
        "false,true,false,true,true", // item only
        "true,true,false,false,false",
        "true,true,true,false,true", // both configured: room alone is sufficient
        "true,true,false,true,true", // both configured: item alone is sufficient
        "true,true,true,true,true",
        "false,false,true,true,false"
    )
    fun `winning conditions use OR not AND`(roomCondition: Boolean, itemCondition: Boolean, enter: Boolean, grab: Boolean, wins: Boolean) {
        val game = TestGame(game("r-start") {
            room("r-start") {
                north("r-end")
                item("i-prize") { storable = true; matchers("prize") }
            }
            room("r-end") {}
            winIf {
                if (roomCondition) playerEnters = "r-end"
                if (itemCondition) playerOwns = "i-prize"
            }
        })
        assertFalse(game.run.win)
        if (grab) game.commands.createGrabCommand("prize").execute().success()
        if (enter) game.commands.createGoCommand(Direction.NORTH).execute().success()
        assertEquals(wins, game.run.win)
    }

    @Test
    fun `no configured conditions never wins and end stops running`() {
        val game = testGame()
        assertFalse(game.run.win)
        assertTrue(game.run.isRunning)
        assertSame(EndGame, game.commands.createEndCommand().execute().success())
        assertFalse(game.run.isRunning)
        assertFalse(game.run.win)
    }

    @Test
    fun `command manager records history and clears stale errors and actions`() {
        val game = testGame()
        val manager = CommandManager()
        val missing = game.commands.createInspectCommand("missing")
        manager.submit(missing)
        assertIs<GameRuntimeError.CannotFindInspectable>(manager.error)
        assertNull(manager.nextAction)
        val grab = game.commands.createGrabCommand("key")
        manager.submit(grab)
        assertNull(manager.error)
        assertIs<ConfirmGrab>(manager.nextAction)
        assertEquals(listOf(itemId("i-a")), game.state.inventory)
        assertEquals(listOf(missing, grab), manager.history)
        manager.resetAction()
        assertNull(manager.nextAction)
    }
}
