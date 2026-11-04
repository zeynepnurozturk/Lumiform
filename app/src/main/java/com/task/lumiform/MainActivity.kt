package com.task.lumiform

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.task.lumiform.ui.navigation.LumiformNavHost
import com.task.lumiform.ui.navigation.LumiformNavigator
import com.task.lumiform.ui.theme.LumiformTheme
import com.task.lumiform.ui.toast.ToastManager
import com.task.lumiform.ui.toast.ToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigator: LumiformNavigator

    @Inject lateinit var toastManager: ToastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            LumiformTheme {
                LumiformNavHost(navigator = navigator)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                toastManager.messages.collect(::showToast)
            }
        }
    }

    private fun showToast(message: ToastMessage) {
        val text = when (message) {
            is ToastMessage.Resource -> getString(message.resId)
            is ToastMessage.Text -> message.text
        }
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
