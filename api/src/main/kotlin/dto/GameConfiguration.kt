package com.saggiodev.kastle.dto

/**
 * A game definition, validated by ConfigurationManager before runtime registries are populated.
 * Room, item and character IDs must be unique within this definition. Question IDs must be
 * unique within each character's dialogue; different characters may reuse question IDs.
 * All references must resolve within this game, including link triggers, dialogue rewards,
 * the initial room and winning conditions. Reward items follow the same uniqueness rules
 * as ordinary item definitions.
 *
 * Dialogues must be acyclic, with a defined first question and defined answer destinations.
 * A terminal question has null answers and may have a reward; a nonterminal question has
 * at least one answer and no reward. Shared branches and unused valid questions are allowed.
 * Invalid definitions return accumulated GameDefinitionErrors when loaded. Constructing
 * these DTOs (including through the DSL) does not itself perform validation.
 */
class GameConfiguration(
    val rooms: List<RoomDto>,
    val player: PlayerDto,
    val initialRoomId: String,
    val items: List<ItemDto>?,
    val characters: List<CharacterDto>?,
    val metadata: MetadataDto?,
    val winningConditions: WinningConditionsDto?,
    val preface: String?,
    val epilogue: String?
)
