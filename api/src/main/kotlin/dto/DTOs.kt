package com.saggiodev.kastle.dto

import kotlinx.datetime.LocalDate
import com.saggiodev.kastle.model.LinkBehavior


data class MetadataDto(
    val author: String? = null, val version: String? = null, val published: LocalDate? = null, // Format YYYY-MM-DD
    val kastleVersions: List<String>? = null, val name: String? = null
)

data class SpecDto(
    val preface: String?, // A string that works as a preface for the whole story, it gets printed at the very beginning of the game
    val rooms: List<RoomDto>,
    val items: List<ItemDto>? = null,
    val characters: List<CharacterDto>? = null,
    val player: PlayerDto,
    val initialRoomId: String,
    val winningConditions: WinningConditionsDto? = null
)

data class GameConfigDto(val metadata: MetadataDto? = null, val spec: SpecDto)

data class RoomDto(
    val name: String,
    val id: String,
    val links: LinksDto? = null,
    val items: List<String>? = null,
    val characters: List<String>? = null,
    val description: String? = null
)

data class LinksDto(
    val north: DirectionDto? = null,
    val south: DirectionDto? = null,
    val east: DirectionDto? = null,
    val west: DirectionDto? = null
)

data class DirectionDto(
    val roomId: String, val state: DirectionStateDto
)

data class DirectionStateDto(
    val value: LinkState, val behavior: LinkBehavior, val triggers: List<String>? = null
)

enum class LinkState { OPEN, LOCKED }

data class ItemDto(
    val name: String,
    val id: String,
    val description: String? = null,
    val matchers: List<String>? = null,
    val use: String? = null
)

data class CharacterDto(
    val id: String,
    val name: String,
    val description: String?,
    val matchers: List<String>? = null,
    val dialogue: DialogueDto? = null
)

data class DialogueDto(
    val firstQuestion: String, val questions: List<QuestionDto>
)

data class QuestionDto(
    val id: String, val question: String, val answers: List<AnswerDto>? = null, val reward: String? = null
)

data class AnswerDto(
    val text: String, val nextQuestion: String
)

data class PlayerDto(
    val name: String, val description: String? = null, val fighting: FightingStatsDto? = null
)

data class FightingStatsDto(
    val hp: Int, val attack: Int
)

data class WinningConditionsDto(
    val playerOwns: String? = null, val playerEnters: String? = null
)
