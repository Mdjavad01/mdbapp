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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.mrdastbaaf.mdbapp.core.ui.state.dataOrNull
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCategory
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopNode
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct

/** The "everything" pseudo-section: not a term id, so it can never collide. */
private const val ALL_PRODUCTS_ID = -1

/**
 * The physical shop, as a menu and then a section.
 *
 * It used to be one long scroll: every category and every product on a single
 * page. With two or three categories that is fine; with a real WooCommerce tree
 * it is a wall the user scrolls past. So the tab now opens on a list of
 * categories — the menu — and a category opens its own page, with its
 * subcategories as chips across the top and its products underneath.
 *
 * Both levels live in this composable rather than in two nav destinations: the
 * catalogue is already cached in memory, so switching is instant, and the bottom
 * bar stays where it is instead of the shop pushing a new screen over it.
 * [BackHandler] makes the system back button leave the section before it leaves
 * the tab.
 *
 * Tapping a product opens the checkout, not the website: the purchase happens
 * inside the app, which is the whole point of the tab.
 */
@Composable
fun ShopScreen(
    state: UiState<ShopCatalog>,
    isOffline: Boolean,
    onRetry: () -> Unit,
    onOpenProduct: (Int) -> Unit,
) {
    var openCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    val catalog = state.dataOrNull
    val allLabel = stringResource(R.string.shop_all_products)
    val openNode = openCategoryId?.let { id ->
        when {
            catalog == null -> null
            // A sentinel, not a real term: -1 is the "everything" pseudo-section.
            id == ALL_PRODUCTS_ID -> ShopNode(
                category = ShopCategory(
                    id = ALL_PRODUCTS_ID,
                    name = allLabel,
                    products = catalog.roots.flatMap { it.products }.distinctBy { it.id },
                ),
            )
            else -> catalog.node(id)
        }
    }

    BackHandler(enabled = openNode != null) { openCategoryId = null }

    // Typed explicitly: a bare `if` returning a lambda infers a plain function
    // type, which does not satisfy BrandTopAppBar's @Composable parameter.
    val backIcon: (@Composable () -> Unit)? = if (openNode != null) {
        {
            IconButton(onClick = { openCategoryId = null }) {
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
                title = openNode?.category?.name ?: stringResource(R.string.shop_title),
                navigationIcon = backIcon,
                actions = {
                    if (openNode == null) {
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
        AsyncStateView(
            state = state,
            isOffline = isOffline,
            onRetry = onRetry,
            emptyMessage = stringResource(R.string.shop_empty),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) { loaded ->
            if (openNode != null) {
                CategoryPage(node = openNode, onOpenProduct = onOpenProduct)
            } else {
                ShopMenu(
                    catalog = loaded,
                    onOpenCategory = { id -> openCategoryId = id },
                    onOpenProduct = onOpenProduct,
                )
            }
        }
    }
}

// ── Level 1: the menu ───────────────────────────────────────────────────────

@Composable
private fun ShopMenu(
    catalog: ShopCatalog,
    onOpenCategory: (Int) -> Unit,
    onOpenProduct: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (catalog.featured.isNotEmpty()) {
            item(key = "featured-title") {
                SectionHeader(stringResource(R.string.shop_featured))
            }
            item(key = "featured-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Positional keys: the bridge can list the same product twice
                    // (featured plus a category), and a duplicated key is a hard
                    // crash, not a warning.
                    itemsIndexed(items = catalog.featured) { _, product ->
                        FeaturedCard(product, onClick = { onOpenProduct(product.id) })
                    }
                }
            }
        }

        item(key = "categories-title") {
            SectionHeader(stringResource(R.string.shop_categories))
        }

        // Every category in one tap, before choosing a section. Without this the
        // user has to know which subcategory holds what.
        item(key = "all-products") {
            val total = catalog.roots.sumOf { it.count }
            MenuRow(
                title = stringResource(R.string.shop_all_products),
                count = total,
                onClick = { onOpenCategory(ALL_PRODUCTS_ID) },
                showCount = total > 0,
            )
        }

        itemsIndexed(items = catalog.roots) { _, root ->
            MenuRow(
                title = root.category.name,
                count = root.count,
                onClick = { onOpenCategory(root.category.id) },
                showCount = root.count > 0,
                hasChildren = root.children.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun MenuRow(
    title: String,
    count: Int,
    onClick: () -> Unit,
    showCount: Boolean,
    hasChildren: Boolean = false,
) {
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        showCount -> stringResource(
                            R.string.shop_product_count,
                            PersianFormat.toPersianDigits(count.toString()),
                        )
                        hasChildren -> stringResource(R.string.shop_subcategories)
                        else -> stringResource(R.string.shop_coming_soon)
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
private fun CategoryPage(
    node: ShopNode,
    onOpenProduct: (Int) -> Unit,
) {
    // 0 means "everything in this section". Keyed on the node so opening a
    // different category never inherits the previous chip selection.
    var selectedChildId by rememberSaveable(node.category.id) { mutableStateOf(0) }

    val shown = node.children.firstOrNull { it.category.id == selectedChildId } ?: node
    val products = shown.products

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (node.children.isNotEmpty()) {
            item(key = "chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    item(key = "chip-all") {
                        SectionChip(
                            label = stringResource(R.string.shop_all_chip),
                            selected = selectedChildId == 0,
                            onClick = { selectedChildId = 0 },
                        )
                    }
                    itemsIndexed(items = node.children) { _, child ->
                        SectionChip(
                            label = child.category.name,
                            selected = selectedChildId == child.category.id,
                            onClick = { selectedChildId = child.category.id },
                        )
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.shop_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        } else {
            itemsIndexed(items = products) { _, product ->
                ProductRow(product, onClick = { onOpenProduct(product.id) })
            }
        }
    }
}

@Composable
private fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
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
private fun FeaturedCard(product: ShopProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                product.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // The price sits on the image: it is the first thing a buyer
                // looks for, and the padding below is then free for the title.
                Text(
                    text = PersianFormat.formatToman(product.priceToman),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.sm)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f))
                        .padding(horizontal = Spacing.sm, vertical = 4.dp),
                )
            }

            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(Spacing.sm),
            )
        }
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
