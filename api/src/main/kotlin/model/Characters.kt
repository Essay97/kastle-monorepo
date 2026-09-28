package com.saggiodev.kastle.model

object Characters {
    private val characters = mutableMapOf<CharacterId, Character>()

    // Allows module tests to isolate the process-wide registry.
    internal fun clear() = characters.clear()

    fun getById(id: CharacterId): Character? = characters[id]

    fun add(vararg characters: Character) {
        characters.forEach {
            this.characters[it.id] = it
        }
    }
}