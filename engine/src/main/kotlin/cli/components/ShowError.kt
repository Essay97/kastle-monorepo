package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session

fun Session.showError(error: String) {
    section {
        red { textLine(error) }
    }.run()
}

fun Session.showWarning(error: String) {
    section {
        yellow { textLine(error) }
    }.run()
}