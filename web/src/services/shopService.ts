/**
 * @file services/shopService.ts
 * @description سرویس «محصولات» (v1.3.0 — ویترین + v1.4.0 «محصولات ویژه») — GET /shop/products
 *
 * قرارداد (افزونهٔ 1.5.0):
 *   GET /shop/products (عمومی) → { success:true,
 *     featured: [{ id, title, price, imageUrl|null, url }],   ← NEW در 1.5.0 (قدیمی‌ها: غایب → [])
 *     categories: [{ id, slug, name, products: [{ id, title, price, imageUrl|null, url }] }] }
 *
 * اصول:
 *  - در حالت دمو (بدون VITE_API_BASE_URL) یک نمونهٔ ثابت محلی با ۲ دسته و ۲ محصول
 *    ویژه برگردانده می‌شود تا سکشن صفحهٔ نخست + کاروسل قابل ارزیابی باشند — بدون هیچ
 *    تصویر بیرونی (imageUrl = null → UI کاشی حرف‌اول می‌کشد).
 *  - پاسخ سرور تدافعی پالایش می‌شود؛ ردیف مخدوش/ناقص دور ریخته می‌شود و هرگز
 *    اپ را نمی‌شکند (همان الگوی appContentService/appPagesService).
 */

import { ShopCategory, ShopCategoryNode, ShopFeed, ShopPage, ShopProduct, ShopTreeNode } from '../types';
import { apiFetch, isRealApiMode } from './apiClient';
import { toEnglishDigits } from '../utils/format';

interface ApiShopProductShape {
  id?: unknown;
  title?: unknown;
  price?: unknown;
  imageUrl?: unknown;
  url?: unknown;
}

interface ApiShopCategoryShape {
  id?: unknown;
  slug?: unknown;
  name?: unknown;
  products?: unknown;
}

function shapeString(value: unknown, fallback = ''): string {
  return typeof value === 'string' && value.trim() !== '' ? value : fallback;
}

function shapeId(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? Math.round(value) : fallback;
}

function shapePrice(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value) && value >= 0) return Math.round(value);
  // قیمت رشته‌ای عددی («۱۹۸۰۰۰» / "198000") هم پذیرفته می‌شود تا UI خالی نماند —
  // ارقام فارسی/عربی ابتدا انگلیسی می‌شوند (Number روی ارقام فارسی NaN می‌دهد).
  if (typeof value === 'string') {
    const trimmed = toEnglishDigits(value).trim();
    if (trimmed !== '' && Number.isFinite(Number(trimmed)) && Number(trimmed) >= 0) {
      return Math.round(Number(trimmed));
    }
  }
  return 0;
}

function shapeImageUrl(value: unknown): string | null {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null;
}

/** پالایش یک محصول — ردیف بدون عنوان/لینک معتبر دور ریخته می‌شود (url برای CTA الزامی است). */
export function normalizeShopProduct(raw: unknown, index: number): ShopProduct | null {
  const item = raw && typeof raw === 'object' ? (raw as ApiShopProductShape) : {};
  const title = shapeString(item.title);
  const url = shapeString(item.url);
  if (title === '' || url === '') return null;
  return {
    id: shapeId(item.id, index + 1),
    title,
    price: shapePrice(item.price),
    imageUrl: shapeImageUrl(item.imageUrl),
    url,
  };
}

/**
 * پالایش و گروه‌بندی پاسخ /shop/products — دسته‌های بدون نام یا بدون محصول
 * معتبر دور ریخته می‌شوند؛ ترتیب پاسخ سرور حفظ می‌شود.
 */
export function normalizeShopProducts(raw: unknown): ShopCategory[] {
  const data = raw && typeof raw === 'object' ? (raw as { categories?: unknown }) : null;
  const list = Array.isArray(data?.categories)
    ? (data?.categories as unknown[])
    : Array.isArray(raw)
      ? (raw as unknown[])
      : [];

  const out: ShopCategory[] = [];
  list.forEach((entry, catIndex) => {
    const cat = entry && typeof entry === 'object' ? (entry as ApiShopCategoryShape) : {};
    const name = shapeString(cat.name);
    if (name === '') return;
    const products = Array.isArray(cat.products)
      ? (cat.products as unknown[])
          .map((p, i) => normalizeShopProduct(p, i))
          .filter((p): p is ShopProduct => p !== null)
      : [];
    if (products.length === 0) return;
    out.push({
      id: shapeId(cat.id, catIndex + 1),
      slug: shapeString(cat.slug, `shop-cat-${catIndex + 1}`),
      name,
      products,
    });
  });
  return out;
}

/**
 * v1.4.0 — پالایش فهرست «محصولات ویژه» سطح‌بالای پاسخ (هم‌شکل آیتم محصولات دسته‌ها).
 * غایب/مخدوش روی سرور قدیمی → [] (هرگز اپ را نمی‌شکند).
 */
