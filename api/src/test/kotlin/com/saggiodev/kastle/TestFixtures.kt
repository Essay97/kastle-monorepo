package com.saggiodev.kastle

import arrow.core.Either
import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.dto.*
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.model.commands.CommandFactory
import com.saggiodev.kastle.service.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.fail

fun <L, R> Either<L, R>.success(): R = fold({ fail("Expected Right, got $it") }, { it })
fun <L, R> Either<L, R>.failure(): L = fold({ it }, { fail("Expected Left, got $it") })
fun roomId(value: String) = RoomId(value).success()
fun itemId(value: String) = ItemId(value).success()

abstract class RegistryTest {
    @BeforeEach
    fun resetBefore() = clearRegistries()

    @AfterEach
    fun resetAfter() = clearRegistries()

    private fun clearRegistries() {
        Rooms.clear()
        Items.clear()
        Characters.clear()
    }
}

// Use the production mappers in the same loading order as ConfigurationManager,
// without loading installed games from the user's home or creating a database.
class TestGame(config: GameConfiguration) {
    init {
        config.characters.orEmpty().forEach { Characters.add(it.toCharacter().success()) }
        config.items.orEmpty().forEach { Items.add(it.toItem().success()) }
        config.rooms.forEach { Rooms.add(it.toRoom().success()) }
    }
    val dungeon = config.rooms.associate {
        roomId(it.id) to DungeonNode(
            north = it.links?.north?.toLink()?.success(),
            south = it.links?.south?.toLink()?.success(),
            east = it.links?.east?.toLink()?.success(),
            west = it.links?.west?.toLink()?.success()
        )
    }
    val state = GameState(roomId(config.initialRoomId))
    val movement = MovementManager(state, dungeon).success()
    val inventory = InventoryManager(state)
    val interactables = InteractableManager(state)
    val run = RunManager(config.winningConditions?.toWinningConditions()?.success(), state)
    val commands = CommandFactory(movement, run, interactables, inventory, state)
    val start get() = Rooms.getById(roomId("r-start"))!!
    val end get() = Rooms.getById(roomId("r-end"))!!
}

fun testGame(
    behavior: LinkBehavior = LinkBehavior.COMPLETE,
    open: Boolean = true,
    triggers: List<String> = listOf("i-a", "i-b")
) = TestGame(game("r-start") {
    room("r-start") {
        description = "Starting room"
        north("r-end") {
            this.behavior = behavior
            state = if (open) LinkState.OPEN else LinkState.LOCKED
            triggers(*triggers.toTypedArray())
        }
        item("i-a") { name = "Key"; matchers("key"); storable = true }
        item("i-b") { matchers("spare"); storable = true }
        item("i-statue") { matchers("statue") }
        character("c-guard") { matchers("guard"); description = "A silent guard" }
    }
    room("r-end") { south("r-start") }
})
