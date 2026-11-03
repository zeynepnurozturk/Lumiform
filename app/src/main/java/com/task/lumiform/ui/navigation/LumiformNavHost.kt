package com.task.lumiform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.task.lumiform.ui.LumiformHomeRoute
import com.task.lumiform.ui.image.LumiformImageRoute
import com.task.lumiform.ui.summary.LumiformSummaryRoute

@Composable
fun LumiformNavHost(
    navigator: LumiformNavigator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(navigator, navController) {
        navigator.commands.collect { command ->
            when (command) {
                is NavigationCommand.To -> navController.navigate(command.destination)
                NavigationCommand.Back -> navController.popBackStack()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = LumiformDestination.Home,
        modifier = modifier,
    ) {
        composable<LumiformDestination.Home> {
            LumiformHomeRoute()
        }
        composable<LumiformDestination.FullImage> {
            LumiformImageRoute()
        }
        composable<LumiformDestination.Summary> {
            LumiformSummaryRoute()
        }
    }
}
