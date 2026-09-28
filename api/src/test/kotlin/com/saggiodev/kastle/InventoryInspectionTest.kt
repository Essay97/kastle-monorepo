package com.saggiodev.kastle

import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.error.*
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.model.nextaction.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class InventoryInspectionTest : RegistryTest() {
    @Test
    fun `grab transfers a storable item and drop puts it in the current room`() {
        val game = testGame()
        val key = itemId("i-a")
        val grabbed = assertIs<ConfirmGrab>(game.commands.createGrabCommand("key").execute().success())
        assertEquals(key, grabbed.item.id)
        assertFalse(key in game.start.items)
        assertEquals(listOf(key), game.state.inventory)
        game.movement.moveNorth().success()
        val dropped = assertIs<ConfirmDrop>(game.commands.createDropCommand("key").execute().success())
        assertSame(grabbed.item, dropped.item)
        assertTrue(game.state.inventory.isEmpty())
        assertEquals(listOf(key), game.end.items.toList())
        assertFalse(key in game.start.items)
    }

    @Test
    fun `non storable grab and unowned drop fail without changing state`() {
        val game = testGame()
        val original = game.start.items.toList()
        assertIs<GameRuntimeError.CannotFindStorable>(game.commands.createGrabCommand("statue").execute().failure())
        assertIs<GameRuntimeError.ItemNotInInventory>(game.commands.createDropCommand("key").execute().failure())
        assertIs<GameRuntimeError.ItemNotInInventory>(game.inventory.removeItem(itemId("i-a")).failure())
        assertTrue(game.state.inventory.isEmpty())
        assertEquals(original, game.start.items.toList())
    }

    @Test
    fun `manager cannot grab the same item twice`() {
        val game = testGame()
        game.inventory.addItem(itemId("i-a")).success()
        assertIs<GameRuntimeError.ItemNotInRoom>(game.inventory.addItem(itemId("i-a")).failure())
        assertEquals(listOf(itemId("i-a")), game.state.inventory)
    }

    @Test
    fun `inspection finds current room room items inventory items and characters`() {
        val game = testGame()
        fun inspect(matcher: String) = assertIs<DescribeInspectable>(
            game.commands.createInspectCommand(matcher).execute().success()).inspectable
        assertSame(game.start, inspect("room"))
        assertSame<com.saggiodev.kastle.model.capabilities.Inspectable?>(Items.getById(itemId("i-a")), inspect("key"))
        assertSame<com.saggiodev.kastle.model.capabilities.Inspectable?>(Characters.getById(CharacterId("c-guard").success()), inspect("guard"))
        game.inventory.addItem(itemId("i-a")).success()
        game.movement.moveNorth().success()
        assertSame(game.end, inspect("room"))
        assertSame<com.saggiodev.kastle.model.capabilities.Inspectable?>(Items.getById(itemId("i-a")), inspect("key"))
        assertIs<GameRuntimeError.CannotFindInspectable>(game.interactables.getForInspection("guard").failure())
        assertIs<GameRuntimeError.CannotFindInspectable>(game.commands.createInspectCommand("missing").execute().failure())
    }

    @Test
    fun `KNOWN BUG - DSL name Key does not supply matcher key or ID`() {
        val game = TestGame(game("r-start") {
            room("r-start") { item("i-key") { name = "Key" } }
        })
        val key = Items.getById(itemId("i-key"))!!
        assertEquals("Key", key.name)
        assertTrue(key.matchers.isEmpty())
        for (matcher in listOf("key", "Key", "i-key")) {
            assertIs<GameRuntimeError.CannotFindInspectable>(game.commands.createInspectCommand(matcher).execute().failure())
        }
    }

    @Test
    fun `room creation rejects unregistered items and characters`() {
        assertIs<GameDefinitionError>(Room(roomId("r-start"), "Start", "", items = listOf(itemId("i-missing"))).failure())
        assertIs<GameDefinitionError>(Room(roomId("r-start"), "Start", "", characters = listOf(CharacterId("c-missing").success())).failure())
    }

    @Test
    fun `internal reset removes entries from all registries`() {
        val game = testGame()
        Rooms.clear()
        Items.clear()
        Characters.clear()
        assertNull(Rooms.getById(game.state.currentRoom))
        assertNull(Items.getById(itemId("i-a")))
        assertNull(Characters.getById(CharacterId("c-guard").success()))
    }
}
