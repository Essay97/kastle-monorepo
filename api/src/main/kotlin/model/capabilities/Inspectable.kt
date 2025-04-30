package com.saggiodev.kastle.model.capabilities

interface Inspectable {
    val description: String
    val matchers: List<String>
}