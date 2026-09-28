package com.saggiodev.kastle

import com.saggiodev.kastle.error.*
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.model.nextaction.*
import com.saggiodev.kastle.service.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*

class MovementTest : RegistryTest() {
    @Test
    fun `existing initial room creates a dungeon and go moves through an open link`() {
        val game = testGame()
        assertEquals(game.start.id, game.state.currentRoom)
        assertSame(DescribeCurrentRoom, game.commands.createGoCommand(Direction.NORTH).execute().success())
        assertEquals(game.end.id, game.state.currentRoom)
        game.movement.moveSouth().success()
        assertEquals(game.start.id, game.state.currentRoom)
    }

    @Test
    fun `missing initial room is rejected`() {
        assertIs<GameDefinitionError.UnknownInitialRoom>(
            MovementManager(GameState(roomId("r-missing")), emptyMap()).failure())
    }

    @ParameterizedTest
    @EnumSource(Direction::class)
    fun `dangling link is rejected in every direction`(direction: Direction) {
        val link = Link(roomId("r-missing"))
        val node = when (direction) {
            Direction.NORTH -> DungeonNode(north = link)
            Direction.SOUTH -> DungeonNode(south = link)
            Direction.EAST -> DungeonNode(east = link)
            Direction.WEST -> DungeonNode(west = link)
        }
        val start = roomId("r-start")
        assertIs<GameDefinitionError.IncoherentDungeonMap>(
            MovementManager(GameState(start), mapOf(start to node)).failure())
    }

    @Test
    fun `closed link and absent direction leave current room unchanged`() {
        val game = testGame(open = false)
        assertIs<GameRuntimeError.TraversingClosedLink>(game.commands.createGoCommand(Direction.NORTH).execute().failure())
        assertSame(GameRuntimeError.MissingRoomToEast, game.movement.moveEast().failure())
        assertSame(GameRuntimeError.MissingRoomToWest, game.movement.moveWest().failure())
        assertSame(GameRuntimeError.MissingRoomToSouth, game.movement.moveSouth().failure())
        assertEquals(game.start.id, game.state.currentRoom)
    }

    @ParameterizedTest
    @EnumSource(LinkBehavior::class)
    fun `door behavior controls allowed transitions even with a key`(behavior: LinkBehavior) {
        val game = testGame(behavior, open = false)
        game.inventory.addItem(itemId("i-a")).success()
        val link = game.dungeon.getValue(game.start.id).north!!
        if (behavior == LinkBehavior.OPENABLE || behavior == LinkBehavior.COMPLETE) {
            game.movement.openNorth().success()
            assertTrue(link.open)
        } else {
            assertIs<GameRuntimeError.OpeningUnopenableLink>(game.movement.openNorth().failure())
            assertFalse(link.open)
        }
        link.open = true
        if (behavior == LinkBehavior.LOCKABLE || behavior == LinkBehavior.COMPLETE) {
            game.movement.closeNorth().success()
            assertFalse(link.open)
        } else {
            assertIs<GameRuntimeError.ClosingUnclosableLink>(game.movement.closeNorth().failure())
            assertTrue(link.open)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["i-a", "i-b"])
    fun `DSL triggers are OR and mutations persist across commands and movement`(key: String) {
        val game = testGame(open = false)
        game.inventory.addItem(itemId(key)).success()
        assertEquals(listOf(itemId(key)), game.state.inventory)
        val opened = assertIs<ConfirmOpen>(game.commands.createOpenCommand(Direction.NORTH).execute().success())
        // KNOWN BUG: the manager returns current room, so both confirmation endpoints are identical.
        assertSame(game.start, opened.source)
        assertSame(game.start, opened.destination)
        game.movement.moveNorth().success()
        game.movement.moveSouth().success()
        val closed = assertIs<ConfirmClose>(game.commands.createCloseCommand(Direction.NORTH).execute().success())
        assertSame(game.start, closed.source)
        assertSame(game.start, closed.destination)
        assertFalse(game.dungeon.getValue(game.start.id).north!!.open)
        assertIs<GameRuntimeError.TraversingClosedLink>(game.movement.moveNorth().failure())
    }

    @Test
    fun `missing keys prevent opening and closing without mutating the door`() {
        val game = testGame(open = false)
        val link = game.dungeon.getValue(game.start.id).north!!
        assertIs<GameRuntimeError.NoOpenTriggerOwned>(game.movement.openNorth().failure())
        assertFalse(link.open)
        link.open = true
        assertIs<GameRuntimeError.NoCloseTriggerOwned>(game.movement.closeNorth().failure())
        assertTrue(link.open)
    }

    @Test
    fun `current behavior - empty trigger list prevents both opening and closing`() {
        val game = testGame(triggers = emptyList())
        assertIs<GameRuntimeError.NoCloseTriggerOwned>(game.movement.closeNorth().failure())
        assertIs<GameRuntimeError.NoOpenTriggerOwned>(game.movement.openNorth().failure())
    }
}
