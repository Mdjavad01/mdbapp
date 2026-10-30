package com.mrdastbaaf.mdbapp.features.shop.domain

import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCatalog
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopCategory
import com.mrdastbaaf.mdbapp.features.shop.domain.model.ShopProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shop menu is built from a flat list, so these tests cover the shapes a
 * real WooCommerce tree produces — including the broken ones.
 */
class ShopTreeTest {

    private fun product(id: Int) = ShopProduct(
        id = id,
        title = "محصول $id",
        priceToman = id * 1000L,
        imageUrl = null,
    )

    private fun category(id: Int, parent: Int = 0, products: IntRange = IntRange.EMPTY) =
        ShopCategory(
            id = id,
            name = "دسته $id",
            products = products.map { product(it) },
            parentId = parent,
        )

    private fun catalogOf(vararg categories: ShopCategory) =
        ShopCatalog(featured = emptyList(), categories = categories.toList())

    @Test
    fun `children are nested under their parent`() {
        val catalog = catalogOf(
            category(id = 1),
            category(id = 2, parent = 1, products = 10..11),
            category(id = 3, parent = 1, products = 12..12),
        )

        assertEquals(1, catalog.roots.size)
        val root = catalog.roots.first()
        assertEquals(1, root.category.id)
        assertEquals(2, root.children.size)
        assertEquals(listOf(2, 3), root.children.map { it.category.id })
    }

    @Test
    fun `a parent with no products of its own still counts its subtree`() {
        // This is exactly what the bridge sends for a group category: empty
        // products, children holding the goods.
        val catalog = catalogOf(
            category(id = 1),
            category(id = 2, parent = 1, products = 10..12),
            category(id = 3, parent = 1, products = 13..13),
        )

        val root = catalog.roots.first()
        assertTrue(root.isGroup)
        assertEquals(4, root.count)
        assertEquals(listOf(10, 11, 12, 13), root.products.map { it.id })
    }

    @Test
    fun `a subcategory whose parent is missing becomes a root`() {
        // The owner ticked a child but not the parent, so the bridge never sent
        // the parent. The child must still be reachable.
        val catalog = catalogOf(
            category(id = 7, parent = 99, products = 10..10),
        )

        assertEquals(1, catalog.roots.size)
        assertEquals(7, catalog.roots.first().category.id)
    }

    @Test
    fun `an empty category with no children is pruned`() {
        val catalog = catalogOf(
            category(id = 1, products = 10..10),
            category(id = 2),
        )

        assertEquals(1, catalog.roots.size)
        assertEquals(1, catalog.roots.first().category.id)
        assertNull(catalog.node(2))
    }

    @Test
    fun `a group whose whole subtree is empty is pruned too`() {
        val catalog = catalogOf(
            category(id = 1),
            category(id = 2, parent = 1),
            category(id = 3, products = 10..10),
        )

        assertEquals(1, catalog.roots.size)
        assertEquals(3, catalog.roots.first().category.id)
    }

    @Test
    fun `a term that is its own parent is a root`() {
        val catalog = catalogOf(category(id = 5, parent = 5, products = 10..10))

        assertEquals(1, catalog.roots.size)
        assertEquals(5, catalog.roots.first().category.id)
    }

    @Test
    fun `a two-node cycle does not hang and stays reachable`() {
        val catalog = catalogOf(
            category(id = 1, parent = 2, products = 10..10),
            category(id = 2, parent = 1, products = 11..11),
        )

        assertTrue(catalog.roots.isNotEmpty())
        assertNotNull(catalog.node(1))
        assertNotNull(catalog.node(2))
    }

    @Test
    fun `node lookup finds a nested category`() {
        val catalog = catalogOf(
            category(id = 1),
            category(id = 2, parent = 1),
            category(id = 3, parent = 2, products = 10..10),
        )

        assertEquals(3, catalog.node(3)?.category?.id)
        assertEquals(1, catalog.node(3)?.count)
    }

    @Test
    fun `an empty response is empty, a response with products is not`() {
        assertTrue(catalogOf().isEmpty)
        assertTrue(catalogOf(category(id = 1)).isEmpty)
        assertTrue(!catalogOf(category(id = 1, products = 10..10)).isEmpty)
    }
}
