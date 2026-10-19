package com.mrdastbaaf.mdbapp.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.settings.ThemeViewModel
import com.mrdastbaaf.mdbapp.core.util.ExternalLinks
import com.mrdastbaaf.mdbapp.features.account.presentation.DashboardScreen
import com.mrdastbaaf.mdbapp.features.account.presentation.DashboardViewModel
import com.mrdastbaaf.mdbapp.features.auth.presentation.session.SessionViewModel
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CatalogueScreen
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CatalogueViewModel
import com.mrdastbaaf.mdbapp.features.home.presentation.HomeViewModel
import com.mrdastbaaf.mdbapp.features.shop.presentation.ShopScreen
import com.mrdastbaaf.mdbapp.features.shop.presentation.ShopViewModel

/** The destinations reachable from the bottom bar. */
enum class MainTab { COURSES, SHOP, ACCOUNT }

/**
 * The signed-in shell.
 *
 * The tab selection is [rememberSaveable] rather than a navigation destination:
 * pressing back from a course detail must land on the tab the user came from,
 * and a tab switch must not push anything onto the back stack. The NavHost keeps
 * the saved state while this screen is behind another destination, which is what
 * makes that work without a nested graph.
 *
 * Each tab keeps its own `Scaffold` for the top bar — hence
 * `contentWindowInsets = WindowInsets(0)`: this outer scaffold owns only the
 * bottom bar, and letting it also apply the top inset would double-pad the
 * screen inside it.
 */
@Composable
fun MainScaffold(
    onOpenCourse: (Int) -> Unit,
    onOpenProduct: (Int) -> Unit,
    onSignedOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.COURSES) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.COURSES,
                    onClick = { tab = MainTab.COURSES },
                    icon = {
                        Icon(imageVector = Icons.Filled.Home, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.nav_courses)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.SHOP,
                    onClick = { tab = MainTab.SHOP },
                    icon = {
                        Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.nav_shop)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.ACCOUNT,
                    onClick = { tab = MainTab.ACCOUNT },
                    icon = {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.nav_account)) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (tab) {
                MainTab.COURSES -> CatalogueRoute(onOpenCourse = onOpenCourse)
                MainTab.SHOP -> ShopRoute(onOpenProduct = onOpenProduct)
                MainTab.ACCOUNT -> AccountRoute(onSignedOut = onSignedOut)
            }
        }
    }
}

@Composable
private fun CatalogueRoute(onOpenCourse: (Int) -> Unit) {
    val viewModel: CatalogueViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    CatalogueScreen(
        state = state,
        isOffline = isOffline,
        onModeSelected = viewModel::selectMode,
        onRetry = viewModel::refresh,
        onOpenCourse = onOpenCourse,
    )
}

@Composable
private fun ShopRoute(onOpenProduct: (Int) -> Unit) {
    val viewModel: ShopViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    ShopScreen(
        state = state,
        isOffline = isOffline,
        onRetry = viewModel::refresh,
        onOpenProduct = onOpenProduct,
    )
}

@Composable
private fun AccountRoute(onSignedOut: () -> Unit) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()

    val profile by dashboardViewModel.profile.collectAsStateWithLifecycle()
    val plans by dashboardViewModel.plans.collectAsStateWithLifecycle()
    val orders by dashboardViewModel.orders.collectAsStateWithLifecycle()
    val serverInfo by homeViewModel.state.collectAsStateWithLifecycle()
    val isOffline by homeViewModel.isOffline.collectAsStateWithLifecycle()
    val themePreference by themeViewModel.preference.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // An explicit sign-out (or a token revoked elsewhere) must leave this tab,
    // not sit on a dashboard for a session that no longer exists.
    LaunchedEffect(sessionState.isSignedIn) {
        if (!sessionState.isSignedIn) onSignedOut()
    }

    DashboardScreen(
        profile = profile,
        plans = plans,
        orders = orders,
        serverInfo = serverInfo,
        themePreference = themePreference,
        isOffline = isOffline,
        onRefresh = {
            dashboardViewModel.refresh()
            homeViewModel.refresh()
            sessionViewModel.refreshProfile()
        },
        onRetryPlans = dashboardViewModel::refresh,
        onRetryOrders = dashboardViewModel::refresh,
        onThemeSelected = themeViewModel::select,
        onSignOut = sessionViewModel::logout,
        onOpenMembership = {
            ExternalLinks.open(context, ExternalLinks.MEMBERSHIP_URL)
        },
    )
}
