package com.saggiodev.kastle.cli.components

import com.varabyte.kotter.foundation.render.offscreen
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import com.varabyte.kotterx.decorations.BorderCharacters
import com.varabyte.kotterx.decorations.bordered
import com.varabyte.kotterx.text.Justification
import com.varabyte.kotterx.text.justified
import com.saggiodev.kastle.model.GameMetadata

fun Session.header(metadata: GameMetadata?, gameName: String) = section {
    bordered(
        borderCharacters = BorderCharacters.CURVED,
        paddingTopBottom = 0,
        paddingLeftRight = 2
    ) {
        val buffer = offscreen {
            if (metadata != null) {
                if (metadata.author != null) {
                    text("Author: ")
                    bold { textLine(metadata.author!!) }
                }
                if (metadata.version != null) {
                    text("Version: ")
                    bold { textLine(metadata.version!!) }
                }
                if (metadata.published != null) {
                    text("Published: ")
                    bold {
                        textLine(
                            with(metadata.published) {
                                "${this!!.dayOfMonth}/${
                                    monthNumber.toString().padStart(2, '0')
                                }/${year}"
                            }
                        )
                    }
                }
                if (metadata.kastleVersions != null) {
                    text("Compatible with: Kastle ")
                    bold { textLine(metadata.kastleVersions!!.joinToString(", ")) }
                }
            }
        }

        justified(
            justification = Justification.CENTER,
            minWidth = maxOf(gameName.length + 16, buffer.lineLengths.maxOrNull() ?: 0)
        ) {
            val title = metadata?.gameName ?: gameName
            bold { text(title.uppercase()) }
        }

        val renderer = buffer.createRenderer()
        while (renderer.hasNextRow()) {
            renderer.renderNextRow()
            textLine()
        }
    }
    textLine()
}.run()