package com.mrdastbaaf.mdbapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.features.home.presentation.HomeScreen
import com.mrdastbaaf.mdbapp.features.home.presentation.HomeViewModel

/**
 * The navigation graph.
 *
 * Phase P0 hosts a single destination. The graph is structured so that the auth
 * guard can be added in P3 without moving anything: [SessionManager.unauthorized]
 * is already collected here, and the only change required will be a redirect to
 * [Routes.LOGIN].
 */
@Composable
fun MdbNavHost(
    sessionManager: SessionManager,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME,
) {
    // A 401 anywhere in the app funnels through SessionManager. Wiring it here,
    // once, is what stops individual screens from each inventing their own
    // "session expired" handling.
    LaunchedEffect(Unit) {
        sessionManager.unauthorized.collect {
            // P3: navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                isOffline = isOffline,
                onRetry = viewModel::refresh,
            )
        }
    }
}
