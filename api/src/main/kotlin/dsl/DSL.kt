package com.saggiodev.kastle.dsl

import com.saggiodev.kastle.dto.*
import kotlinx.datetime.LocalDate
import com.saggiodev.kastle.model.LinkBehavior

@DslMarker
annotation class KastleDsl

/**
 * The entry point of the DSL.
 *
 * The game begins in the room specified by [initialRoomId]. Ensure that exactly one room
 * has an ID matching this value.
 * All game details, including metadata, rooms, characters, and the player,
 * are defined through the [init] function.
 *
 */
@KastleDsl
fun game(initialRoomId: String, init: GameScope.() -> Unit): GameConfiguration {
    val scope = GameScope(initialRoomId)
    scope.init()
    return scope.build()
}

/**
 * Works as the receiver for the [game] function and provides the DSL to configure the whole game.
 */
@KastleDsl
class GameScope(private val initialRoomId: String) {
    private var metadata = MetadataDto()
    private var player = PlayerDto("Player", "Default player")
    private var rooms = mutableListOf<RoomDto>()
    private var items = mutableListOf<ItemDto>()
    private var characters = mutableListOf<CharacterDto>()
    private var winningConditions: WinningConditionsDto? = null

    /**
     * The preface is a phrase or short text that, if defined, is printed at the very beginning of the game.
     *
     * It can be used to provide context about the game and its world.
     */
    var preface: String? = null

    /**
     * The preface is a phrase or short text that, if defined, is printed at the very end of the game, after that
     * the victory has been announced.
     *
     * It can be used to provide a narrative ending to the game or to show credits.
     */
    var epilogue: String? = null



    fun metadata(init: MetadataScope.() -> Unit) {
        val scope = MetadataScope()
        scope.init()
        metadata = scope.build()
    }

    fun player(init: PlayerScope.() -> Unit) {
        val scope = PlayerScope()
        scope.init()
        player = scope.build()
    }

    /**
     * Adds a room to the game that can be configured through the [init] function.
     *
     * The [roomId] is used to reference the room from other parts of the DSL when needed.
     * It must start with <code>r-</code> and contain only lowercase letters, numbers and dashes.
     * It must match the regex <code>^r-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$</code>.
     */
    fun room(roomId: String, init: RoomScope.() -> Unit) {
        val scope = RoomScope(roomId)
        scope.init()
        val result = scope.build()
        items += result.items
        characters += result.characters
        rooms += result.room
    }

    /**
     * Sets the winning conditions for the entire game.
     */
    fun winIf(init: WinningConditionsScope.() -> Unit) {
        val scope = WinningConditionsScope()
        scope.init()
        winningConditions = scope.build()
    }

    internal fun build(): GameConfiguration = GameConfiguration(
        rooms = rooms,
        player = player,
        initialRoomId = initialRoomId,
        items = items,
        characters = characters,
        metadata = metadata,
        winningConditions = winningConditions,
        preface = preface,
        epilogue = epilogue
    )


}

/**
 * Works as the receiver for the [GameScope.metadata] function and provides the DSL to configure the metadata.
 */
@KastleDsl
class MetadataScope {
    var author: String? = null

    /**
     * The version of the game. It does not affect the game in any way, it's just documentation.
     */
    var version: String? = null
    var published: LocalDate? = null

    /**
     * The versions of the engine compatible with this version of the game.
     * It does not affect the game in any way, it's just documentation.
     */
    var kastleVersions: List<String>? = null

    /**
     * The name of the game.
     */
    var name: String? = null

    internal fun build(): MetadataDto = MetadataDto(
        author = author,
        version = version,
        published = published,
        kastleVersions = kastleVersions,
        name = name
    )
}

/**
 * Works as the receiver for the [GameScope.player] function and provides the DSL to configure the player of the game.
 */
@KastleDsl
class PlayerScope {
    /**
     * The name of the player. If not defined, defaults to "Player"
     */
    var name = "Player"
    var description: String? = null

    internal fun build(): PlayerDto = PlayerDto(
        name = name,
        description = description
    )
}

/**
 * Works as the receiver for the [GameScope.room] function and provides the DSL to configure a room.
 */
@KastleDsl
class RoomScope(private val roomId: String) {
    /**
     * The name of the room. If not defined, defaults to [roomId]
     */
    var name = roomId
    var description: String? = null

    private var north: DirectionDto? = null
    private var south: DirectionDto? = null
    private var east: DirectionDto? = null
    private var west: DirectionDto? = null
    private val items: MutableList<ItemDto> = mutableListOf()
    private val characters: MutableList<CharacterDto> = mutableListOf()

    /**
     * Links the room to another one referenced by [roomId] through the north door.
     *
     * If the [init] block is defined, you can use it to define the state and behavior of the door.
     */
    fun north(roomId: String, init: DirectionScope.() -> Unit = {}) {
        val scope = DirectionScope(roomId)
        scope.init()
        north = scope.build()
    }

