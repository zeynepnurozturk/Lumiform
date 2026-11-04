package com.task.lumiform.ui.image

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.task.lumiform.ui.theme.LumiformTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LumiformImageScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheTitleAndTheImage() {
        setScreen {}

        composeRule.onNodeWithText("Welcome Image").assertIsDisplayed()
        // The image itself is described by its title, so it is found by content description.
        composeRule.onNodeWithContentDescription("Welcome Image").assertIsDisplayed()
    }

    @Test
    fun backButtonCallsBack() {
        var backClicks = 0
        setScreen { backClicks++ }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }

    private fun setScreen(onBackClick: () -> Unit) {
        composeRule.setContent {
            LumiformTheme(dynamicColor = false) {
                LumiformImageScreen(
                    src = "https://example.invalid/image.png",
                    title = "Welcome Image",
                    callbacks = LumiformImageCallbacks(onBackClick = onBackClick),
                )
            }
        }
    }
}
