package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.render.RenderScope

fun RenderScope.prompt() = green { bold { text("> ") } }
fun RenderScope.question(q: String) {
    green { bold { text("? ") } }
    textLine(q)
}