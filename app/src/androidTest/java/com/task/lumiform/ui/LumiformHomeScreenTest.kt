package com.task.lumiform.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.task.lumiform.R
import com.task.lumiform.domain.model.ResponseOptionDomainModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType
import com.task.lumiform.ui.model.LumiformHomePageUiModel
import com.task.lumiform.ui.theme.LumiformTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LumiformHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val callbacks = LumiformHomeCallbacks(
        onResponseClick = { question, responseId -> calls += "response ${question.id} $responseId" },
        onPreviousClick = { calls += "previous" },
        onNextClick = { calls += "next" },
        onFinishClick = { calls += "finish" },
        onImageClick = { src, title -> calls += "image $src $title" },
        onRetryClick = { calls += "retry" },
    )

    @Test
    fun fontSizesGetSmallerDownTheHierarchy() {
        setScreen(pageIndex = 0)

        val sizes = listOf("Main Page", "Chapter 1", "Subsection", "Deep section", "Intro text")
            .map { composeRule.fontSizeOf(it).value }

        assertEquals(sizes.sortedDescending(), sizes)
        assertEquals("every level has its own size", sizes.size, sizes.distinct().size)
    }

    @Test
    fun textQuestionShowsItsContent() {
        setScreen(pageIndex = 0)

        composeRule.onNodeWithText("Intro text").assertIsDisplayed()
        composeRule.onNodeWithText("Deep text").assertIsDisplayed()
    }

    @Test
    fun imageQuestionShowsItsTitleAndOpensOnTap() {
        setScreen(pageIndex = 0)

        composeRule.onNodeWithText("Welcome Image").assertIsDisplayed().performClick()

        assertEquals(listOf("image $IMAGE_SRC Welcome Image"), calls)
    }

    @Test
    fun singleChoiceShowsRadioButtonsWithTheSavedSelection() {
        setScreen(pageIndex = 1, selectedResponses = mapOf(SINGLE_ID to listOf(2)))

        composeRule.onNodeWithText("Yes").assert(hasRole(Role.RadioButton)).assertIsNotSelected()
        composeRule.onNodeWithText("No").assert(hasRole(Role.RadioButton)).assertIsSelected()
    }

    @Test
    fun multipleChoiceShowsCheckboxesWithTheSavedSelection() {
        setScreen(pageIndex = 1, selectedResponses = mapOf(MULTIPLE_ID to listOf(3, 4)))

        composeRule.onNodeWithText("Red").assert(hasRole(Role.Checkbox)).assertIsOn()
        composeRule.onNodeWithText("Blue").assert(hasRole(Role.Checkbox)).assertIsOn()
        composeRule.onNodeWithText("Green").assert(hasRole(Role.Checkbox)).assertIsOff()
    }

    @Test
    fun tappingAResponseReportsTheQuestionAndResponse() {
        setScreen(pageIndex = 1)

        composeRule.onNodeWithText("No").performClick()
        composeRule.onNodeWithText("Green").performClick()

        assertEquals(listOf("response $SINGLE_ID 2", "response $MULTIPLE_ID 5"), calls)
    }

    @Test
    fun firstPageHasPreviousDisabledAndNext() {
        setScreen(pageIndex = 0)

        composeRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.page_previous)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.page_next)).assertIsEnabled().performClick()

        assertEquals(listOf("next"), calls)
    }

    @Test
    fun lastPageShowsFinish() {
        setScreen(pageIndex = 1)

        composeRule.onNodeWithText(string(R.string.page_previous)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.page_finish)).performClick()

        assertEquals(listOf("finish"), calls)
    }

    @Test
    fun noPagesShowsTheEmptyMessage() {
        setScreen(uiState = LumiformHomeUiState.Empty, pages = emptyList())

        composeRule.onNodeWithText(string(R.string.pages_empty)).assertIsDisplayed()
    }

    @Test
    fun errorShowsTheMessageAndRetryReportsTheTap() {
        setScreen(uiState = LumiformHomeUiState.Error(R.string.error_network), pages = null)

        composeRule.onNodeWithText(string(R.string.error_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(listOf("retry"), calls)
    }

    private fun setScreen(
        pageIndex: Int = 0,
        selectedResponses: Map<Int, List<Int>> = emptyMap(),
        uiState: LumiformHomeUiState = LumiformHomeUiState.Success,
        pages: List<LumiformHomePageUiModel>? = PAGES,
    ) {
        composeRule.setContent {
            LumiformTheme(dynamicColor = false) {
                LumiformHomeScreen(
                    uiState = uiState,
                    pages = pages,
                    currentPageIndex = pageIndex,
                    selectedResponses = selectedResponses,
                    callbacks = callbacks,
                )
            }
        }
    }

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun ComposeContentTestRule.fontSizeOf(text: String): TextUnit {
        val node = onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        val getLayout = node.config[SemanticsActions.GetTextLayoutResult].action
        assertTrue("no text layout for $text", getLayout?.invoke(results) == true)
        return results.first().layoutInput.style.fontSize
    }

    private companion object {
        const val SINGLE_ID = 20
        const val MULTIPLE_ID = 21
        const val IMAGE_SRC = "https://example.invalid/image.png"

        val PAGES = listOf(
            LumiformHomePageUiModel(
                id = 1,
                title = "Main Page",
                items = listOf(
                    LumiformHomeItemUiModel.Section(
                        id = 2,
                        title = "Chapter 1",
                        items = listOf(
                            LumiformHomeItemUiModel.Text(id = 3, content = "Intro text"),
                            LumiformHomeItemUiModel.Section(
                                id = 4,
                                title = "Subsection",
                                items = listOf(
                                    LumiformHomeItemUiModel.Section(
                                        id = 5,
                                        title = "Deep section",
                                        items = listOf(LumiformHomeItemUiModel.Text(id = 6, content = "Deep text"))
                                    )
                                )
                            )
                        )
                    ),
                    LumiformHomeItemUiModel.Image(id = 7, src = IMAGE_SRC, title = "Welcome Image")
                )
            ),
            LumiformHomePageUiModel(
                id = 10,
                title = "Second Page",
                items = listOf(
                    LumiformHomeItemUiModel.Choice(
                        id = SINGLE_ID,
                        content = "Do you agree?",
                        selectionType = SelectionType.Single,
                        responses = listOf(
                            ResponseOptionDomainModel(id = 1, label = "Yes"),
                            ResponseOptionDomainModel(id = 2, label = "No")
                        )
                    ),
                    LumiformHomeItemUiModel.Choice(
                        id = MULTIPLE_ID,
                        content = "Pick colors",
                        selectionType = SelectionType.Multiple,
                        responses = listOf(
                            ResponseOptionDomainModel(id = 3, label = "Red"),
                            ResponseOptionDomainModel(id = 4, label = "Blue"),
                            ResponseOptionDomainModel(id = 5, label = "Green")
                        )
                    )
                )
            )
        )
    }
}
