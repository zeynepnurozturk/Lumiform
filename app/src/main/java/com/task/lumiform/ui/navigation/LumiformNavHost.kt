package com.task.lumiform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.task.lumiform.ui.LumiformHomeRoute
import com.task.lumiform.ui.image.LumiformImageRoute

@Composable
fun LumiformNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = LumiformDestination.Home,
        modifier = modifier,
    ) {
        composable<LumiformDestination.Home> {
            LumiformHomeRoute(
                onImageClick = { src, title ->
                    navController.navigate(LumiformDestination.FullImage(src = src, title = title))
                }
            )
        }
        composable<LumiformDestination.FullImage> {
            LumiformImageRoute(onBack = navController::popBackStack)
        }
    }
}
