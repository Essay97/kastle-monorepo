package com.saggiodev.kastle.dto

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.mapOrAccumulate
import com.saggiodev.kastle.error.KastleError
import com.saggiodev.kastle.error.SerializationError
import com.saggiodev.kastle.model.*

internal fun MetadataDto.toGameMetadata(): GameMetadata =
    GameMetadata(author, version, published, kastleVersions, name)

internal fun DirectionDto.toLink(): Either<KastleError, Link> = either {
    val triggers = mapOrAccumulate(state.triggers ?: emptyList(), KastleError::join) {
        ItemId(it).bind()
    }
    Link(
        destination = RoomId(roomId).bind(),
        open = state.value == LinkState.OPEN,
        behavior = state.behavior,
        triggeredBy = triggers
    )
}


internal fun RoomDto.toRoom(): Either<KastleError, Room> = either {
    val itemIds = mapOrAccumulate(items ?: emptyList(), KastleError::join) {
        ItemId(it).bind()
    }
    val characterIds = mapOrAccumulate(characters ?: emptyList(), KastleError::join) {
        CharacterId(it).bind()
    }

    Room(
        id = RoomId(id).bind(),
        name = name,
        description = description ?: "You cannot see that much of the room.",
        items = itemIds,
        characters = characterIds
    ).bind()
}


internal fun ItemDto.toItem(): Either<KastleError, Item> = either {
    val id = ItemId(this@toItem.id).bind()
    val description = description ?: "It's not so clear how this looks..."
    val matchers = matchers ?: emptyList()
    if (use != null) {
        StorableItem(
            id = id,
            name = name,
            description = description,
            matchers = matchers,
            use = use
        )
    } else {
        RegularItem(
            id = id,
            name = name,
            description = description,
            matchers = matchers
        )
    }

}

internal fun CharacterDto.toCharacter(): Either<KastleError, Character> = either {
    Character(
        id = CharacterId(id).bind(),
        name = name,
        description = description ?: "This is nearly unrecognizable",
        matchers = matchers ?: emptyList(),
        dialogue = dialogue?.toQuestion()?.bind()
    )
}

internal fun DialogueDto.toQuestion(): Either<KastleError, Question> = either {
    val firstQuestionDto = ensureNotNull(questions.find { it.id == firstQuestion }) {
        SerializationError("firstQuestion references a non existent dialogue ID (${firstQuestion})")
    }
    firstQuestionDto.toQuestion(questions).bind()
}

internal fun QuestionDto.toQuestion(questionDtoList: List<QuestionDto>): Either<KastleError, Question> = either {
    ensure(!(answers != null && reward != null)) {
        SerializationError("Question should have answers or reward, not both ('${id}')")
    }
    if (answers == null) {
        Question.Leaf(question, reward?.let { ItemId(it).bind() })
    } else {
        val domainAnswers = answers.map { it.toAnswer(questionDtoList) }.bindAll()
        Question.Node(question, domainAnswers)
    }
}

internal fun AnswerDto.toAnswer(questionDtoList: List<QuestionDto>): Either<KastleError, Answer> = either {
    val nextQuestionDto = ensureNotNull(questionDtoList.find { it.id == nextQuestion }) {
        SerializationError("Trying to reference non existent dialogue id ($nextQuestion)")
    }
    val nextDomainQuestion = nextQuestionDto.toQuestion(questionDtoList).bind()
    Answer(text, nextDomainQuestion)
}

internal fun WinningConditionsDto.toWinningConditions(): Either<KastleError, WinningConditions> = either {
    val roomId = playerEnters?.let { RoomId(it).bind() }
    val itemId = playerOwns?.let { ItemId(it).bind() }
    WinningConditions(itemId, roomId).bind()
}

internal fun PlayerDto.toPlayer(): Player =
    Player(name, description ?: "At the end of the day, even you don't know yourself very well...")