export function normalizeShopFeatured(raw: unknown): ShopProduct[] {
  const data = raw && typeof raw === 'object' ? (raw as { featured?: unknown }) : null;
  if (!Array.isArray(data?.featured)) return [];
  return (data?.featured as unknown[])
    .map((p, i) => normalizeShopProduct(p, i))
    .filter((p): p is ShopProduct => p !== null);
}

/**
 * v1.4.0 — خوراک کامل فروشگاه: {featured, categories} (نرمال‌سازها مشترک‌اند تا
 * شکل آیتم‌ها دقیقاً یکی بماند).
 */
export function normalizeShopFeed(raw: unknown): ShopFeed {
  return {
    featured: normalizeShopFeatured(raw),
    categories: normalizeShopProducts(raw),
  };
}

// ---------------------------------------------------------------------------
// v1.8.0 — فروشگاه مقیاس‌پذیر: /shop/categories + /shop/catalog
//
// چرا مسیر دوم: /shop/products همهٔ محصولات را در یک پاسخ می‌فرستد (۱۸۰۰
// محصول ≈ ۶۰۰KB). سایت همان مدل اپ را می‌گیرد — منو، بعد یک صفحهٔ ۲۴تایی —
// و انتخاب محصولات هم به ووکامرس سپرده می‌شود (منتشرشده و در کاتالوگ پنهان
// نباشد)، پس محصول تازه بدون تیک‌زدن دستی در سایت هم دیده می‌شود.
// ---------------------------------------------------------------------------

/** اندازهٔ صفحهٔ کاتالوگ — همان ۲۴ اپ و پیش‌فرض افزونه. */
export const SHOP_PAGE_SIZE = 24;

/** برچسب‌های HTML از متن پنل می‌آیند؛ منو نباید `<b>` نشان دهد. */
function stripHtml(value: string): string {
  return value
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/\s+/g, ' ')
    .trim();
}

interface ApiShopCategoryNodeShape {
  id?: unknown;
  slug?: unknown;
  name?: unknown;
  parent?: unknown;
  count?: unknown;
}

function shapeCount(value: unknown): number {
  return shapePrice(value);
}

/**
 * پالایش فهرست تخت دسته‌ها — ردیف بدون شناسه یا بدون نام دور ریخته می‌شود.
 * نام تکراری حذف نمی‌شود (دو دستهٔ هم‌نام در ووکامرس ممکن است)، فقط شناسهٔ
 * تکراری.
 */
export function normalizeShopCategories(raw: unknown): ShopCategoryNode[] {
  const data = raw && typeof raw === 'object' ? (raw as { categories?: unknown }) : null;
  const list = Array.isArray(data?.categories)
    ? (data?.categories as unknown[])
    : Array.isArray(raw)
      ? (raw as unknown[])
      : [];

  const out: ShopCategoryNode[] = [];
  const seen = new Set<number>();
  list.forEach((entry) => {
    const row = entry && typeof entry === 'object' ? (entry as ApiShopCategoryNodeShape) : {};
    const id = shapeId(row.id, 0);
    if (id <= 0 || seen.has(id)) return;
    const name = stripHtml(shapeString(row.name));
    if (name === '') return;
    seen.add(id);
    out.push({
      id,
      slug: shapeString(row.slug, `shop-cat-${id}`),
      name,
      parent: shapeId(row.parent, 0),
      count: shapeCount(row.count),
    });
  });
  return out;
}

/**
 * فهرست تخت → جنگل. همان سه حالت غیرعادی که در دادهٔ واقعی ووکامرس هست و در
 * اپ هم پوشش داده شده: بدون والد، والدِ غایب از پاسخ، و ترمی که والد خودش است.
 * دستهٔ دست‌نیافتنی (چرخهٔ دوگانهٔ خراب) به ریشه ارتقا می‌یابد، نه حذف —
 * دسته‌ای که ادمین ساخته نباید نامرئی شود.
 */
