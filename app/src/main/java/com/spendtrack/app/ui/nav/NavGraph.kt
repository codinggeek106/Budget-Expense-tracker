package com.spendtrack.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendtrack.app.ui.debug.DebugScreen
import com.spendtrack.app.ui.onboarding.PermissionScreen

object Routes {
    const val SETUP = "setup"
    const val DEBUG = "debug"
}

@Composable
fun SpendTrackNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            PermissionScreen(onOpenDebug = { navController.navigate(Routes.DEBUG) })
        }
        composable(Routes.DEBUG) {
            DebugScreen(onBack = { navController.popBackStack() })
        }
    }
}
