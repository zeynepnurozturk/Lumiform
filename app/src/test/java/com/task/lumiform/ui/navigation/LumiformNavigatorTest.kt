package com.task.lumiform.ui.navigation

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LumiformNavigatorTest {

    @Test
    fun `commands are delivered in the order they were sent`() = runTest {
        val navigator = LumiformNavigator()

        navigator.navigate(LumiformDestination.FullImage(src = "url", title = "Image"))
        navigator.back()
        navigator.navigate(LumiformDestination.Summary)

        assertEquals(
            listOf(
                NavigationCommand.To(LumiformDestination.FullImage(src = "url", title = "Image")),
                NavigationCommand.Back,
                NavigationCommand.To(LumiformDestination.Summary)
            ),
            navigator.commands.take(3).toList()
        )
    }

    @Test
    fun `commands sent before anyone collects are not lost`() = runTest {
        val navigator = LumiformNavigator()
        navigator.back()

        assertEquals(listOf(NavigationCommand.Back), navigator.commands.take(1).toList())
    }
}
