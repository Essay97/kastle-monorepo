package com.saggiodev.kastle.dto

import arrow.core.Either
import com.saggiodev.kastle.error.GameDefinitionError
import com.saggiodev.kastle.error.ValidationError
import com.saggiodev.kastle.model.*

/** Validate the complete definition against its own IDs before touching runtime registries. */
internal fun GameConfiguration.validateDefinition(): Either<GameDefinitionError, Unit> {
    val errors = mutableListOf<String>()
    fun checkId(context: String, result: Either<ValidationError, *>) {
        result.fold({ errors += "$context: ${it.description}" }, {})
    }
    fun duplicates(ids: List<String>, context: String) {
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            errors += "$context: duplicate ID '$it'"
        }
    }
    fun reference(id: String, ids: Set<String>, context: String) {
        if (id !in ids) errors += "$context: unknown reference '$id'"
    }

    val roomIds = rooms.map { it.id }.toSet()
    val itemIds = items.orEmpty().map { it.id }.toSet()
    val characterIds = characters.orEmpty().map { it.id }.toSet()
    duplicates(rooms.map { it.id }, "Rooms")
    duplicates(items.orEmpty().map { it.id }, "Items")
    duplicates(characters.orEmpty().map { it.id }, "Characters")
    checkId("Initial room", RoomId(initialRoomId))
    reference(initialRoomId, roomIds, "Initial room")
    items.orEmpty().forEach { checkId("Item '${it.id}'", ItemId(it.id)) }
    rooms.forEach { room ->
        val context = "Room '${room.id}'"
        checkId(context, RoomId(room.id))
        room.items.orEmpty().forEach {
            checkId("$context item", ItemId(it))
            reference(it, itemIds, "$context item")
        }
        room.characters.orEmpty().forEach {
            checkId("$context character", CharacterId(it))
            reference(it, characterIds, "$context character")
        }
        val links = room.links
        listOf("north" to links?.north, "south" to links?.south,
            "east" to links?.east, "west" to links?.west).forEach { (direction, link) ->
            if (link != null) {
                checkId("$context $direction", RoomId(link.roomId))
                reference(link.roomId, roomIds, "$context $direction")
                link.state.triggers.orEmpty().forEach {
                    checkId("$context $direction trigger", ItemId(it))
                    reference(it, itemIds, "$context $direction trigger")
                }
            }
        }
    }
    characters.orEmpty().forEach { character ->
        val context = "Character '${character.id}' dialogue"
        checkId("Character '${character.id}'", CharacterId(character.id))
        character.dialogue?.let { dialogue ->
            val questionIds = dialogue.questions.map { it.id }.toSet()
            duplicates(dialogue.questions.map { it.id }, context)
            checkId("$context first question", DialogueId(dialogue.firstQuestion))
            reference(dialogue.firstQuestion, questionIds, "$context first question")
            dialogue.questions.forEach { question ->
                val questionContext = "$context question '${question.id}'"
                checkId(questionContext, DialogueId(question.id))
                if (question.answers != null) {
                    if (question.answers.isEmpty()) errors += "$questionContext: answers must not be empty (use a terminal question)"
                    if (question.reward != null) errors += "$questionContext: a reward is only allowed on a terminal question"
                }
                question.reward?.let {
                    checkId("$questionContext reward", ItemId(it))
                    reference(it, itemIds, "$questionContext reward")
                }
                question.answers.orEmpty().forEach {
                    checkId("$questionContext answer", DialogueId(it.nextQuestion))
                    reference(it.nextQuestion, questionIds, "$questionContext answer")
                }
            }
            // Iterative topological traversal includes unreachable questions and shared branches.
            val edges = dialogue.questions.associate { question ->
                question.id to question.answers.orEmpty().map { it.nextQuestion }.filter { it in questionIds }
            }
            val incoming = questionIds.associateWith { 0 }.toMutableMap()
            edges.values.flatten().forEach { incoming[it] = incoming.getValue(it) + 1 }
            val ready = ArrayDeque(incoming.filterValues { it == 0 }.keys)
            var visited = 0
            while (ready.isNotEmpty()) {
                val id = ready.removeFirst()
                visited++
                edges[id].orEmpty().forEach {
                    incoming[it] = incoming.getValue(it) - 1
                    if (incoming[it] == 0) ready.addLast(it)
                }
            }
            if (visited != questionIds.size) {
                errors += "$context: dialogue cycle detected; blocked questions: ${incoming.filterValues { it > 0 }.keys.joinToString()}"
            }
        }
    }
    winningConditions?.playerOwns?.let {
        checkId("Winning condition playerOwns", ItemId(it))
        reference(it, itemIds, "Winning condition playerOwns")
    }
    winningConditions?.playerEnters?.let {
        checkId("Winning condition playerEnters", RoomId(it))
        reference(it, roomIds, "Winning condition playerEnters")
    }
    return if (errors.isEmpty()) Either.Right(Unit)
    else Either.Left(GameDefinitionError(errors.joinToString("\n")))
}
