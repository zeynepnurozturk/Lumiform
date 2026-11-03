package com.task.lumiform.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface LumiformDestination {

    @Serializable
    data object Home : LumiformDestination

    @Serializable
    data class FullImage(val src: String, val title: String) : LumiformDestination
}
