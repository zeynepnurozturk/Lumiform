package com.task.lumiform.ui.summary

data class LumiformSummaryPageUiModel(
    val id: Int,
    val title: String,
    val answers: List<LumiformSummaryAnswerUiModel>
)

/**
 * One choice question with the labels of its selected responses. [answers] is empty when it was skipped.
 */
data class LumiformSummaryAnswerUiModel(
    val questionId: Int,
    val question: String,
    val answers: List<String>,
    val score: Int
)
