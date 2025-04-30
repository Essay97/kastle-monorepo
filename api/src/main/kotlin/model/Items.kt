package com.saggiodev.kastle.model

object Items {
    private val items = mutableMapOf<ItemId, Item>()

    fun getById(id: ItemId): Item? = items[id]

    fun getStorableById(id: ItemId): StorableItem? = if (items[id] is StorableItem) items[id] as StorableItem else null

    fun add(vararg items: Item) {
        items.forEach {
            this.items[it.id] = it
        }
    }
}