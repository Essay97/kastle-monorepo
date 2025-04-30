package com.saggiodev.kastle.model

import com.saggiodev.kastle.model.capabilities.Inspectable

sealed class Item : Inspectable {
    abstract val id: ItemId
    abstract val name: String
    abstract override val matchers: List<String>
}

data class RegularItem(
    override val id: ItemId,
    override val name: String,
    override val description: String,
    override val matchers: List<String>
) : Item()

data class StorableItem(
    override val id: ItemId,
    override val name: String,
    override val matchers: List<String>,
    override val description: String,
    val use: String
) : Item()

