package com.task.lumiform.ui.image

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.task.lumiform.ui.navigation.LumiformDestination
import com.task.lumiform.ui.navigation.LumiformNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LumiformImageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: LumiformNavigator,
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<LumiformDestination.FullImage>()

    val src: String = destination.src
    val title: String = destination.title

    val callbacks by lazy {
        LumiformImageCallbacks(
            onBackClick = navigator::back,
        )
    }
}

data class LumiformImageCallbacks(
    val onBackClick: () -> Unit,
)
