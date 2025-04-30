package com.saggiodev.kastle.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.saggiodev.kastle.error.ValidationError


abstract class Id {
    abstract val value: String
    override fun equals(other: Any?): Boolean = other is Id && value == other.value

    override fun hashCode(): Int {
        return value.hashCode()
    }
}


class ItemId private constructor(override val value: String) : Id() {
    companion object {
        operator fun invoke(value: String): Either<ValidationError, ItemId> = either {
            ensure(Regex("^i-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$").matches(value)) {
                ValidationError.InvalidItemId(value)
            }
            ItemId(value)
        }
    }
}

class RoomId private constructor(override val value: String) : Id() {
    companion object {
        operator fun invoke(value: String): Either<ValidationError, RoomId> = either {
            ensure(Regex("^r-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$").matches(value)) {
                ValidationError.InvalidRoomId(value)
            }
            RoomId(value)
        }
    }
}

class CharacterId private constructor(override val value: String) : Id() {
    companion object {
        operator fun invoke(value: String): Either<ValidationError, CharacterId> = either {
            ensure(Regex("^c-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$").matches(value)) {
                ValidationError.InvalidCharacterId(value)
            }
            CharacterId(value)
        }
    }
}

class DialogueId private constructor(override val value: String) : Id() {
    companion object {
        operator fun invoke(value: String): Either<ValidationError, DialogueId> = either {
            ensure(Regex("^d-(?!-)(?:[a-z0-9]+-)*[a-z0-9]+$").matches(value)) {
                ValidationError.InvalidDialogueId(value)
            }
            DialogueId(value)
        }
    }
}