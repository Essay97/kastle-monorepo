package com.saggiodev.kastle

import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.error.ValidationError
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*

class IdValidationTest {
    @ParameterizedTest
    @ValueSource(strings = ["room", "room-1", "1", "a-b-2"])
    fun `all four ID types accept lowercase segments and numbers`(suffix: String) {
        assertEquals("r-$suffix", RoomId("r-$suffix").success().value)
        assertEquals("i-$suffix", ItemId("i-$suffix").success().value)
        assertEquals("c-$suffix", CharacterId("c-$suffix").success().value)
        assertEquals("d-$suffix", DialogueId("d-$suffix").success().value)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Room", "-room", "room_", "", "room-", "room--1", "room name"])
    fun `all four ID types reject malformed suffixes`(suffix: String) {
        assertIs<ValidationError.InvalidRoomId>(RoomId("r-$suffix").failure())
        assertIs<ValidationError.InvalidItemId>(ItemId("i-$suffix").failure())
        assertIs<ValidationError.InvalidCharacterId>(CharacterId("c-$suffix").failure())
        assertIs<ValidationError.InvalidDialogueId>(DialogueId("d-$suffix").failure())
    }

    @ParameterizedTest
    @ValueSource(strings = ["room", "x-room", "R-room"])
    fun `all four ID types require their own prefix`(value: String) {
        assertIs<ValidationError.InvalidRoomId>(RoomId(value).failure())
        assertIs<ValidationError.InvalidItemId>(ItemId(value).failure())
        assertIs<ValidationError.InvalidCharacterId>(CharacterId(value).failure())
        assertIs<ValidationError.InvalidDialogueId>(DialogueId(value).failure())
    }
}
