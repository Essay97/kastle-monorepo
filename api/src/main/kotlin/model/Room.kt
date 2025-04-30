package com.saggiodev.kastle.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.mapOrAccumulate
import com.saggiodev.kastle.model.capabilities.Inspectable
import com.saggiodev.kastle.error.GameDefinitionError
import com.saggiodev.kastle.error.KastleError

class Room private constructor(
    val id: RoomId,
    val name: String,
    override val description: String,
    val characters: List<CharacterId> = emptyList(),
    items: List<ItemId> = emptyList(),
) : Inspectable {
    override val matchers: List<String> = listOf("room")

    private val _items: MutableCollection<ItemId> = items.toMutableList()
    val items: Collection<ItemId> = _items

    fun addItem(itemId: ItemId): Either<KastleError, Unit> = either {
        ensureNotNull(Items.getById(itemId)) { GameDefinitionError.IncoherentItemInRoom(id.value, itemId) }
        _items.add(itemId)
    }

    fun removeItem(itemId: ItemId): Boolean = _items.remove(itemId)


    companion object {
        operator fun invoke(
            id: RoomId,
            name: String,
            description: String,
            characters: List<CharacterId> = emptyList(),
            items: List<ItemId> = emptyList()
        ): Either<KastleError, Room> = either {
            mapOrAccumulate(items, KastleError::join) {
                ensureNotNull(Items.getById(it)) { GameDefinitionError.IncoherentItemInRoom(id.value, it) }
            }
            mapOrAccumulate(characters, KastleError::join) {
                ensureNotNull(Characters.getById(it)) { GameDefinitionError.IncoherentCharacterInRoom(id.value, it) }
            }
            Room(id, name, description, characters, items)
        }
    }
}

