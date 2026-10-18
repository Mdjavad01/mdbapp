package com.mrdastbaaf.mdbapp.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mrdastbaaf.mdbapp.core.session.SessionManager
import com.mrdastbaaf.mdbapp.core.session.SessionState
import com.mrdastbaaf.mdbapp.features.auth.presentation.login.LoginScreen
import com.mrdastbaaf.mdbapp.features.auth.presentation.otp.OtpScreen
import com.mrdastbaaf.mdbapp.features.auth.presentation.otp.OtpViewModel
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CourseDetailScreen
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CourseDetailViewModel
import com.mrdastbaaf.mdbapp.features.player.presentation.PlayerScreen
import com.mrdastbaaf.mdbapp.features.player.presentation.PlayerViewModel

/**
 * The navigation graph.
 *
 * The auth guard is structural rather than per-screen: the start destination is
 * chosen from the session state, and any 401 anywhere in the app funnels through
 * [SessionManager.unauthorized] to here. That is what stops individual screens
 * from each inventing their own "session expired" handling — and, more
 * importantly, what stops one of them forgetting to.
 */
@Composable
fun MdbNavHost(
    sessionManager: SessionManager,
    navController: NavHostController = rememberNavController(),
) {
    val sessionState by sessionManager.state.collectAsStateWithLifecycle()

    // Read once, at graph construction. Later changes are handled by the two
    // LaunchedEffects below, because recreating the NavHost would reset the
    // back stack under the user's feet.
    val startDestination = when (sessionState) {
        SessionState.SignedIn -> Routes.MAIN
        SessionState.SignedOut -> Routes.LOGIN
    }

    /** Clears the whole back stack and lands on [route]. */
    fun resetTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // A rejected token (expired after 30 days, revoked from another login, or
    // wiped server-side) must land the user on login with no way back into a
    // screen that will only fail again.
    LaunchedEffect(Unit) {
        sessionManager.unauthorized.collect {
            resetTo(Routes.LOGIN)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ── Auth ────────────────────────────────────────────────────────────

        composable(Routes.LOGIN) {
            LoginScreen(
                onOtpRequested = { phone, isRegistered, _ ->
                    navController.navigate(Routes.otp(phone, isRegistered))
                },
            )
        }

        composable(
            route = Routes.OTP,
            arguments = listOf(
                navArgument(OtpViewModel.ARG_PHONE) { type = NavType.StringType },
                navArgument(OtpViewModel.ARG_REGISTERED) { type = NavType.StringType },
            ),
        ) {
            OtpScreen(
                onSignedIn = { resetTo(Routes.MAIN) },
                onChangePhone = { navController.popBackStack() },
            )
        }

        // ── Signed-in shell ─────────────────────────────────────────────────

        composable(Routes.MAIN) {
            MainScaffold(
                onOpenCourse = { productId ->
                    navController.navigate(Routes.courseDetail(productId))
                },
                onSignedOut = { resetTo(Routes.LOGIN) },
            )
        }

        composable(
            route = Routes.COURSE_DETAIL,
            arguments = listOf(
                navArgument(CourseDetailViewModel.ARG_PRODUCT_ID) { type = NavType.IntType },
            ),
        ) {
            val viewModel: CourseDetailViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

            CourseDetailScreen(
                state = state,
                isOffline = isOffline,
                onBack = { navController.popBackStack() },
                onRetry = viewModel::refresh,
                onPlayLesson = { lessonId ->
                    navController.navigate(Routes.player(lessonId))
                },
            )
        }

        // ── Player ──────────────────────────────────────────────────────────

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument(PlayerViewModel.ARG_LESSON_ID) { type = NavType.StringType },
                navArgument(ARG_COURSE_SLUG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Declared by `Routes.PLAYER`; carried so the back stack can name the course. */
private const val ARG_COURSE_SLUG = "courseSlug"