    /**
     * Links the room to another one referenced by [roomId] through the south door.
     *
     * If the [init] block is defined, you can use it to define the state and behavior of the door.
     */
    fun south(roomId: String, init: DirectionScope.() -> Unit = {}) {
        val scope = DirectionScope(roomId)
        scope.init()
        south = scope.build()
    }

    /**
     * Links the room to another one referenced by [roomId] through the west door.
     *
     * If the [init] block is defined, you can use it to define the state and behavior of the door.
     */
    fun west(roomId: String, init: DirectionScope.() -> Unit = {}) {
        val scope = DirectionScope(roomId)
        scope.init()
        west = scope.build()
    }

    /**
     * Links the room to another one referenced by [roomId] through the east door.
     *
     * If the [init] block is defined, you can use it to define the state and behavior of the door.
     */
    fun east(roomId: String, init: DirectionScope.() -> Unit = {}) {
        val scope = DirectionScope(roomId)
        scope.init()
        east = scope.build()
    }

    internal fun build(): BuildResult = BuildResult(
        room = RoomDto(
            name = name,
            id = roomId,
            items = items.map { it.id },
            characters = characters.map { it.id },
            description = description,
            links = LinksDto(north, south, east, west)
        ),
        characters = characters,
        items = items
    )

    /**
     * Adds a character to the room that can be configured through the [init] function.
     *
     * The [characterId] is used to reference the character from other parts of the DSL when needed.
     * It must start with <code>c-</code> and contain only lowercase letters, numbers and dashes.
     * It must match the regex <code>^c-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$</code>.
     */
    fun character(characterId: String, init: CharacterScope.() -> Unit) {
        val scope = CharacterScope(characterId)
        scope.init()
        val result = scope.build()
        characters += result.character
        items += result.items
    }

    /**
     * Adds an item to the room that can be configured through the [init] function.
     *
     * The [itemId] is used to reference the room from other parts of the DSL when needed.
     * It must start with <code>i-</code> and contain only lowercase letters, numbers and dashes.
     * It must match the regex <code>^i-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$</code>.
     */
    fun item(itemId: String, init: ItemScope.() -> Unit) {
        val scope = ItemScope(itemId)
        scope.init()
        items += scope.build()
    }

    class BuildResult(val room: RoomDto, val items: List<ItemDto>, val characters: List<CharacterDto>)
}

/**
 * Works as the receiver for the [RoomScope.north], [RoomScope.south], [RoomScope.east] and [RoomScope.west]
 * functions and provides the DSL to configure the links between rooms.
 */
@KastleDsl
class DirectionScope(private val roomId: String) {
    /**
     * Indicates whether the door is open or closed. Defaults to open.
     */
    var state = LinkState.OPEN
    /**
     * Indicates whether the state of the door can be changed by the player. By default it cannot change
     */
    var behavior = LinkBehavior.CONSTANT

    private var triggers: List<String> = listOf()

    /**
     * Sets the items that can be used to change the state of the door, i.e., items that function as a "key".
     * The items are referenced by their IDs.
     */
    fun triggers(vararg itemIds: String) {
        triggers = itemIds.asList()
    }

    internal fun build(): DirectionDto = DirectionDto(
        roomId = roomId,
        state = DirectionStateDto(state, behavior, triggers),
    )
}

/**
 * Works as the receiver for the [RoomScope.character] function and provides the DSL to configure an NPC.
 */
@KastleDsl
class CharacterScope(private val characterId: String) {
    /**
     * The name of the character. If not defined, defaults to [characterId]
     */
    var name = characterId
    var description: String? = null
    private var matchers: List<String> = listOf()
    private var dialogue: DialogueDto? = null
    // This is needed to store the items that are created as dialogue rewards
    private var items: List<ItemDto> = mutableListOf()

    /**
     * Defines the strings that the player can use to reference this character.
     */
    fun matchers(vararg words: String) {
        matchers = words.asList()
    }

    fun dialogue(init: DialogueScope.() -> Unit) {
        val scope = DialogueScope()
        scope.init()
        val result = scope.build()
        dialogue = result.dialogue
        items = result.items
    }

    internal fun build(): BuildResult = BuildResult(
        character = CharacterDto(
            id = characterId,
            name = name,
            description = description,
            matchers = matchers,
            dialogue = dialogue
        ),
        items = items
    )

    class BuildResult(val character: CharacterDto, val items: List<ItemDto>)
}

/**
 * Works as the receiver for the [RoomScope.item] and [QuestionScope.reward] functions and provides the DSL
 * to configure an item.
 */
@KastleDsl
class ItemScope(private val itemId: String) {
    /**
     * The name of the item. If not defined, defaults to [itemId]
     */
    var name = itemId
    var description: String? = null

    /**
     * Indicates whether the item can be stored in the inventory by the player.
     */
    var storable: Boolean = false
    private var matchers: List<String> = listOf()

    /**
     * Defines the strings that the player can use to reference this item.
     */
    fun matchers(vararg words: String) {
        matchers = words.asList()
    }

