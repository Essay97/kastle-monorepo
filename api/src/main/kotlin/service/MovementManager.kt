package com.saggiodev.kastle.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.saggiodev.kastle.model.*
import com.saggiodev.kastle.error.GameDefinitionError
import com.saggiodev.kastle.error.GameRuntimeError
import kotlin.reflect.KProperty1

class MovementManager private constructor(
    private val state: GameState,
    private val dungeon: Map<RoomId, DungeonNode>,
) {

    companion object {
        operator fun invoke(
            state: GameState, dungeon: Map<RoomId, DungeonNode>
        ): Either<GameDefinitionError, MovementManager> = either {
            ensure(dungeon.containsKey(state.currentRoom)) { GameDefinitionError.UnknownInitialRoom(state.currentRoom.value) }
            dungeon.entries.forEach { room ->
                listOf(DungeonNode::north, DungeonNode::south, DungeonNode::west, DungeonNode::east).forEach {
                    val roomId = it.get(room.value)?.destination
                    ensure(roomId == null || dungeon.containsKey(roomId)) {
                        GameDefinitionError.IncoherentDungeonMap(room.key.value, roomId?.value ?: "", it.name)
                    }
                }
            }
            MovementManager(state, dungeon)
        }
    }

    fun moveNorth(): Either<GameRuntimeError, Unit> = moveTo(DungeonNode::north)
    fun moveSouth(): Either<GameRuntimeError, Unit> = moveTo(DungeonNode::south)
    fun moveEast(): Either<GameRuntimeError, Unit> = moveTo(DungeonNode::east)
    fun moveWest(): Either<GameRuntimeError, Unit> = moveTo(DungeonNode::west)

    private fun moveTo(direction: KProperty1<DungeonNode, Link?>): Either<GameRuntimeError, Unit> = either {
        val nextRoomLink = direction.get(dungeon[state.currentRoom]!!)
        ensureNotNull(nextRoomLink) { mapError(direction) }
        ensure(nextRoomLink.open) {
            GameRuntimeError.TraversingClosedLink(
                source = Rooms.getById(state.currentRoom)?.name ?: "UNKNOWN ROOM",
                destination = Rooms.getById(nextRoomLink.destination)?.name ?: "UNKNOWN ROOM"
            )
        }

        state.currentRoom = nextRoomLink.destination
    }

    private fun mapError(direction: KProperty1<DungeonNode, Link?>) = when (direction.name) {
        "north" -> GameRuntimeError.MissingRoomToNorth
        "south" -> GameRuntimeError.MissingRoomToSouth
        "east" -> GameRuntimeError.MissingRoomToEast
        "west" -> GameRuntimeError.MissingRoomToWest
        else -> GameRuntimeError("Trying to map unknown direction")
    }

    fun openNorth(): Either<GameRuntimeError, Room> = open(DungeonNode::north)
    fun openSouth(): Either<GameRuntimeError, Room> = open(DungeonNode::south)
    fun openEast(): Either<GameRuntimeError, Room> = open(DungeonNode::east)
    fun openWest(): Either<GameRuntimeError, Room> = open(DungeonNode::west)

    private fun open(direction: KProperty1<DungeonNode, Link?>): Either<GameRuntimeError, Room> = either {
        val link = direction.get(dungeon[state.currentRoom]!!)
        val currentRoom = Rooms.getById(state.currentRoom)!!
        ensureNotNull(link) { mapError(direction) }
        val destinationRoom = Rooms.getById(link.destination)!!
        ensure(link.behavior.canOpen) { GameRuntimeError.OpeningUnopenableLink(currentRoom.name, destinationRoom.name) }
        ensure(link.triggeredBy.any { it in state.inventory }) {
            GameRuntimeError.NoOpenTriggerOwned(currentRoom.name, destinationRoom.name)
        }
        link.open = true
        currentRoom
    }

    fun closeNorth(): Either<GameRuntimeError, Room> = close(DungeonNode::north)
    fun closeSouth(): Either<GameRuntimeError, Room> = close(DungeonNode::south)
    fun closeEast(): Either<GameRuntimeError, Room> = close(DungeonNode::east)
    fun closeWest(): Either<GameRuntimeError, Room> = close(DungeonNode::west)

    private fun close(direction: KProperty1<DungeonNode, Link?>): Either<GameRuntimeError, Room> = either {
        val link = direction.get(dungeon[state.currentRoom]!!)
        val currentRoom = Rooms.getById(state.currentRoom)!!
        ensureNotNull(link) { mapError(direction) }
        val destinationRoom = Rooms.getById(link.destination)!!
        ensure(link.behavior.canClose) { GameRuntimeError.ClosingUnclosableLink(currentRoom.name, destinationRoom.name) }
        ensure(link.triggeredBy.any { it in state.inventory }) {
            GameRuntimeError.NoCloseTriggerOwned(currentRoom.name, destinationRoom.name)
        }
        link.open = false
        currentRoom
    }
}
