package com.saggiodev.kastle

import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.dto.*
import com.saggiodev.kastle.error.GameDefinitionError
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.service.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

class ValidationProvider : GameProvider {
    override fun provideConfiguration() = configuration
    companion object { lateinit var configuration: GameConfiguration }
}

class DefinitionValidationTest : RegistryTest() {
    @TempDir lateinit var home: Path

    private fun load(config: GameConfiguration) = run {
        ValidationProvider.configuration = config
        val games = home.resolve(".kastle/games").toFile().apply { mkdirs() }
        JarOutputStream(games.resolve("fixture.jar").outputStream()).use {
            it.putNextEntry(JarEntry("META-INF/services/${GameProvider::class.java.name}"))
            it.write(ValidationProvider::class.java.name.toByteArray())
            it.closeEntry()
        }
        val previous = System.getProperty("user.home")
        try {
            System.setProperty("user.home", home.toString())
            ConfigurationManager().getManagersForGameClass(ValidationProvider::class.java.name)
        } finally { System.setProperty("user.home", previous) }
    }

    private fun config(
        rooms: List<RoomDto> = listOf(RoomDto("Start", "r-start")),
        items: List<ItemDto> = emptyList(),
        characters: List<CharacterDto> = emptyList(),
        initial: String = "r-start",
        win: WinningConditionsDto? = null
    ) = GameConfiguration(rooms, PlayerDto("Player"), initial, items, characters, null, win, null, null)

    @Test fun `duplicate definitions fail before registries change`() {
        val error = assertIs<GameDefinitionError>(load(config(
            rooms = listOf(RoomDto("A", "r-start"), RoomDto("B", "r-start")),
            items = listOf(ItemDto("A", "i-key"), ItemDto("B", "i-key")),
            characters = listOf(CharacterDto("c-guide", "A", null), CharacterDto("c-guide", "B", null))
        )).failure())
        listOf("r-start", "i-key", "c-guide").forEach { assertContains(error.description, it) }
        assertNull(Rooms.getById(roomId("r-start")))
        assertNull(Items.getById(itemId("i-key")))
        assertNull(Characters.getById(CharacterId("c-guide").success()))
    }

    @Test fun `unknown references accumulate including stale registry entries`() {
        TestGame(game("r-old") { room("r-old") { item("i-old") {}; character("c-old") {} } })
        val error = assertIs<GameDefinitionError>(load(config(
            rooms = listOf(RoomDto("Start", "r-start", LinksDto(north = DirectionDto("r-old",
                DirectionStateDto(LinkState.OPEN, LinkBehavior.CONSTANT, listOf("i-trigger")))),
                items = listOf("i-old"), characters = listOf("c-old"))),
            initial = "r-missing", win = WinningConditionsDto("i-win", "r-win")
        )).failure())
        listOf("r-old", "i-trigger", "i-old", "c-old", "r-missing", "i-win", "r-win").forEach {
            assertContains(error.description, it)
        }
        assertNull(Rooms.getById(roomId("r-start")))
        assertNotNull(Rooms.getById(roomId("r-old")))
    }

    @Test fun `all dialogue definitions are validated including unreachable questions`() {
        val dialogue = DialogueDto("d-missing", listOf(
            QuestionDto("d-one", "One", listOf(AnswerDto("Go", "d-unknown")), "i-reward"),
            QuestionDto("d-one", "Duplicate"),
            QuestionDto("bad-id", "Bad"),
            QuestionDto("d-empty", "Empty", emptyList())
        ))
        val error = assertIs<GameDefinitionError>(load(config(characters = listOf(
            CharacterDto("c-guide", "Guide", null, dialogue = dialogue)
        ))).failure())
        listOf("d-missing", "d-one", "d-unknown", "i-reward", "bad-id", "d-empty").forEach {
            assertContains(error.description, it)
        }
    }

    @Test fun `dialogue cycles fail without recursive mapping`() {
        val dialogue = DialogueDto("d-end", listOf(
            QuestionDto("d-end", "End"),
            QuestionDto("d-a", "A", listOf(AnswerDto("Go", "d-b"))),
            QuestionDto("d-b", "B", listOf(AnswerDto("Go", "d-a")))
        ))
        assertContains(assertIs<GameDefinitionError>(load(config(characters = listOf(
            CharacterDto("c-guide", "Guide", null, dialogue = dialogue)
        ))).failure()).description, "cycle")
    }

    @Test fun `malformed entity and reward IDs return definition errors`() {
        val error = assertIs<GameDefinitionError>(load(config(
            rooms = listOf(RoomDto("Bad", "invalid-room")),
            items = listOf(ItemDto("Bad", "invalid-item")),
            characters = listOf(CharacterDto("invalid-character", "Bad", null,
                dialogue = DialogueDto("d-end", listOf(QuestionDto("d-end", "End", reward = "invalid-reward")))))
        )).failure())
        listOf("invalid-room", "invalid-item", "invalid-character", "invalid-reward").forEach {
            assertContains(error.description, it)
        }
    }

    @Test fun `reachable cycles including a branch with an exit are rejected`() {
        val dialogue = DialogueDto("d-start", listOf(
            QuestionDto("d-start", "Start", listOf(AnswerDto("Again", "d-start"), AnswerDto("Exit", "d-end"))),
            QuestionDto("d-end", "End")
        ))
        assertContains(assertIs<GameDefinitionError>(load(config(characters = listOf(
            CharacterDto("c-guide", "Guide", null, dialogue = dialogue)
        ))).failure()).description, "d-start")
    }

    @Test fun `reward definitions participate in global item uniqueness`() {
        val definition = game("r-start") {
            room("r-start") {
                item("i-key") {}
                character("c-guide") {
                    dialogue { firstQuestion("d-end") { reward("i-key") {} } }
                }
            }
        }
        val error = assertIs<GameDefinitionError>(load(definition).failure())
        assertContains(error.description, "duplicate ID 'i-key'")
        assertNull(Items.getById(itemId("i-key")))
    }

    @Test fun `empty games and dialogues fail with definition errors`() {
        assertIs<GameDefinitionError>(load(config(rooms = emptyList())).failure())
        val error = assertIs<GameDefinitionError>(load(config(characters = listOf(
            CharacterDto("c-guide", "Guide", null, dialogue = DialogueDto("d-start", emptyList()))
        ))).failure())
        assertContains(error.description, "d-start")
    }

    @Test fun `valid shared dialogue leaves and local question IDs load`() {
        val dialogue = DialogueDto("d-start", listOf(
            QuestionDto("d-start", "Start", listOf(AnswerDto("A", "d-end"), AnswerDto("B", "d-end"))),
            QuestionDto("d-end", "End", reward = "i-reward")
        ))
        load(config(items = listOf(ItemDto("Reward", "i-reward")), characters = listOf(
            CharacterDto("c-a", "A", null, dialogue = dialogue),
            CharacterDto("c-b", "B", null, dialogue = dialogue)
        ))).success()
    }

    @Test fun `valid DSL with forward references loads playable managers`() {
        val definition = game("r-start") {
            room("r-start") {
                north("r-end") {
                    state = LinkState.LOCKED
                    behavior = LinkBehavior.COMPLETE
                    triggers("i-key")
                }
                item("i-key") { name = "Key"; storable = true }
            }
            room("r-end") {}
            winIf { playerEnters = "r-end" }
        }
        val (_, _, movement, _, run, inventory, state) = load(definition).success()
        inventory.addItem(itemId("i-key")).success()
        movement.openNorth().success()
        movement.moveNorth().success()
        assertEquals(roomId("r-end"), state.currentRoom)
        assertTrue(run.win)
    }
}
