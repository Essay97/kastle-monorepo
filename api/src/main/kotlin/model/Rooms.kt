package com.saggiodev.kastle.model

object Rooms {
    private val rooms = mutableMapOf<RoomId, Room>()

    // Allows module tests to isolate the process-wide registry.
    internal fun clear() = rooms.clear()

    fun getById(id: RoomId): Room? {
        return rooms[id]
    }

    fun add(vararg rooms: Room) {
        rooms.forEach {
            this.rooms[it.id] = it
        }
    }
}