package com.saggiodev.kastle.model

enum class LinkBehavior {
    /**
     * The link can only transition from locked to open.
     */
    OPENABLE {
        override val canOpen: Boolean = true
        override val canClose: Boolean = false
    },
    /**
     * The link can only transition from open to locked.
     */
    LOCKABLE {
        override val canOpen: Boolean = false
        override val canClose: Boolean = true
    },
    /**
     * The link can transition both from locked to open and from open to locked.
     */
    COMPLETE {
        override val canOpen: Boolean = true
        override val canClose: Boolean = true
    },

    /**
     * The link cannot change its state
     */
    CONSTANT {
        override val canOpen: Boolean = false
        override val canClose: Boolean = false
    };

    abstract val canOpen: Boolean
    abstract val canClose: Boolean
}

data class DungeonNode(
    val north: Link? = null,
    val south: Link? = null,
    val east: Link? = null,
    val west: Link? = null
)

data class Link(
    val destination: RoomId,
    var open: Boolean = true,
    val behavior: LinkBehavior = LinkBehavior.COMPLETE,
    val triggeredBy: List<ItemId> = emptyList()
)