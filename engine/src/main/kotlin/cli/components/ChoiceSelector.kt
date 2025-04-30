package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.underline
import com.varabyte.kotter.runtime.Session

fun Session.choiceSelector(choices: List<String>): Int {
    var choice by liveVarOf(0)

    section {
        choices.forEachIndexed { i, c ->
            val number = i + 1
            if (i == choice) {
                greenBold("["); text("$number"); greenBold("]  "); underline { textLine(c) }
            } else {
                textLine(" $number  $c")
            }
        }
    }.runUntilKeyPressed(Keys.ENTER) {
        onKeyPressed {
            if (key == Keys.UP) {
                choice = (choice - 1 + choices.size) % choices.size
            }
            if (key == Keys.DOWN) {
                choice = (choice + 1) % choices.size
            }
        }
    }

    return choice
}