package com.mrdastbaaf.mdbapp.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandGradient

/**
 * The app bar for the main tabs: the brand gradient with white content.
 *
 * Written as a component rather than per-screen `TopAppBar` colours because the
 * bar is the one place the brand shows on every screen, and a `containerColor`
 * that is transparent inside a gradient box cannot be repainted by Material's
 * surface tinting — which is exactly what made the plain primary bar read as
 * pale pink next to the ruby hero.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrandGradient),
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { navigationIcon?.invoke() },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )
    }
}
