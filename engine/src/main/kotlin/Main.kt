package com.saggiodev.kastle.engine

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.saggiodev.kastle.cli.commands.*

fun main(args: Array<String>) = Kastle()
    .subcommands(List(), Install(), Uninstall(), Play(), Info())
    .main(args)
