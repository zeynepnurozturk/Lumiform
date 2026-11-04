package com.task.lumiform.ui.toast

import androidx.annotation.StringRes
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface ToastMessage {
    data class Resource(@StringRes val resId: Int) : ToastMessage
    data class Text(val text: String) : ToastMessage
}

/**
 * Lets view models show a toast without holding a context. MainActivity collects [messages] while started.
 */
@Singleton
class ToastManager @Inject constructor() {

    private val _messages = Channel<ToastMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun showToast(@StringRes resId: Int) {
        _messages.trySend(ToastMessage.Resource(resId))
    }

    fun showToast(text: String) {
        _messages.trySend(ToastMessage.Text(text))
    }
}
