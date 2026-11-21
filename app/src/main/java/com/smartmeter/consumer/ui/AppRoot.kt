package com.smartmeter.consumer.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartmeter.consumer.AppContainer
import com.smartmeter.consumer.data.local.Session
import com.smartmeter.consumer.data.local.SessionState
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.history.HistoryScreen
import com.smartmeter.consumer.ui.home.HomeScreen
import com.smartmeter.consumer.ui.login.LoginScreen
import com.smartmeter.consumer.ui.profile.ChangePasswordScreen
import com.smartmeter.consumer.ui.profile.EditProfileScreen
import com.smartmeter.consumer.ui.profile.ProfileScreen
import com.smartmeter.consumer.ui.recharge.RechargeScreen
import com.smartmeter.consumer.ui.usage.ReadingsScreen
import com.smartmeter.consumer.ui.usage.UsageScreen

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

object Routes {
    const val HOME = "home"
    const val USAGE = "usage"
    const val RECHARGE = "recharge"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val READINGS = "readings"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.USAGE, "Usage", Icons.Outlined.BarChart),
    Tab(Routes.RECHARGE, "Recharge", Icons.Outlined.AccountBalanceWallet),
    Tab(Routes.HISTORY, "History", Icons.Outlined.History),
    Tab(Routes.PROFILE, "Profile", Icons.Outlined.Person),
)

// Shows the login screen or the main app depending on the saved session
@Composable
fun AppRoot(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        val sessionState by container.repository.sessionState
            .collectAsStateWithLifecycle(initialValue = SessionState.Loading)

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val state = sessionState) {
                SessionState.Loading -> LoadingState()
                SessionState.LoggedOut -> LoginScreen()
                is SessionState.LoggedIn -> key(state.session.token) {
                    MainScreen(state.session)
                }
            }
        }
    }
}

// Switch tabs keeping each tab's state (standard bottom navigation behavior)
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainScreen(session: Session) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TABS.any { it.route == destination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    session = session,
                    onRecharge = { navController.navigateToTab(Routes.RECHARGE) },
                    onUsage = { navController.navigateToTab(Routes.USAGE) },
                )
            }
            composable(Routes.USAGE) {
                UsageScreen(onOpenReadings = { navController.navigate(Routes.READINGS) })
            }
            composable(Routes.RECHARGE) {
                RechargeScreen(onOpenHistory = { navController.navigateToTab(Routes.HISTORY) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                )
            }
            composable(Routes.READINGS) {
                ReadingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
