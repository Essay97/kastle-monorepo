package com.saggiodev.kastle.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right

sealed class Question(val text: String) {
    class Node(text: String, val answers: List<Answer>) : Question(text)
    class Leaf(text: String, val reward: ItemId?) : Question(text)
    // This class is used in Dialogue as a return type to return only printable content and keep the "dialogue navigation"
    // logic inside the Dialogue class
    class NodeText(from: Node) {
        val question: String = from.text
        val answers: List<String> = from.answers.map { it.text }
    }
}

class Answer(val text: String, val nextQuestion: Question)

class Dialogue(private val starter: Question, val talker: CharacterId) {
    private var current = starter

    fun first(): Either<Question.NodeText, Question.Leaf> {
        current = starter
        return getCurrent()
    }

    fun next(choice: Int): Either<Question.NodeText, Question.Leaf> {
        val node = current as Question.Node
        current = node.answers[choice].nextQuestion
        return getCurrent()
    }

    fun hasNext(): Boolean = current is Question.Node

    private fun getCurrent(): Either<Question.NodeText, Question.Leaf> = when (current) {
        is Question.Leaf -> (current as Question.Leaf).right()
        is Question.Node -> Question.NodeText(current as Question.Node).left()
    }
}