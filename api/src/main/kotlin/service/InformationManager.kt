package com.saggiodev.kastle.service

import com.saggiodev.kastle.model.GameMetadata
import com.saggiodev.kastle.model.Player

class InformationManager(val metadata: GameMetadata?, val player: Player, val preface: String, val epilogue: String)