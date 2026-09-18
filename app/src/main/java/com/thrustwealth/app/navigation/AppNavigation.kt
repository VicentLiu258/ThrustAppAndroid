package com.thrustwealth.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.ui.calculator.CalculatorScreen
import com.thrustwealth.app.ui.calculator.CalculatorViewModel
import com.thrustwealth.app.ui.chat.ChatScreen
import com.thrustwealth.app.ui.components.BottomTabBar
import com.thrustwealth.app.ui.history.HistoryScreen
import com.thrustwealth.app.ui.journal.JournalScreen
import com.thrustwealth.app.ui.pnl.PnlCalculatorScreen
import com.thrustwealth.app.ui.settings.SettingsScreen
import com.thrustwealth.app.ui.specs.MarketSpecsScreen
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.ui.tools.ToolsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = ThrustTheme.colors

    // Shared calculator VM scoped to activity for history restore
    val calculatorViewModel: CalculatorViewModel = hiltViewModel()
    val pendingRestore = remember { androidx.compose.runtime.mutableStateOf<CalculationRecord?>(null) }

    Scaffold(
        containerColor = colors.bgApp,
        bottomBar = {
            BottomTabBar(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.bgApp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Calculator.route
            ) {
                composable(Screen.Calculator.route) {
                    pendingRestore.value?.let { record ->
                        calculatorViewModel.restoreFromHistory(record)
                        pendingRestore.value = null
                    }
                    CalculatorScreen(
                        viewModel = calculatorViewModel,
                        onOpenHistory = { navController.navigate(Screen.History.route) },
                        onOpenPnl = { navController.navigate(Screen.Pnl.route) },
                        onOpenSpecs = { navController.navigate(Screen.Specs.route) },
                        onOpenJournal = {
                            navController.navigate(Screen.Journal.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenChat = { navController.navigate(Screen.Chat.route) }
                    )
                }
                composable(Screen.Tools.route) {
                    ToolsScreen(
                        onOpenPnl = { navController.navigate(Screen.Pnl.route) },
                        onOpenSpecs = { navController.navigate(Screen.Specs.route) }
                    )
                }
                composable(Screen.Journal.route) {
                    JournalScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onOpenSupport = { navController.navigate(Screen.Chat.route) }
                    )
                }
                composable(Screen.Chat.route) {
                    ChatScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Pnl.route) {
                    PnlCalculatorScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Specs.route) {
                    MarketSpecsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.History.route) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onRestore = { record ->
                            pendingRestore.value = record
                        }
                    )
                }
            }
        }
    }
}
