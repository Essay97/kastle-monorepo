package com.saggiodev.kastle

import com.saggiodev.kastle.dsl.game
import com.saggiodev.kastle.dto.*
import com.saggiodev.kastle.error.*
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.model.nextaction.ExecuteDialogue
import org.junit.jupiter.api.Test
import kotlin.test.*

class DialogueTest : RegistryTest() {
    private fun dialogueConfig(reward: Boolean = false, terminalFirst: Boolean = false) = game("r-start") {
        room("r-start") {
            character("c-guide") {
                matchers("guide")
                dialogue {
                    if (terminalFirst) {
                        firstQuestion("d-first") {
                            text = "Goodbye"
                            if (reward) reward("i-prize") { storable = true; matchers("prize") }
                        }
                    } else {
                        firstQuestion("d-first") {
                            text = "Where?"
                            answer { text = "Continue"; nextQuestion = "d-last" }
                        }
                        question("d-last") {
                            text = "Goodbye"
                            if (reward) reward("i-prize") { storable = true; matchers("prize") }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `character without dialogue cannot talk`() {
        val game = testGame()
        assertIs<GameRuntimeError.CharacterNotTalker>(game.commands.createTalkCommand("guard").execute().failure())
        assertIs<GameRuntimeError.CannotFindTalker>(game.commands.createTalkCommand("missing").execute().failure())
    }

    @Test
    fun `talk starts dialogue and immediately prevents talking again`() {
        val game = TestGame(dialogueConfig())
        val action = assertIs<ExecuteDialogue>(game.commands.createTalkCommand("guide").execute().success())
        assertEquals(CharacterId("c-guide").success(), action.dialogue.talker)
        val first = action.dialogue.first().failure()
        assertEquals("Where?", first.question)
        assertEquals(listOf("Continue"), first.answers)
        assertIs<GameRuntimeError.CharacterAlreadyTalked>(game.commands.createTalkCommand("guide").execute().failure())
    }

    @Test
    fun `DTO null answers map to terminal leaf without reward`() {
        val leaf = assertIs<Question.Leaf>(QuestionDto("d-end", "Goodbye").toQuestion(emptyList()).success())
        val dialogue = Dialogue(leaf, CharacterId("c-guide").success())
        assertSame(leaf, dialogue.first().success())
        assertNull(leaf.reward)
        assertFalse(dialogue.hasNext())
    }

    @Test
    fun `DTO navigation selects answers reaches reward leaf and first restarts dialogue`() {
        val dto = DialogueDto("d-first", listOf(
            QuestionDto("d-first", "Choose", listOf(AnswerDto("Nothing", "d-empty"), AnswerDto("Prize", "d-prize"))),
            QuestionDto("d-empty", "Bye"),
            QuestionDto("d-prize", "Take this", reward = "i-prize")
        ))
        val dialogue = Dialogue(dto.toQuestion().success(), CharacterId("c-guide").success())
        assertEquals(listOf("Nothing", "Prize"), dialogue.first().failure().answers)
        assertTrue(dialogue.hasNext())
        assertEquals(itemId("i-prize"), dialogue.next(1).success().reward)
        assertFalse(dialogue.hasNext())
        dialogue.first()
        assertTrue(dialogue.hasNext())
        assertNull(dialogue.next(0).success().reward)
        assertFalse(dialogue.hasNext())
        // The API exposes the reward ID; NextActionHandler in engine adds it to the room.
    }

    @Test
    fun `DSL terminal emits null answers and maps to leaf`() {
        val dto = dialogueConfig().characters!!.single().dialogue!!
        assertEquals(listOf(AnswerDto("Continue", "d-last")), dto.questions.first().answers)
        val terminal = dto.questions.last()
        assertNull(terminal.answers)
        val leaf = assertIs<Question.Leaf>(terminal.toQuestion(dto.questions).success())
        assertEquals("Goodbye", leaf.text)
        assertNull(leaf.reward)
    }

    @Test
    fun `DSL terminal reward is registered without being placed in inventory`() {
        val config = dialogueConfig(reward = true)
        val terminal = config.characters!!.single().dialogue!!.questions.last()
        assertNull(terminal.answers)
        assertEquals("i-prize", terminal.reward)
        assertEquals("i-prize", config.items!!.single().id)
        val game = TestGame(config)
        assertIs<StorableItem>(Items.getById(itemId("i-prize")))
        assertTrue(config.rooms.single().items.orEmpty().isEmpty())
        assertTrue(game.start.items.isEmpty())
        assertTrue(game.state.inventory.isEmpty())
    }

    @Test
    fun `DSL terminal without reward ends dialogue`() {
        assertTerminalDialogue(reward = false, terminalFirst = false)
    }

    @Test
    fun `DSL terminal with reward yields reward leaf`() {
        assertTerminalDialogue(reward = true, terminalFirst = false)
    }

    @Test
    fun `DSL terminal first question without reward ends dialogue`() {
        assertTerminalDialogue(reward = false, terminalFirst = true)
    }

    @Test
    fun `DSL terminal first question with reward yields reward leaf`() {
        assertTerminalDialogue(reward = true, terminalFirst = true)
    }

    private fun assertTerminalDialogue(reward: Boolean, terminalFirst: Boolean) {
        val game = TestGame(dialogueConfig(reward, terminalFirst))
        val dialogue = assertIs<ExecuteDialogue>(game.commands.createTalkCommand("guide").execute().success()).dialogue
        val first = dialogue.first()
        val leaf = if (terminalFirst) first.success() else {
            assertEquals(listOf("Continue"), first.failure().answers)
            assertTrue(dialogue.hasNext())
            dialogue.next(0).success()
        }
        assertEquals("Goodbye", leaf.text)
        assertEquals(if (reward) itemId("i-prize") else null, leaf.reward)
        assertFalse(dialogue.hasNext())
        assertIs<GameRuntimeError.CharacterAlreadyTalked>(game.commands.createTalkCommand("guide").execute().failure())
    }

    @Test
    fun `rewarded first question places one collectible reward only after completion`() {
        assertRewardPlacement(terminalFirst = true)
    }

    @Test
    fun `rewarded subsequent question places one collectible reward only after completion`() {
        assertRewardPlacement(terminalFirst = false)
    }

    private fun assertRewardPlacement(terminalFirst: Boolean) {
        val game = TestGame(dialogueConfig(reward = true, terminalFirst = terminalFirst))
        assertTrue(game.start.items.isEmpty())
        assertIs<GameRuntimeError.CannotFindStorable>(game.commands.createGrabCommand("prize").execute().failure())
        val dialogue = assertIs<ExecuteDialogue>(game.commands.createTalkCommand("guide").execute().success()).dialogue
        val first = dialogue.first()
        val leaf = if (terminalFirst) first.success() else {
            first.failure()
            assertTrue(game.start.items.isEmpty())
            dialogue.next(0).success()
        }
        // Perform the room placement used by the engine's NextActionHandler at a terminal leaf.
        Rooms.getById(game.state.currentRoom)!!.addItem(assertNotNull(leaf.reward)).success()
        assertFalse(dialogue.hasNext())
        assertEquals(listOf(itemId("i-prize")), game.start.items.toList())
        assertTrue(game.state.inventory.isEmpty())
        game.commands.createGrabCommand("prize").execute().success()
        assertEquals(listOf(itemId("i-prize")), game.state.inventory.toList())
        assertTrue(game.start.items.isEmpty())
        assertIs<GameRuntimeError.CharacterAlreadyTalked>(game.commands.createTalkCommand("guide").execute().failure())
        assertTrue(game.start.items.isEmpty())
        assertIs<GameRuntimeError.CannotFindStorable>(game.commands.createGrabCommand("prize").execute().failure())
    }

    @Test
    fun `only selected branch reward is placed while ordinary items are present initially`() {
        val config = game("r-start") {
            room("r-start") {
                item("i-before") { storable = true }
                character("c-guide") {
                    matchers("guide")
                    dialogue {
                        firstQuestion("d-choice") {
                            answer { text = "Gold"; nextQuestion = "d-gold" }
                            answer { text = "Silver"; nextQuestion = "d-silver" }
                        }
                        question("d-gold") { reward("i-gold") { storable = true } }
                        question("d-silver") { reward("i-silver") { storable = true } }
                    }
                }
                item("i-after") { storable = true }
            }
        }
        assertEquals(setOf("i-before", "i-after", "i-gold", "i-silver"), config.items!!.map { it.id }.toSet())
        assertEquals(listOf("i-before", "i-after"), config.rooms.single().items)
        val game = TestGame(config)
        assertNotNull(Items.getById(itemId("i-gold")))
        assertNotNull(Items.getById(itemId("i-silver")))
        assertEquals(listOf(itemId("i-before"), itemId("i-after")), game.start.items.toList())
        val dialogue = assertIs<ExecuteDialogue>(game.commands.createTalkCommand("guide").execute().success()).dialogue
        dialogue.first().failure()
        val leaf = dialogue.next(1).success()
        assertEquals(itemId("i-silver"), leaf.reward)
        Rooms.getById(game.state.currentRoom)!!.addItem(assertNotNull(leaf.reward)).success()
        assertEquals(listOf(itemId("i-before"), itemId("i-after"), itemId("i-silver")), game.start.items.toList())
        assertTrue(game.state.inventory.isEmpty())
    }

    @Test
    fun `DSL question with answers and reward still fails serialization`() {
        val config = game("r-start") {
            room("r-start") {
                character("c-guide") {
                    dialogue {
                        firstQuestion("d-first") {
                            answer { text = "Continue"; nextQuestion = "d-last" }
                            reward("i-prize") { storable = true }
                        }
                        question("d-last") { text = "Goodbye" }
                    }
                }
            }
        }
        val error = assertIs<SerializationError>(config.characters!!.single().toCharacter().failure())
        assertContains(error.description, "Question should have answers or reward, not both")
    }
}
