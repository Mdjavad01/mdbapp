package com.mrdastbaaf.mdbapp.features.shop.domain.model

/** A physical product. `id` is the WooCommerce product id. */
data class ShopProduct(
    val id: Int,
    val title: String,
    val priceToman: Long,
    val imageUrl: String?,
)

/**
 * One WooCommerce category.
 *
 * [parentId] is 0 for a root. The bridge sends every category flat, including
 * the ancestors of a ticked category even when those hold no ticked products of
 * their own — those arrive with an empty [products] list and exist purely to
 * group their children.
 */
data class ShopCategory(
    val id: Int,
    val name: String,
    val products: List<ShopProduct>,
    val parentId: Int = 0,
    /**
     * How many products the category holds, as the server counts them.
     *
     * The menu is built from `/shop/categories`, which carries no products at
     * all — so the count is the only thing that tells a real category from an
     * empty one.
     */
    val count: Int = 0,
)

/**
 * A category with its children resolved.
 *
 * [products] is the whole subtree, not just the node's own list: tapping
 * «دست‌ساز» has to show what is inside its subcategories, otherwise the parent
 * looks empty and the user has to guess which chip holds the goods.
 */
data class ShopNode(
    val category: ShopCategory,
    val children: List<ShopNode> = emptyList(),
) {
    val products: List<ShopProduct> =
        category.products + children.flatMap { it.products }

    /** Loaded products if any, otherwise the number the server reported. */
    val count: Int get() = maxOf(products.size, category.count)

    /** True when this node only exists to hold children. */
    val isGroup: Boolean get() = category.products.isEmpty() && children.isNotEmpty()
}

data class ShopCatalog(
    val featured: List<ShopProduct>,
    val categories: List<ShopCategory>,
) {
    /** Built once per response — [ShopNode.products] walks the subtree. */
    val roots: List<ShopNode> = buildShopTree(categories)

    private val index: Map<Int, ShopNode> =
        roots.flatMap { it.flatten() }.associateBy { it.category.id }

    fun node(id: Int): ShopNode? = index[id]

    val isEmpty: Boolean
        get() = featured.isEmpty() && roots.isEmpty() && categories.all { it.products.isEmpty() }
}

/**
 * The shop menu: categories only, no products.
 *
 * Same forest builder as the catalogue — a parent that is missing from the
 * payload, a term that is its own parent and a two-node cycle are handled there
 * — but the nodes carry no products, so the count comes from the server.
 */
fun buildShopMenu(categories: List<ShopCategory>): List<ShopNode> = buildShopTree(categories)

/** One page of one section, as the app renders it. */
data class ShopPage(
    val categoryId: Int,
    val categoryName: String,
    val products: List<ShopProduct>,
    val page: Int,
    val totalPages: Int,
    val total: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

private fun ShopNode.flatten(): List<ShopNode> = listOf(this) + children.flatMap { it.flatten() }

/**
 * Turns the flat list into a forest.
 *
 * Three cases are treated as a root, and all three happen in real WooCommerce
 * data: no parent, a parent that is not in the response (its products were
 * never ticked, so the bridge did not send it), and a term whose parent is
 * itself. Anything still unreachable afterwards — a corrupt two-node cycle — is
 * promoted to a root rather than dropped, because a category the owner ticked
 * must never be invisible.
 */
private fun buildShopTree(categories: List<ShopCategory>): List<ShopNode> {
    val known = categories.filter { it.id > 0 }.associateBy { it.id }

    fun childrenOf(parentId: Int, depth: Int): List<ShopNode> {
        if (depth > 8) return emptyList()
        return categories
            .filter { it.id != parentId && it.parentId == parentId }
            .map { child -> ShopNode(child, childrenOf(child.id, depth + 1)) }
    }

    val roots = categories
        .filter { it.parentId == 0 || it.parentId == it.id || known[it.parentId] == null }
        .map { root -> ShopNode(root, childrenOf(root.id, 1)) }

    val reachable = roots.flatMap { it.flatten() }.map { it.category.id }.toSet()
    val orphans = categories
        .filter { it.id !in reachable }
        .map { orphan -> ShopNode(orphan, childrenOf(orphan.id, 1)) }

    // A node with no products anywhere below it is noise in a menu: the owner
    // ticked nothing in it, or everything in it was unpublished. Pruned after
    // the tree is built, so a parent is judged by its subtree rather than by
    // its own list.
    return pruneEmpty(roots + orphans)
}

private fun pruneEmpty(nodes: List<ShopNode>): List<ShopNode> = nodes.mapNotNull { node ->
    val children = pruneEmpty(node.children)
    val pruned = ShopNode(node.category, children)
    if (pruned.count == 0 && children.isEmpty()) null else pruned
}

/** What the checkout form collects. The server stores it with the order. */
data class ShippingDetails(
    val fullName: String,
    val phone: String,
    val address: String,
    val postalCode: String,
)

/** An order that exists server-side and is waiting for payment. */
data class CheckoutSession(
    val orderId: String,
    val orderNumber: String,
    val trackId: Long,
    val paymentUrl: String,
)
