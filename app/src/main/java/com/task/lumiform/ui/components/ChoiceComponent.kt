package com.task.lumiform.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.task.lumiform.ui.model.LumiformHomeItemUiModel
import com.task.lumiform.ui.model.LumiformHomeItemUiModel.Choice.SelectionType

/**
 * Shows radio buttons for [SelectionType.Single] and checkboxes for [SelectionType.Multiple].
 */
@Composable
fun ChoiceComponent(
    question: LumiformHomeItemUiModel.Choice,
    selected: List<Int>,
    onResponseClick: (responseId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TextComponent(text = question.content, style = MaterialTheme.typography.bodySmall)
        val groupModifier = when (question.selectionType) {
            SelectionType.Single -> Modifier.selectableGroup()
            SelectionType.Multiple -> Modifier
        }
        Column(modifier = groupModifier) {
            question.responses.forEach { response ->
                val isSelected = response.id in selected
                val rowModifier = when (question.selectionType) {
                    SelectionType.Single -> Modifier.selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onResponseClick(response.id) }
                    )
                    SelectionType.Multiple -> Modifier.toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onResponseClick(response.id) }
                    )
                }
                Row(
                    modifier = rowModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (question.selectionType) {
                        SelectionType.Single -> RadioButton(selected = isSelected, onClick = null)
                        SelectionType.Multiple -> Checkbox(checked = isSelected, onCheckedChange = null)
                    }
                    TextComponent(
                        text = response.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
