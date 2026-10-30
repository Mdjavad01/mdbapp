package com.mrdastbaaf.mdbapp.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.settings.ThemeViewModel
import com.mrdastbaaf.mdbapp.features.account.presentation.DashboardScreen
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.HomeConfig
import com.mrdastbaaf.mdbapp.features.appcontent.presentation.HomeScreen
// Aliased: two ViewModels are called HomeViewModel — this one drives the
// landing screen, the other supplies the server-info card on the account tab.
import com.mrdastbaaf.mdbapp.features.appcontent.presentation.HomeViewModel as HomeContentViewModel
import com.mrdastbaaf.mdbapp.features.account.presentation.DashboardViewModel
import com.mrdastbaaf.mdbapp.features.auth.presentation.session.SessionViewModel
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CatalogueScreen
import com.mrdastbaaf.mdbapp.features.catalogue.presentation.CatalogueViewModel
import com.mrdastbaaf.mdbapp.features.home.presentation.HomeViewModel
import com.mrdastbaaf.mdbapp.features.shop.presentation.ShopScreen
import com.mrdastbaaf.mdbapp.features.shop.presentation.ShopViewModel

/**
 * The destinations reachable from the bottom bar.
 *
 * [key] is the panel's dock key, not the app's own name: `catalogue` and
 * `dashboard` are what the site calls the courses tab and the account tab. There
 * is no key for [SHOP] — the web dock has never had a shop tab — so the shop is
 * switched by the `physical_products` section instead.
 */
enum class MainTab(
    val key: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    COURSES("catalogue", R.string.nav_courses, Icons.Filled.PlayArrow),
    SHOP("shop", R.string.nav_shop, Icons.Filled.ShoppingCart),
    ACCOUNT("dashboard", R.string.nav_account, Icons.Filled.Person),
    ;

    companion object {
        /**
         * The tabs the panel has not switched off.
         *
         * Read as a set of "off" signals, never as a replacement list: the panel
         * describes the website's dock, and copying it literally would delete the
         * shop tab, which the site does not have. [HOME] is never hidden — an app
         * that opens on a tab the owner disabled elsewhere is a bug report, and
         * one tab alone is not a shell, so a combination that leaves fewer than
         * two falls back to all four.
         */
        fun visibleIn(config: HomeConfig?): List<MainTab> {
            if (config == null) return values().toList()

            val dock = config.dock.associateBy { it.key }
            val hidden = mutableSetOf<MainTab>()
            if (dock[COURSES.key]?.enabled == false) hidden += COURSES
            if (dock[ACCOUNT.key]?.enabled == false) hidden += ACCOUNT
            if (!config.shopEnabled) hidden += SHOP

            val visible = values().filterNot { it in hidden }
            return if (visible.size < 2) values().toList() else visible
        }
    }
}

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
    onOpenPage: (String) -> Unit,
    onBuyPlan: (String) -> Unit,
    onOpenPlans: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val shell: AppShellViewModel = hiltViewModel()
    val config by shell.config.collectAsStateWithLifecycle()
    val tabs = remember(config) { MainTab.visibleIn(config) }

    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    // A tab the owner switches off in the panel must not stay selected under the
    // user's finger; the first surviving tab takes over.
    LaunchedEffect(tabs) {
        if (tab !in tabs) tab = tabs.first()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {
                            Icon(imageVector = entry.icon, contentDescription = null)
                        },
                        label = { Text(stringResource(entry.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (tab) {
                MainTab.HOME -> HomeRoute(
                    onOpenCourse = onOpenCourse,
                    onOpenPage = onOpenPage,
                    // Guarded: a hidden courses tab has nowhere to go, so the
                    // hero's CTA stays put instead of selecting a tab that is
                    // not on screen.
                    onBrowseCourses = { if (MainTab.COURSES in tabs) tab = MainTab.COURSES },
                    onOpenPlans = onOpenPlans,
                )

                MainTab.COURSES -> CatalogueRoute(onOpenCourse = onOpenCourse)
                MainTab.SHOP -> ShopRoute(onOpenProduct = onOpenProduct)
                MainTab.ACCOUNT -> AccountRoute(
                    onSignedOut = onSignedOut,
                    onBuyPlan = onBuyPlan,
                    onOpenPlans = onOpenPlans,
                )
            }
        }
    }
}

@Composable
private fun HomeRoute(
    onOpenCourse: (Int) -> Unit,
    onOpenPage: (String) -> Unit,
    onBrowseCourses: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    val viewModel: HomeContentViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        isOffline = isOffline,
        onRetry = viewModel::refresh,
        onOpenCourse = onOpenCourse,
        onOpenPage = onOpenPage,
        onBrowseCourses = onBrowseCourses,
        onOpenPlans = onOpenPlans,
    )
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
    val menu by viewModel.menu.collectAsStateWithLifecycle()
    val path by viewModel.path.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    ShopScreen(
        menu = menu,
        path = path,
        page = page,
        loadingMore = loadingMore,
        isOffline = isOffline,
        onRetry = viewModel::retry,
        onOpen = viewModel::open,
        onBack = viewModel::back,
        onJumpTo = viewModel::jumpTo,
        onLoadMore = viewModel::loadMore,
        onOpenProduct = onOpenProduct,
    )
}

@Composable
private fun AccountRoute(
    onSignedOut: () -> Unit,
    onBuyPlan: (String) -> Unit,
    onOpenPlans: () -> Unit,
) {
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
        onBuyPlan = onBuyPlan,
        onOpenPlans = onOpenPlans,
    )
}
