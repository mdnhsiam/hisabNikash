package com.hisabnikash.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun HisabNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentPath = backStackEntry?.destination?.route ?: Route.Dashboard.path

    Scaffold(
        bottomBar = {
            NavigationBar {
                Route.bottomRoutes.forEach { route ->
                    NavigationBarItem(
                        selected = currentPath == route.path,
                        onClick = {
                            navController.navigate(route.path) {
                                popUpTo(Route.Dashboard.path) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        },
                        label = { Text(stringResource(route.labelRes)) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Text(text = "+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard.path,
            modifier = Modifier.padding(padding),
        ) {
            Route.bottomRoutes.forEach { route ->
                composable(route.path) { PlaceholderScreen(route) }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(route: Route) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(route.labelRes), style = MaterialTheme.typography.headlineMedium)
    }
}
