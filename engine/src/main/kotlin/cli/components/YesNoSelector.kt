package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.underline
import com.varabyte.kotter.runtime.Session

fun Session.yesNoSelector(question: String, initialState: Boolean = false): Boolean {
    var state by liveVarOf(initialState)
    section {
        question(question)
        if (state) {
            greenBold(" ["); underline { text("Yes") }; greenBold("]"); text("  No ")
        } else {
            text("  Yes  "); greenBold("["); underline { text("No") }; greenBold("]")
        }
    }.runUntilKeyPressed(Keys.ENTER) {
        onKeyPressed {
            if (key == Keys.LEFT || key == Keys.RIGHT) {
                state = !state
            }
        }
    }

    return state
}