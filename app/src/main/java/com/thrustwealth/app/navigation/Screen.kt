package com.thrustwealth.app.navigation

sealed class Screen(val route: String) {
    data object Calculator : Screen("calculator")
    data object Tools : Screen("tools")
    data object Journal : Screen("journal")
    data object Settings : Screen("settings")
    data object Pnl : Screen("pnl")
    data object Specs : Screen("specs")
    data object History : Screen("history")
}

val bottomNavScreens = listOf(
    Screen.Calculator,
    Screen.Tools,
    Screen.Journal,
    Screen.Settings
)
