package com.task.lumiform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.task.lumiform.ui.navigation.LumiformNavHost
import com.task.lumiform.ui.navigation.LumiformNavigator
import com.task.lumiform.ui.theme.LumiformTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigator: LumiformNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LumiformTheme {
                LumiformNavHost(navigator = navigator)
            }
        }
    }
}