export function buildShopTree(categories: ShopCategoryNode[]): ShopTreeNode[] {
  const known = new Map<number, ShopCategoryNode>();
  categories.forEach((c) => {
    if (c.id > 0) known.set(c.id, c);
  });

  const makeNode = (category: ShopCategoryNode, children: ShopTreeNode[]): ShopTreeNode => ({
    category,
    children,
    // منو محصولی ندارد، پس شمارش فقط از سرور می‌آید (pad_counts شامل زیرشاخه‌ها).
    count: Math.max(category.count, 0),
    isGroup: children.length > 0,
  });

  const childrenOf = (parentId: number, depth: number): ShopTreeNode[] => {
    if (depth > 8) return [];
    return categories
      .filter((c) => c.id !== parentId && c.parent === parentId)
      .map((c) => makeNode(c, childrenOf(c.id, depth + 1)));
  };

  const roots = categories
    .filter((c) => c.parent === 0 || c.parent === c.id || !known.has(c.parent))
    .map((c) => makeNode(c, childrenOf(c.id, 1)));

  const reachable = new Set<number>();
  const walk = (nodes: ShopTreeNode[]): void => {
    nodes.forEach((node) => {
      reachable.add(node.category.id);
      walk(node.children);
    });
  };
  walk(roots);

  const orphans = categories
    .filter((c) => !reachable.has(c.id))
    .map((c) => makeNode(c, childrenOf(c.id, 1)));

  // دستهٔ بی‌محصول در منو فقط شلوغی است؛ بعد از ساخت درخت داوری می‌شود تا والد
  // بر اساس زیرشاخه‌اش سنجیده شود، نه بر اساس فهرست خودش.
  const prune = (nodes: ShopTreeNode[]): ShopTreeNode[] =>
    nodes.flatMap((node) => {
      const children = prune(node.children);
      if (node.count === 0 && children.length === 0) return [];
      return [{ ...node, children, isGroup: children.length > 0 }];
    });

  return prune([...roots, ...orphans]);
}

/** مسیر ریشه → گره، برای لینک عمیق `#/shop?cat=…` و بردکرامب. */
export function findShopPath(tree: ShopTreeNode[], id: number): ShopTreeNode[] {
  const walk = (nodes: ShopTreeNode[], trail: ShopTreeNode[]): ShopTreeNode[] | null => {
    for (const node of nodes) {
      const next = [...trail, node];
      if (node.category.id === id) return next;
      const found = walk(node.children, next);
      if (found) return found;
    }
    return null;
  };
  return walk(tree, []) ?? [];
}

/**
 * یک محصول از /shop/catalog. برخلاف ریل «محصولات ویژه»، لینک سایت الزامی
 * نیست: خرید درون‌اپ به آن نیاز ندارد و حذف ردیف فقط برای یک لینک خالی،
 * محصولی را از فروشگاه ناپدید می‌کند.
 */
function normalizeCatalogProduct(raw: unknown, index: number): ShopProduct | null {
  const item = raw && typeof raw === 'object' ? (raw as ApiShopProductShape) : {};
  const title = stripHtml(shapeString(item.title));
  if (title === '') return null;
  return {
    id: shapeId(item.id, index + 1),
    title,
    price: shapePrice(item.price),
    imageUrl: shapeImageUrl(item.imageUrl),
    url: shapeString(item.url),
  };
}

/** پالایش پاسخ یک صفحه — اعداد سرور مبنای صفحه‌بندی‌اند، نه حدس کلاینت. */
export function normalizeShopPage(raw: unknown, fallbackCategoryId: number): ShopPage {
  const data = raw && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
  const categoryRow =
    data.category && typeof data.category === 'object'
      ? (data.category as ApiShopCategoryNodeShape)
      : null;

  const products = Array.isArray(data.products)
    ? (data.products as unknown[])
        .map((p, i) => normalizeCatalogProduct(p, i))
        .filter((p): p is ShopProduct => p !== null)
    : [];

  const total = shapeCount(data.total);
  const perPage = shapeId(data.perPage, SHOP_PAGE_SIZE);
  const page = shapeId(data.page, 1);

  return {
    categoryId: categoryRow ? shapeId(categoryRow.id, fallbackCategoryId) : fallbackCategoryId,
    categoryName: categoryRow ? stripHtml(shapeString(categoryRow.name)) : '',
    products,
    page,
    perPage,
    total: total > 0 ? total : products.length,
    totalPages: shapeCount(data.totalPages),
  };
}

/** نمونهٔ ثابت دمو — ۲ دسته با کاشی حرف‌اول (بدون تصویر بیرونی؛ دمو صادقانه) */const DEMO_SHOP_CATEGORIES: ShopCategory[] = [
  {
    id: 1,
    slug: 'handwoven-rugs',
    name: 'قالی و گلیم دستباف',
    products: [
      { id: 101, title: 'قالی تبریزی طرح لچک ترنج (۹۰×۶۰)', price: 4850000, imageUrl: null, url: 'https://mrdastbaaf.com/shop' },
      { id: 102, title: 'گلیم سفال‌رنگ بافت اردبیل (۱۲۰×۸۰)', price: 3200000, imageUrl: null, url: 'https://mrdastbaaf.com/shop' },
      { id: 103, title: 'تابلوفرش دستباف طرح ترنج (رودوزی)', price: 2650000, imageUrl: null, url: 'https://mrdastbaaf.com/shop' },
    ],
  },
  {
    id: 2,
    slug: 'accessories',
    name: 'کیف و زیورآلات صنایع دستی',
    products: [
      { id: 201, title: 'کیف دست‌دوز ترمه (جیبی)', price: 890000, imageUrl: null, url: 'https://mrdastbaaf.com/shop' },
      { id: 202, title: 'زیورآلات مکرمه با سنگ‌های نیمه‌قیمتی', price: 640000, imageUrl: null, url: 'https://mrdastbaaf.com/shop' },
    ],
  },
];

