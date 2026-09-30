package com.spendtrack.app.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.spendtrack.app.R
import com.spendtrack.app.appContainer
import com.spendtrack.app.ui.budget.BudgetScreen
import com.spendtrack.app.ui.dashboard.DashboardScreen
import com.spendtrack.app.ui.debug.DebugScreen
import com.spendtrack.app.ui.history.HistoryScreen
import com.spendtrack.app.ui.manual.ManualEntryScreen
import com.spendtrack.app.ui.onboarding.PermissionScreen
import com.spendtrack.app.ui.onboarding.SetupStatus
import com.spendtrack.app.ui.pending.PendingScreen

object Routes {
    const val SETUP = "setup"
    const val DEBUG = "debug"
    const val MANUAL = "manual"

    /** Nested graph holding the bottom-nav tabs. */
    const val MAIN = "main"
    const val DASHBOARD = "dashboard"
    const val PENDING = "pending"
    const val HISTORY = "history"
    const val BUDGET = "budget"
}

private sealed class TabIcon {
    data class Vector(val image: ImageVector) : TabIcon()
    data class Drawable(@param:DrawableRes val id: Int) : TabIcon()
}

private data class Tab(val route: String, val label: String, val icon: TabIcon)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Dashboard", TabIcon.Drawable(R.drawable.ic_tab_dashboard)),
    Tab(Routes.PENDING, "Pending", TabIcon.Vector(Icons.Filled.Notifications)),
    Tab(Routes.HISTORY, "History", TabIcon.Vector(Icons.AutoMirrored.Filled.List)),
    Tab(Routes.BUDGET, "Budget", TabIcon.Drawable(R.drawable.ic_tab_budget)),
)

@Composable
fun SpendTrackNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    // First run (no notification access yet) starts on setup; afterwards straight into the app.
    val start = remember { if (SetupStatus.read(context).listenerEnabled) Routes.MAIN else Routes.SETUP }
    val pendingCount by remember { context.appContainer.transactionRepository.observePendingCount() }
        .collectAsStateWithLifecycle(initialValue = 0)

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoutes: Set<String?> = backStack?.destination?.hierarchy?.map { it.route }?.toSet().orEmpty()
    val showBottomBar = Routes.MAIN in currentRoutes

    Scaffold(
        // Screens handle system insets themselves; this Scaffold only reserves the bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.route in currentRoutes,
                            onClick = { navController.navigateToTab(tab.route) },
                            label = { Text(tab.label) },
                            icon = {
                                if (tab.route == Routes.PENDING && pendingCount > 0) {
                                    BadgedBox(badge = { Badge { Text(if (pendingCount > 99) "99+" else "$pendingCount") } }) {
                                        TabIconImage(tab)
                                    }
                                } else {
                                    TabIconImage(tab)
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            // Each screen draws its own top bar; only the bottom bar's height is reserved here.
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.SETUP) {
                PermissionScreen(
                    onOpenDebug = { navController.navigate(Routes.DEBUG) },
                    onDone = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.SETUP) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable(Routes.DEBUG) {
                DebugScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MANUAL) {
                ManualEntryScreen(onBack = { navController.popBackStack() })
            }
            navigation(route = Routes.MAIN, startDestination = Routes.DASHBOARD) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenSetup = { navController.navigate(Routes.SETUP) },
                        onOpenPending = { navController.navigateToTab(Routes.PENDING) },
                        onOpenBudgets = { navController.navigateToTab(Routes.BUDGET) },
                        onAddCash = { navController.navigate(Routes.MANUAL) },
                    )
                }
                composable(Routes.PENDING) {
                    PendingScreen(onOpenSetup = { navController.navigate(Routes.SETUP) })
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(onAddCash = { navController.navigate(Routes.MANUAL) })
                }
                composable(Routes.BUDGET) {
                    BudgetScreen()
                }
            }
        }
    }
}

@Composable
private fun TabIconImage(tab: Tab) {
    when (val icon = tab.icon) {
        is TabIcon.Vector -> Icon(icon.image, contentDescription = null)
        is TabIcon.Drawable -> Icon(painterResource(icon.id), contentDescription = null)
    }
}

/** Standard bottom-nav behaviour: one copy of each tab, state kept when switching. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
