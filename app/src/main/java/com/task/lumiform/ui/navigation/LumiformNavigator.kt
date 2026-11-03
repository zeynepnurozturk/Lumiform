package com.task.lumiform.ui.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface NavigationCommand {
    data class To(val destination: LumiformDestination) : NavigationCommand
    data object Back : NavigationCommand
}

/**
 * Lets view models trigger navigation without knowing the nav controller. [LumiformNavHost] collects [commands].
 */
@Singleton
class LumiformNavigator @Inject constructor() {

    private val _commands = Channel<NavigationCommand>(Channel.BUFFERED)
    val commands = _commands.receiveAsFlow()

    fun navigate(destination: LumiformDestination) {
        _commands.trySend(NavigationCommand.To(destination))
    }

    fun back() {
        _commands.trySend(NavigationCommand.Back)
    }
}