// v1.4.0: دمو — ۲ محصول ویژه (مرجع به همان آیتم‌های دسته‌ها؛ بدون دادهٔ تکراری)
const DEMO_FEATURED: ShopProduct[] = [
  DEMO_SHOP_CATEGORIES[0].products[0],
  DEMO_SHOP_CATEGORIES[1].products[0],
];

// v1.8.0: دمو — همان دسته‌های بالا به شکل درختِ /shop/categories (بدون فرزند،
// شمارش از تعداد محصولات) تا صفحهٔ فروشگاه در حالت دمو هم قابل ارزیابی باشد.
const DEMO_CATEGORY_NODES: ShopCategoryNode[] = DEMO_SHOP_CATEGORIES.map((cat) => ({
  id: cat.id,
  slug: cat.slug,
  name: cat.name,
  parent: 0,
  count: cat.products.length,
}));

/** دمو — یک صفحه از یک دسته، با همان قاعدهٔ ۲۴تایی سرور. */
function demoCatalogPage(categoryId: number, page: number, perPage: number): ShopPage {
  const source = DEMO_SHOP_CATEGORIES.find((c) => c.id === categoryId);
  const all = source ? source.products : DEMO_SHOP_CATEGORIES.flatMap((c) => c.products);
  const start = (page - 1) * perPage;
  const products = all.slice(start, start + perPage);
  return {
    categoryId,
    categoryName: source?.name ?? 'همهٔ محصولات',
    products,
    page,
    perPage,
    total: all.length,
    totalPages: Math.max(1, Math.ceil(all.length / perPage)),
  };
}

export const shopService = {
  /**
   * خوراک «محصولات» (دسته‌ها + ویژه‌ها) — در دمو نمونهٔ ثابت، در Real از سرور
   * (پیشخوان ← مستر دستباف ← محصولات فیزیکی). خطای شبکه بالا فرستاده می‌شود
   * تا HomeView رفتار تحمل‌پذیر خودش (سکشن مخفی) را اعمال کند.
   *
   * v1.8.0: فقط ریل «محصولات ویژه» صفحهٔ نخست از این مسیر می‌آید؛ خودِ
   * فروشگاه از /shop/categories + /shop/catalog می‌خواند.
   */
  async fetchShopProducts(): Promise<ShopFeed> {
    if (!isRealApiMode) {
      // دمو: تأخیر کوتاه هم‌سبک بقیهٔ سرویس‌ها
      await new Promise((res) => setTimeout(res, 200));
      return { featured: DEMO_FEATURED, categories: DEMO_SHOP_CATEGORIES };
    }
    const data = await apiFetch<unknown>('/shop/products');
    return normalizeShopFeed(data);
  },

  /**
   * v1.8.0 — درخت دسته‌ها، بدون هیچ محصولی (پاسخ < ۲KB، ده دقیقه کش سرور).
   * خطا بالا می‌رود تا ShopView کارت خطا با «تلاش مجدد» نشان دهد.
   */
  async fetchCategories(): Promise<ShopCategoryNode[]> {
    if (!isRealApiMode) {
      await new Promise((res) => setTimeout(res, 150));
      return DEMO_CATEGORY_NODES;
    }
    const data = await apiFetch<unknown>('/shop/categories');
    return normalizeShopCategories(data);
  },

  /**
   * v1.8.0 — یک صفحه از یک دسته (۲۴ ردیف). `categoryId = 0` یعنی همهٔ
   * محصولات؛ برای سازگاری با قرارداد افزونه نگه داشته شده.
   */
  async fetchCatalog(
    categoryId: number,
    page = 1,
    perPage = SHOP_PAGE_SIZE
  ): Promise<ShopPage> {
    const safePage = Number.isFinite(page) && page > 0 ? Math.floor(page) : 1;
    if (!isRealApiMode) {
      await new Promise((res) => setTimeout(res, 180));
      return demoCatalogPage(categoryId, safePage, perPage);
    }
    const query = new URLSearchParams({
      category: String(Math.max(0, Math.floor(categoryId))),
      page: String(safePage),
      per_page: String(perPage),
    });
    const data = await apiFetch<unknown>(`/shop/catalog?${query.toString()}`);
    return normalizeShopPage(data, categoryId);
  },
};
