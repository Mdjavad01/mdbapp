/**
 * @file tests/web/shopCatalog.test.ts
 * @description v1.8.0 — منطق درخت فروشگاه و پالایش پاسخ‌های /shop/categories و /shop/catalog
 *
 * چرا تست: ساخت درخت سه حالت غیرعادی دادهٔ واقعی ووکامرس را مدیریت می‌کند
 * (والدِ غایب، ترمی که والد خودش است، چرخهٔ دوگانه) و یک باگ در آن یعنی یک
 * دستهٔ ساختهٔ ادمین که در سایت نامرئی است — چیزی که در بیلد و تایپ‌چک دیده
 * نمی‌شود. همان موارد ShopTreeTest اندروید، تا دو کلاینت یک رفتار داشته باشند.
 *
 * محیط تست node است؛ shopService → apiClient به localStorage/window دست
 * می‌زند، پس پیش از import شیم می‌شود (همان الگوی v140Contracts).
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ShopCategoryNode } from '../../src/types';

let storage: {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
};

beforeEach(() => {
  vi.resetModules();
  const store = new Map<string, string>();
  storage = {
    getItem: (key) => (store.has(key) ? (store.get(key) as string) : null),
    setItem: (key, value) => void store.set(key, String(value)),
    removeItem: (key) => void store.delete(key),
  };
  vi.stubGlobal('localStorage', storage);
  vi.stubGlobal('window', {
    localStorage: storage,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

/** import داخل هر تست — بعد از شیم‌ها. */
const load = () => import('../../src/services/shopService');

const cat = (id: number, parent: number, count = 1, name = `دسته ${id}`): ShopCategoryNode => ({
  id,
  slug: `cat-${id}`,
  name,
  parent,
  count,
});

describe('normalizeShopCategories', () => {
  it('keeps id, parent and count and drops rows without an id or a name', async () => {
    const { normalizeShopCategories } = await load();

    const rows = normalizeShopCategories({
      success: true,
      categories: [
        { id: 34, slug: 'yarn', name: 'نخ و نقشه', parent: 0, count: 120 },
        { id: 0, name: 'بی‌شناسه', parent: 0, count: 3 },
        { id: 35, name: '   ', parent: 34, count: 2 },
        { id: 36, slug: 'loom', name: '<b>دار</b>', parent: 34, count: '7' },
      ],
    });

    expect(rows.map((r) => r.id)).toEqual([34, 36]);
    // HTML پنل در منو نباید دیده شود.
    expect(rows[1].name).toBe('دار');
    expect(rows[1].parent).toBe(34);
    // شمارش رشته‌ای عددی هم پذیرفته می‌شود.
    expect(rows[1].count).toBe(7);
  });

  it('survives a missing or malformed payload', async () => {
    const { normalizeShopCategories } = await load();

    expect(normalizeShopCategories(null)).toEqual([]);
    expect(normalizeShopCategories({ success: false })).toEqual([]);
    expect(normalizeShopCategories('nonsense')).toEqual([]);
  });
});

describe('buildShopTree', () => {
  it('nests children under their parent and marks groups', async () => {
    const { buildShopTree } = await load();

    const tree = buildShopTree([cat(1, 0), cat(2, 1), cat(3, 2)]);

    expect(tree).toHaveLength(1);
    expect(tree[0].isGroup).toBe(true);
    expect(tree[0].children[0].category.id).toBe(2);
    expect(tree[0].children[0].children[0].category.id).toBe(3);
  });

  it('promotes a category whose parent is not in the payload', async () => {
    const { buildShopTree } = await load();

    // والد تیک نخورده بود و افزونه آن را نفرستاده — دسته نباید گم شود.
    const tree = buildShopTree([cat(5, 99)]);

    expect(tree).toHaveLength(1);
    expect(tree[0].category.id).toBe(5);
  });

  it('treats a term that is its own parent as a root', async () => {
    const { buildShopTree } = await load();

    const tree = buildShopTree([cat(7, 7)]);

    expect(tree).toHaveLength(1);
    expect(tree[0].category.id).toBe(7);
    expect(tree[0].children).toHaveLength(0);
  });

  it('rescues a two-node cycle instead of dropping it', async () => {
    const { buildShopTree } = await load();

    // 8 ↔ 9: هر کدام والد دیگری. هیچ‌کدام ریشه نیست، پس هر دو باید ارتقا یابند.
    const tree = buildShopTree([cat(8, 9), cat(9, 8)]);

    expect(tree.map((n) => n.category.id).sort()).toEqual([8, 9]);
  });

  it('prunes a category with no products and no children', async () => {
    const { buildShopTree } = await load();

    const tree = buildShopTree([cat(1, 0, 10), cat(2, 1, 0)]);

    expect(tree).toHaveLength(1);
    // فرزند خالی حذف شده، ولی والد با شمارش خودش می‌ماند.
    expect(tree[0].children).toHaveLength(0);
  });

  it('keeps a group whose own count is zero but whose children hold products', async () => {
    const { buildShopTree } = await load();

    const tree = buildShopTree([cat(1, 0, 0), cat(2, 1, 5)]);

    expect(tree).toHaveLength(1);
    expect(tree[0].children[0].category.id).toBe(2);
  });

  it('returns an empty menu for an empty payload', async () => {
    const { buildShopTree } = await load();

    expect(buildShopTree([])).toEqual([]);
  });
});

describe('findShopPath', () => {
  it('returns the trail from the root to the node', async () => {
    const { buildShopTree, findShopPath } = await load();

    const tree = buildShopTree([cat(1, 0), cat(2, 1), cat(3, 2)]);

    expect(findShopPath(tree, 3).map((n) => n.category.id)).toEqual([1, 2, 3]);
  });

  it('returns an empty trail for an id that is not in the tree', async () => {
    const { buildShopTree, findShopPath } = await load();

    const tree = buildShopTree([cat(1, 0)]);

    expect(findShopPath(tree, 404)).toEqual([]);
  });
});

describe('normalizeShopPage', () => {
  it('reads the server paging numbers and keeps a product without a site link', async () => {
    const { normalizeShopPage } = await load();

    const page = normalizeShopPage(
      {
        success: true,
        category: { id: 34, name: 'نخ و نقشه' },
        page: 2,
        perPage: 24,
        total: 130,
        totalPages: 6,
        products: [
          { id: 900, title: 'نخ پشم <i>دست‌ریس</i>', price: 250000, imageUrl: null, url: '' },
          { id: 901, price: 0 },
          { id: 902, title: '', price: 1000, url: 'https://mrdastbaaf.com/p/902' },
        ],
      },
      34
    );

    expect(page.categoryId).toBe(34);
    expect(page.categoryName).toBe('نخ و نقشه');
    expect(page.page).toBe(2);
    expect(page.perPage).toBe(24);
    expect(page.totalPages).toBe(6);
    expect(page.total).toBe(130);
    // ردیف بدون عنوان دور ریخته شد؛ ردیف بدون لینک ماند (خرید درون‌اپ).
    expect(page.products).toHaveLength(1);
    expect(page.products[0].title).toBe('نخ پشم دست‌ریس');
    expect(page.products[0].url).toBe('');
  });

  it('falls back to the requested category when the server echoes nothing', async () => {
    const { normalizeShopPage } = await load();

    const page = normalizeShopPage({ success: true, products: [] }, 12);

    expect(page.categoryId).toBe(12);
    expect(page.total).toBe(0);
    expect(page.products).toEqual([]);
  });
});
