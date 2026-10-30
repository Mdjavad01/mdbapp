package com.mrdastbaaf.mdbapp.features.shop.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopNode
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopPage
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct

/**
 * The shop, as a menu and then a section.
 *
 * The catalogue has ten categories and 1800 products, so nothing is loaded up
 * front: the tab opens on a tree of categories, and a category asks the server
 * for one page of twenty-four at a time.
 *
 * The path is a list rather than a navigation destination. The shop never pushes
 * a screen over the tab — the bottom bar stays put — and going back is a pop off
 * that list, which also gives the breadcrumb something to point at.
 */
@Composable
fun ShopScreen(
    menu: UiState<List<ShopNode>>,
    path: List<ShopNode>,
    page: UiState<ShopPage>,
    loadingMore: Boolean,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpen: (ShopNode) -> Unit,
    onBack: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onOpenProduct: (Int) -> Unit,
) {
    val current = path.lastOrNull()

    // Back leaves the section before it leaves the tab.
    BackHandler(enabled = current != null) { onBack() }

    // Typed explicitly: a bare `if` returning a lambda infers a plain function
    // type, which does not satisfy the @Composable parameter.
    val backIcon: (@Composable () -> Unit)? = if (current != null) {
        {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        }
    } else {
        null
    }

    Scaffold(
        topBar = {
            BrandTopAppBar(
                title = current?.category?.name ?: stringResource(R.string.shop_title),
                navigationIcon = backIcon,
                actions = {
                    if (current == null) {
                        IconButton(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_retry),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            if (current == null) {
                ShopMenu(menu = menu, isOffline = isOffline, onRetry = onRetry, onOpen = onOpen)
            } else {
                SectionPage(
                    path = path,
                    current = current,
                    page = page,
                    loadingMore = loadingMore,
                    isOffline = isOffline,
                    onRetry = onRetry,
                    onOpen = onOpen,
                    onJumpTo = onJumpTo,
                    onLoadMore = onLoadMore,
                    onOpenProduct = onOpenProduct,
                )
            }
        }
    }
}

// ── Level 1: the menu ───────────────────────────────────────────────────────

@Composable
private fun ShopMenu(
    menu: UiState<List<ShopNode>>,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpen: (ShopNode) -> Unit,
) {
    AsyncStateView(
        state = menu,
        isOffline = isOffline,
        onRetry = onRetry,
        emptyMessage = stringResource(R.string.shop_empty),
        modifier = Modifier.fillMaxSize(),
    ) { roots ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "title") { SectionHeader(stringResource(R.string.shop_categories)) }

            itemsIndexed(items = roots) { _, node ->
                MenuRow(node = node, onClick = { onOpen(node) })
            }
        }
    }
}

@Composable
private fun MenuRow(node: ShopNode, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm + Spacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = node.category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (node.children.isNotEmpty()) {
                        stringResource(
                            R.string.shop_subcategories_count,
                            PersianFormat.toPersianDigits(node.children.size.toString()),
                            PersianFormat.toPersianDigits(node.count.toString()),
                        )
                    } else {
                        stringResource(
                            R.string.shop_product_count,
                            PersianFormat.toPersianDigits(node.count.toString()),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

// ── Level 2: one section ────────────────────────────────────────────────────

@Composable
private fun SectionPage(
    path: List<ShopNode>,
    current: ShopNode,
    page: UiState<ShopPage>,
    loadingMore: Boolean,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpen: (ShopNode) -> Unit,
    onJumpTo: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onOpenProduct: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // «مواد اولیه › نخ و نقشه ها» — a three-level shop needs the path, or
        // the user cannot tell where they are or how to get back up.
        Breadcrumb(path = path, onJumpTo = onJumpTo)

        if (current.children.isNotEmpty()) {
            ChildChips(children = current.children, onOpen = onOpen)
        }

        AsyncStateView(
            state = page,
            isOffline = isOffline,
            onRetry = onRetry,
            emptyMessage = stringResource(R.string.shop_empty),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { data ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = Spacing.xs, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(items = data.products) { _, product ->
                    ProductRow(product = product, onClick = { onOpenProduct(product.id) })
                }

                if (data.hasMore) {
                    item(key = "more") {
                        MoreRow(isLoading = loadingMore, onClick = onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(path: List<ShopNode>, onJumpTo: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(items = path) { index, node ->
            val isLast = index == path.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLast) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isLast) { onJumpTo(index) }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                )
                if (!isLast) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildChips(children: List<ShopNode>, onOpen: (ShopNode) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        itemsIndexed(items = children) { _, child ->
            FilterChip(
                selected = false,
                onClick = { onOpen(child) },
                label = {
                    Text(
                        text = "${child.category.name} (${PersianFormat.toPersianDigits(child.count.toString())})",
                        maxLines = 1,
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun MoreRow(isLoading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = onClick, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = Spacing.sm)
                        .size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(stringResource(R.string.shop_more))
        }
    }
}

// ── Shared pieces ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.md,
            bottom = Spacing.xs,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProductRow(product: ShopProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                product.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = PersianFormat.formatToman(product.priceToman),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
