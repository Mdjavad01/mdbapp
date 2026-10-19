package com.mrdastbaaf.mdbapp.features.shop.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AsyncStateView
import com.mrdastbaaf.mdbapp.core.ui.component.BrandTopAppBar
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct

/**
 * The physical shop.
 *
 * Tapping a product opens the checkout, not the website: the purchase happens
 * inside the app, which is the whole point of the tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    state: UiState<ShopCatalog>,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpenProduct: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = stringResource(R.string.shop_title),
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        AsyncStateView(
            state = state,
            isOffline = isOffline,
            onRetry = onRetry,
            emptyMessage = stringResource(R.string.shop_empty),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) { catalog ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.lg),
            ) {
                if (catalog.featured.isNotEmpty()) {
                    item(key = "featured-title") {
                        SectionTitle(stringResource(R.string.shop_featured))
                    }
                    item(key = "featured-row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            // Positional keys: the bridge can list the same
                            // product twice (featured plus a category), and a
                            // duplicated key is a hard crash, not a warning.
                            itemsIndexed(items = catalog.featured) { index, product ->
                                FeaturedCard(product, onClick = { onOpenProduct(product.id) })
                            }
                        }
                    }
                }

                catalog.categories.forEachIndexed { categoryIndex, category ->
                    item(key = "cat-$categoryIndex") {
                        SectionTitle(category.name)
                    }
                    itemsIndexed(items = category.products) { index, product ->
                        ProductRow(product, onClick = { onOpenProduct(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = Spacing.sm,
        ),
    )
}

@Composable
private fun FeaturedCard(product: ShopProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            ProductImage(
                imageUrl = product.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                corner = 0.dp,
            )

            Column(
                modifier = Modifier.padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = PersianFormat.formatToman(product.priceToman),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: ShopProduct, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            modifier = Modifier.size(72.dp),
            corner = 12.dp,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = PersianFormat.formatToman(product.priceToman),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductImage(imageUrl: String?, modifier: Modifier, corner: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
