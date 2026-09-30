package com.spendtrack.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spendtrack.app.ui.debug.DebugScreen
import com.spendtrack.app.ui.onboarding.PermissionScreen
import com.spendtrack.app.ui.onboarding.SetupStatus
import com.spendtrack.app.ui.pending.PendingScreen

object Routes {
    const val SETUP = "setup"
    const val PENDING = "pending"
    const val DEBUG = "debug"
}

@Composable
fun SpendTrackNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    // First run (no notification access yet) starts on setup; afterwards straight into the app.
    val start = remember { if (SetupStatus.read(context).listenerEnabled) Routes.PENDING else Routes.SETUP }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.SETUP) {
            PermissionScreen(
                onOpenDebug = { navController.navigate(Routes.DEBUG) },
                onDone = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.PENDING) {
                            popUpTo(Routes.SETUP) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.PENDING) {
            PendingScreen(onOpenSetup = { navController.navigate(Routes.SETUP) })
        }
        composable(Routes.DEBUG) {
            DebugScreen(onBack = { navController.popBackStack() })
        }
    }
}
