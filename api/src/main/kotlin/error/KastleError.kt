package com.saggiodev.kastle.error

import com.saggiodev.kastle.model.CharacterId
import com.saggiodev.kastle.model.ItemId

sealed class KastleError(open val description: String) {
    abstract fun join(second: KastleError): KastleError
}

data object NegativePaddingError : KastleError("Padding is negative: game name is longer than table column width") {
    override fun join(second: KastleError): KastleError = NegativePaddingError
}

/*
 * Configuration Errors
 */
abstract class ConfigError(description: String) : KastleError(description)

open class UserHomeError(description: String) : ConfigError(description) {
    data object NoPermission : UserHomeError("Insufficient permission to access home directory")
    data object NoHomeDirectory : UserHomeError("Fetching home directory returned null")
    data object EmptyProperty : UserHomeError("Fetching home directory returned empty string")

    override fun join(second: KastleError): KastleError =
        UserHomeError("$description\n${second.description}")
}

open class KastleDirectoryError(description: String) : ConfigError(description) {
    data object NoPermission : KastleDirectoryError("Insufficient permission to access .kastle directory")
    data object CreationError : KastleDirectoryError("Could not create .kastle folder in user's home directory")

    override fun join(second: KastleError): KastleError =
        KastleDirectoryError("$description\n${second.description}")
}

open class GamesDirectoryError(description: String) : ConfigError(description) {
    data object NoPermission : GamesDirectoryError("Insufficient permission to access games directory")

    override fun join(second: KastleError): KastleError =
        KastleDirectoryError("$description\n${second.description}")
}

open class DbFileError(description: String) : ConfigError(description) {
    data object NoPermission :
        DbFileError("Insufficient permission to access games.db file in \$HOME/.kastle directory")

    data object IOError : DbFileError("Filesystem raised an IO error")

    override fun join(second: KastleError): KastleError =
        DbFileError("$description\n${second.description}")
}

open class GameFileError(description: String) : ConfigError(description) {
    data object NonExistentGame : GameFileError("Game does not exist")
    data object GameAlreadyExists : GameFileError("Game already exists")

    override fun join(second: KastleError): KastleError =
        GameFileError("$description\n${second.description}")
}



/*
 * Serialization Errors
 */
class SerializationError(description: String) : KastleError("Serialization error\n$description") {
    override fun join(second: KastleError): KastleError =
        SerializationError("$description\n${second.description}")
}

/*
 * Game Definition Errors
 */
open class GameDefinitionError(description: String) : KastleError("Game definition error\n$description") {
    class UnknownInitialRoom(roomId: String) :
        GameDefinitionError("Initial room ID $roomId is not contained in dungeon")

    class IncoherentDungeonMap(source: String, destination: String, direction: String) :
        GameDefinitionError(
            "Dungeon is incoherent: $direction direction of $source points " +
                    "to a room that is not a key of the dungeon ($destination)"
        )

    class IncoherentItemInRoom(room: String, item: ItemId) :
        GameDefinitionError("Room $room is incoherent: trying to insert a non defined item (${item.value})")
    class IncoherentCharacterInRoom(room: String, item: CharacterId) :
        GameDefinitionError("Room $room is incoherent: trying to insert a non defined character (${item.value})")

    abstract class IncoherentWinningCondition(condition: String, id: String) :
        GameDefinitionError("Incoherent winning condition on $condition: trying to reference non existent entity with id $id")

    class IncoherentItemWin(id: String) : IncoherentWinningCondition("playerOwns", id)
    class IncoherentRoomWin(id: String) : IncoherentWinningCondition("playerEnters", id)


    override fun join(second: KastleError): KastleError =
        GameDefinitionError("$description\n${second.description}")
}

/*
 * Player Errors: the player is using the commands in the wrong way
 */