    internal fun build(): ItemDto = ItemDto(
        name = name,
        id = itemId,
        description = description,
        matchers = matchers,
        /* At the moment the use is a string, even if it's not really needed.
         This workaround makes the DSL more legible and makes more sense */
        use = if (storable) "" else null
    )
}

/**
 * Works as the receiver for the [CharacterScope.dialogue] function and provides the DSL to configure a dialogue.
 */
@KastleDsl
class DialogueScope {
    private var questions: MutableList<QuestionDto> = mutableListOf()
    private var firstQuestionId = "d-default-question"
    // This is needed to store the items that are created as dialogue rewards
    private var items: MutableList<ItemDto> = mutableListOf()

    /**
     * Defines the first question of the dialogue, told by the NPC.
     *
     * The [questionId] is used to reference the room from other parts of the DSL when needed.
     * It must start with <code>d-</code> and contain only lowercase letters, numbers and dashes.
     * It must match the regex <code>^d-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$</code>.
     * This function is mandatory for every dialogue definition.
     */
    fun firstQuestion(questionId: String, init: QuestionScope.() -> Unit) {
        val scope =  QuestionScope(questionId)
        scope.init()
        firstQuestionId = questionId
        val result = scope.build()
        questions += result.question
        if (result.item != null) {
            items += result.item
        }
    }
    /**
     * Defines every other question except the first of the dialogue (see [firstQuestion]).
     *
     * The [questionId] is used to reference the room from other parts of the DSL when needed.
     * It must start with <code>d-</code> and contain only lowercase letters, numbers and dashes.
     * It must match the regex <code>^d-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$</code>.
     * This function is mandatory for every dialogue definition.
     */
    fun question(questionId: String, init: QuestionScope.() -> Unit) {
        val scope =  QuestionScope(questionId)
        scope.init()
        val result = scope.build()
        questions += result.question
        if (result.item != null) {
            items += result.item
        }

    }

    internal fun build(): BuildResult = BuildResult(
        dialogue = DialogueDto(
            firstQuestion = firstQuestionId,
            questions = questions
        ),
        items = items
    )

    class BuildResult(val dialogue: DialogueDto, val items: List<ItemDto>)
}

/**
 * Works as the receiver for the [DialogueScope.question] function and provides the DSL to configure a question inside
 * a dialogue.
 */
@KastleDsl
class QuestionScope(private val questionId: String) {
    /**
     * The actual question told by the character. Defaults to "Default question".
     */
    var text = "Default question"
    private var reward: ItemDto? = null //Item id of the object that will be dropped

    private var answers: MutableList<AnswerDto> = mutableListOf()

    /**
     * Defines one possible answer for the question. To provide multiple options, invoke this function multiple times.
     *
     * - If at least one answer is defined, no [reward] can be defined
     * - If no answer is defined, the question is considered a terminal node of the dialogue
     */
    fun answer(init: AnswerScope.() -> Unit) {
        val scope = AnswerScope()
        scope.init()
        answers += scope.build()
    }

    /**
     * Defines the reward that the character awards the player at the end of the dialogue.
     *
     * If at least one answer is defined, this reward has no effect.
     * The reward is never mandatory.
     */
    fun reward(itemId: String, init: ItemScope.() -> Unit) {
        val scope = ItemScope(itemId)
        scope.init()
        reward = scope.build()
    }

    internal fun build(): BuildResult = BuildResult(
        question = QuestionDto(
            id = questionId,
            question = text,
            answers = answers,
            reward = reward?.id
        ),
        item = reward
    )

    class BuildResult(val question: QuestionDto, val item: ItemDto?)
}

/**
 * Works as the receiver for the [QuestionScope.answer] function and provides the DSL to configure an answer
 * to a question.
 */
@KastleDsl
class AnswerScope {
    /**
     * The actual answer told by the player if this answer option is chosen. Defaults to "Default answer".
     */
    var text = "Default answer"

    /**
     * The next question in the dialogue if this answer option is chosen, referenced by its ID.
     * Defaults to "d-default-question".
     */
    var nextQuestion = "d-default-question"

    internal fun build(): AnswerDto = AnswerDto(
        text = text,
        nextQuestion = nextQuestion
    )
}

/**
 * Works as the receiver for the [GameScope.winIf] function and provides the DSL to configure the conditions that
 * the player must meet in order to win the game.
 *
 * The conditions that can be met are:
 * - the player owns a specific item
 * - the player enters a specific room
 *
 * If both conditions are defined, fulfilling either one is sufficient to win the game.
 */
@KastleDsl
class WinningConditionsScope {
    /**
     * Indicates the item that the player must own in order to win the game, referenced by its ID.
     */
    var playerOwns: String? = null
    /**
     * Indicates the room that the player must enter in order to win the game, referenced by its ID.
     */
    var playerEnters: String? = null

    internal fun build(): WinningConditionsDto = WinningConditionsDto(
        playerOwns = playerOwns,
        playerEnters = playerEnters
    )
}

