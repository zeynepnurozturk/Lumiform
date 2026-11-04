package com.task.lumiform.ui.image

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.task.lumiform.ui.navigation.LumiformNavigator
import com.task.lumiform.ui.navigation.NavigationCommand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// SavedStateHandle.toRoute needs Android's Bundle, so this runs on a device.
@RunWith(AndroidJUnit4::class)
class LumiformImageViewModelTest {

    @Test
    fun readsSourceAndTitleFromTheRoute() {
        val viewModel = newViewModel(LumiformNavigator())

        assertEquals("https://example.com/image.png", viewModel.src)
        assertEquals("Welcome Image", viewModel.title)
    }

    @Test
    fun backAsksTheNavigatorToGoBack() = runBlocking {
        val navigator = LumiformNavigator()
        val viewModel = newViewModel(navigator)

        viewModel.callbacks.onBackClick()

        assertEquals(NavigationCommand.Back, navigator.commands.first())
    }

    private fun newViewModel(navigator: LumiformNavigator) = LumiformImageViewModel(
        savedStateHandle = SavedStateHandle(mapOf("src" to "https://example.com/image.png", "title" to "Welcome Image")),
        navigator = navigator,
    )
}