open class PlayerError(description: String) :
    KastleError("User error: the player is using the commands in the wrong way\n$description") {
    class UnknownGameCommand(input: String) : PlayerError("Unknown game command ($input)")
    class UnknownGoDirection(input: String) : PlayerError("Player invoked 'go' with an unknown direction ($input)")
    data object NoGoDirection : PlayerError("Player invoked the 'go' command without a direction")
    data object NoOpenDirection : PlayerError("Player invoked the 'open' command without a direction")
    data object NoCloseDirection : PlayerError("Player invoked the 'close' command without a direction")
    class CommandRequiresNoArguments(command: String, argument: String) : PlayerError(
        "Command $command " +
                "requires no arguments, player passed $argument"
    )

    open class NoEntitySpecified(command: String, verb: String) :
        PlayerError("Player invoked the '$command' command without specifying what to $verb")
    data object NoInspectableSpecified : NoEntitySpecified("inspect", "inspect")
    data object NoStorableSpecified : NoEntitySpecified("grab", "grab")
    data object NoDroppableSpecified : NoEntitySpecified("drop", "drop")
    data object NoTalkerSpecified : NoEntitySpecified("talk", "talk to")

    override fun join(second: KastleError): KastleError =
        PlayerError("$description\n${second.description}")
}

/*
 * Game Runtime Errors
 */
open class GameRuntimeError(description: String) : KastleError(description) {
    abstract class MissingRoomToDirection(direction: String) : GameRuntimeError("There's no room at $direction")
    data object MissingRoomToNorth : MissingRoomToDirection("north")
    data object MissingRoomToSouth : MissingRoomToDirection("south")
    data object MissingRoomToEast : MissingRoomToDirection("east")
    data object MissingRoomToWest : MissingRoomToDirection("west")

    class TraversingClosedLink(source: String, destination: String) :
        GameRuntimeError("The link between ${source.uppercase()} and ${destination.uppercase()} is closed")

    class CannotFindInspectable(matcher: String) :
        GameRuntimeError("There's no inspectable object that matches $matcher")

    class ItemNotInRoom(item: String) : GameRuntimeError("There's no $item item in the current room")
    class ItemNotInInventory(item: String) : GameRuntimeError("There's no $item item in the inventory")
    class CannotFindStorable(matcher: String) : GameRuntimeError("$matcher is not an item that can be grabbed")
    class CannotFindTalker(matcher: String) : GameRuntimeError("$matcher is not a character that you can talk to")

    class OpeningUnopenableLink(source: String, destination: String) :
        GameRuntimeError("The link between $source and $destination cannot be opened")
    class ClosingUnclosableLink(source: String, destination: String) :
        GameRuntimeError("The link between $source and $destination cannot be closed")

    class NoOpenTriggerOwned(source: String, destination: String) :
        GameRuntimeError("There are no items in the inventory that can open the link " +
                "between $source and $destination")
    class NoCloseTriggerOwned(source: String, destination: String) :
        GameRuntimeError("There are no items in the inventory that can close the link " +
                "between $source and $destination")

    class CharacterNotTalker(name: String) : GameRuntimeError("The character $name cannot talk")
    class CharacterAlreadyTalked(name: String) : GameRuntimeError("The character $name already talked, cannot say anything more")

    override fun join(second: KastleError): KastleError =
        GameRuntimeError("$description\n${second.description}")
}

/*
 * Domain Validation Errors
 */
open class ValidationError(description: String) : KastleError(description) {
    class InvalidItemId(val id: String) : ValidationError(
        "Trying to create item with invalid ID ($id)." +
                "The ID must starts with 'i-'"
    )

    class InvalidRoomId(id: String) : ValidationError(
        "Trying to create room with invalid ID ($id)." +
                "The ID must starts with 'r-'"
    )

    class InvalidCharacterId(id: String) : ValidationError(
        "Trying to create character with invalid ID ($id)." +
                "The ID must starts with 'c-'"
    )

    class InvalidDialogueId(id: String) : ValidationError(
        "Trying to create character with invalid ID ($id)." +
                "The ID must starts with 'd-'"
    )

    override fun join(second: KastleError): KastleError =
        ValidationError("$description\n${second.description}")
}
