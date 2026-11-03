package com.task.lumiform.ui.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.task.lumiform.R
import com.task.lumiform.ui.components.ImageComponent
import com.task.lumiform.ui.components.TitleComponent

@Composable
fun LumiformImageRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LumiformImageViewModel = hiltViewModel(),
) {
    LumiformImageScreen(
        modifier = modifier,
        src = viewModel.src,
        title = viewModel.title,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumiformImageScreen(
    src: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleComponent(title = title, style = MaterialTheme.typography.headlineMedium)
            ImageComponent(
                src = src,
                title = title,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
