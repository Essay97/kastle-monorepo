package com.saggiodev.kastle.dto

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