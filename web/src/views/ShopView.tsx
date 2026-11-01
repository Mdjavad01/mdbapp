/**
 * @file views/ShopView.tsx
 * @description v1.8.0 — فروشگاه دسته‌محور: منو ← دسته ← زیردسته، ۲۴ محصول در هر صفحه
 *
 * چرا بازنویسی: نسخهٔ قبلی فقط ریل «محصولات ویژه» صفحهٔ نخست بود و از
 * /shop/products می‌خواند — فهرست دستیِ تیک‌خورده، همهٔ محصولات در یک پاسخ.
 * نتیجه: محصول تازهٔ ووکامرس در اپ دیده می‌شد و در سایت نه.
 *
 * اکنون همان مدل اپ اندروید: GET /shop/categories درخت را می‌دهد (بدون محصول،
 * < ۲KB) و GET /shop/catalog یک صفحه از یک دسته. انتخاب محصولات سمت سرور است
 * (منتشرشده و در کاتالوگ پنهان نباشد)، پس هیچ محصولی نیاز به تیک دستی ندارد.
 *
 * مسیر در state است، نه در روتر: یک لینک عمیق `#/shop?cat=34` هم پشتیبانی
 * می‌شود (درخت که رسید، مسیر از شناسه بازسازی می‌شود) و دکمهٔ برگشت مرورگر
 * همان مسیر را می‌بندد، نه کل صفحه را.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ExternalLink, PackageSearch, ShoppingBag } from 'lucide-react';
import { ShopPage, ShopProduct, ShopTreeNode } from '../types';
import { buildShopTree, findShopPath, SHOP_PAGE_SIZE, shopService } from '../services/shopService';
import { formatToman } from '../utils/format';
import { AsyncState, AsyncStatus } from '../components/common/AsyncState';
import { useOnlineRefetch } from '../hooks/useOnlineRefetch';

interface ShopViewProps {
  /** لینک عمیق `#/shop?cat=…` — 0 یعنی منو */
  initialCategoryId?: number;
  onNavigate: (view: string) => void;
  /** خرید درون‌اپ — همان مودال پرداخت صفحهٔ نخست */
  onBuyProduct: (product: ShopProduct) => void;
}

const faNum = (n: number) => n.toLocaleString('fa-IR');

export const ShopView: React.FC<ShopViewProps> = ({
  initialCategoryId = 0,
  onNavigate,
  onBuyProduct,
}) => {
  const [tree, setTree] = useState<ShopTreeNode[]>([]);
  const [treeStatus, setTreeStatus] = useState<AsyncStatus>('loading');
  const [treeTick, setTreeTick] = useState(0);

  const [path, setPath] = useState<ShopTreeNode[]>([]);

  const [page, setPage] = useState<ShopPage | null>(null);
  const [pageStatus, setPageStatus] = useState<AsyncStatus>('loading');
  const [pageTick, setPageTick] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);

  // پاسخ دیررس نباید روی صفحهٔ جدید بنشیند (همان گارد CatalogueView).
  const pageSeqRef = useRef(0);
  const current = path.length > 0 ? path[path.length - 1] : null;

  // ── درخت دسته‌ها ─────────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false;
    setTreeStatus('loading');
    shopService
      .fetchCategories()
      .then((rows) => {
        if (cancelled) return;
        const built = buildShopTree(rows);
        setTree(built);
        setTreeStatus(built.length === 0 ? 'empty' : 'success');
      })
      .catch(() => {
        if (cancelled) return;
        setTreeStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, [treeTick]);

  // لینک عمیق: درخت که رسید، مسیر را از شناسه بازسازی می‌کنیم — یک بار.
  const deepLinkApplied = useRef(false);
  useEffect(() => {
    if (deepLinkApplied.current) return;
    if (treeStatus !== 'success' || initialCategoryId <= 0) return;
    deepLinkApplied.current = true;
    const found = findShopPath(tree, initialCategoryId);
    if (found.length > 0) setPath(found);
  }, [treeStatus, tree, initialCategoryId]);

  // ── یک صفحه از دستهٔ باز ─────────────────────────────────────────────────
  useEffect(() => {
    if (!current) {
      setPage(null);
      return;
    }
    const categoryId = current.category.id;
    const myRequest = ++pageSeqRef.current;
    setPageStatus('loading');
    shopService
      .fetchCatalog(categoryId, 1, SHOP_PAGE_SIZE)
      .then((fresh) => {
        if (pageSeqRef.current !== myRequest) return;
        setPage(fresh);
        setPageStatus(fresh.products.length === 0 ? 'empty' : 'success');
      })
      .catch(() => {
        if (pageSeqRef.current !== myRequest) return;
        setPage(null);
        setPageStatus('error');
      });
  }, [current, pageTick]);

  // بازگشت اتصال → refetch بی‌صدا، فقط اگر آخرین بار خطا خورده بود.
  useOnlineRefetch(treeStatus === 'error', () => setTreeTick((t) => t + 1));
  useOnlineRefetch(pageStatus === 'error', () => setPageTick((t) => t + 1));

  // آدرس مرورگر با مسیر هم‌گام می‌شود (replaceState، نه push — برگشت مرورگر
  // باید کل فروشگاه را ترک کند، نه صفحه‌به‌صفحه عقب برود).
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const hash = current ? `#/shop?cat=${current.category.id}` : '#/shop';
    if (window.location.hash !== hash) {
      window.history.replaceState(window.history.state, '', hash);
    }
  }, [current]);

  const open = useCallback((node: ShopTreeNode) => {
    setPath((prev) => [...prev, node]);
  }, []);

  const back = useCallback(() => {
    setPath((prev) => (prev.length === 0 ? prev : prev.slice(0, -1)));
  }, []);

  const jumpTo = useCallback((index: number) => {
    setPath((prev) => (index < 0 || index >= prev.length - 1 ? prev : prev.slice(0, index + 1)));
  }, []);

  const loadMore = useCallback(() => {
    if (!current || !page || loadingMore) return;
    if (page.totalPages > 0 && page.page >= page.totalPages) return;
    const categoryId = current.category.id;
    const nextPage = page.page + 1;
    setLoadingMore(true);
    shopService
      .fetchCatalog(categoryId, nextPage, SHOP_PAGE_SIZE)
      .then((fresh) => {
        setPage((prev) =>
          prev && prev.categoryId === categoryId
            ? { ...fresh, products: [...prev.products, ...fresh.products] }
            : fresh
        );
      })
      // خطای «بیشتر» صفحهٔ روی نمایش را دور نمی‌ریزد؛ فقط دکمه برمی‌گردد.
      .catch(() => undefined)
      .finally(() => setLoadingMore(false));
  }, [current, page, loadingMore]);

  const hasMore = useMemo(() => {
    if (!page) return false;
    if (page.totalPages > 0) return page.page < page.totalPages;
    return page.products.length >= page.perPage;
  }, [page]);

  // ── منو ──────────────────────────────────────────────────────────────────
  if (!current) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-6 space-y-5 pb-24">
        <div className="space-y-1">
          <h1 className="text-xl sm:text-2xl font-black text-amber-950 flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-amber-800" />
            <span>فروشگاه دست‌بافت‌ها</span>
          </h1>
          <p className="text-xs sm:text-sm text-stone-600">
            آثار دست‌ساز کارگاه مستر دستباف — یک دسته را انتخاب کنید.
          </p>
        </div>

        <AsyncState
          status={treeStatus}
          onRetry={() => setTreeTick((t) => t + 1)}
          error="بارگذاری دسته‌بندی‌های فروشگاه با خطا مواجه شد."
          loadingLabel="در حال بارگذاری دسته‌بندی‌ها..."
          emptyTitle="فروشگاه در حال آماده‌سازی است"
          emptyDescription="هنوز محصولی در دسته‌های فروشگاه منتشر نشده است."
        >
          <div className="space-y-2.5">
            {tree.map((node) => (
              <button
                key={node.category.id}
                type="button"
                onClick={() => open(node)}
                className="w-full text-right bg-white rounded-2xl border border-stone-200/90 hover:border-amber-300 hover:shadow-md transition p-4 flex items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <p className="font-bold text-sm text-stone-900 truncate">{node.category.name}</p>
                  <p className="text-[11px] text-stone-500 mt-0.5">
                    {node.children.length > 0
                      ? `${faNum(node.children.length)} زیردسته · ${faNum(node.count)} محصول`
                      : `${faNum(node.count)} محصول`}
                  </p>
                </div>
                <ChevronLeft className="w-4 h-4 text-stone-400 shrink-0" />
              </button>
            ))}
          </div>
        </AsyncState>

        <button
          type="button"
          onClick={() => onNavigate('home')}
          className="text-xs text-amber-900 font-semibold underline"
        >
          بازگشت به صفحهٔ نخست
        </button>
      </div>
    );
  }

  // ── یک دسته ──────────────────────────────────────────────────────────────
  return (
    <div className="max-w-5xl mx-auto px-4 py-6 space-y-4 pb-24">
      {/* بردکرامب + برگشت */}
      <div className="flex items-center justify-between gap-3">
        <nav aria-label="مسیر فروشگاه" className="flex items-center gap-1 flex-wrap min-w-0">
          <button
            type="button"
            onClick={() => setPath([])}
            className="text-[11px] text-amber-800 hover:underline shrink-0"
          >
            فروشگاه
          </button>
          {path.map((node, index) => {
            const isLast = index === path.length - 1;
            return (
              <span key={node.category.id} className="flex items-center gap-1 min-w-0">
                <span className="text-stone-400 text-[11px]">›</span>
                <button
                  type="button"
                  onClick={() => jumpTo(index)}
                  disabled={isLast}
                  className={`text-[11px] truncate ${
                    isLast ? 'text-stone-700 font-semibold' : 'text-amber-800 hover:underline'
                  }`}
                >
                  {node.category.name}
                </button>
              </span>
            );
          })}
        </nav>

        <button
          type="button"
          onClick={back}
          className="inline-flex min-h-9 items-center gap-1 text-[11px] font-semibold text-stone-700 bg-white border border-stone-200 rounded-xl px-2.5 py-1.5 hover:border-stone-300 transition shrink-0"
        >
          <ChevronLeft className="w-3.5 h-3.5 rotate-180" />
          یک سطح بالاتر
        </button>
      </div>

      <div className="space-y-1">
        <h1 className="text-lg sm:text-xl font-black text-amber-950">{current.category.name}</h1>
        <p className="text-[11px] text-stone-500">
          {page ? `${faNum(page.total)} محصول در این دسته` : 'در حال شمارش...'}
        </p>
      </div>

      {/* زیردسته‌ها — چیپ */}
      {current.children.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none]">
          {current.children.map((child) => (
            <button
              key={child.category.id}
              type="button"
              onClick={() => open(child)}
              className="shrink-0 bg-white border border-stone-200 hover:border-amber-400 text-stone-700 text-[11px] font-medium px-3 py-1.5 rounded-full transition"
            >
              {child.category.name} ({faNum(child.count)})
            </button>
          ))}
        </div>
      )}

      <AsyncState
        status={pageStatus}
        onRetry={() => setPageTick((t) => t + 1)}
        error="بارگذاری محصولات این دسته با خطا مواجه شد."
        loadingLabel="در حال بارگذاری محصولات..."
        emptyTitle="در این دسته محصولی نیست"
        emptyDescription="محصولات تازه به‌زودی اینجا نمایش داده می‌شوند."
      >
        {page && (
          <>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
              {page.products.map((product) => (
                <div
                  key={product.id}
                  className="bg-white rounded-2xl border border-stone-200/90 overflow-hidden shadow-xs hover:shadow-md transition flex flex-col"
                >
                  {product.imageUrl ? (
                    <img
                      src={product.imageUrl}
                      alt={product.title}
                      loading="lazy"
                      className="aspect-square w-full object-cover bg-stone-100"
                    />
                  ) : (
                    <div className="aspect-square w-full bg-gradient-to-br from-amber-100 to-amber-200 flex items-center justify-center">
                      <span className="text-2xl font-black text-amber-800">
                        {product.title.slice(0, 1)}
                      </span>
                    </div>
                  )}

                  <div className="p-3 flex-1 flex flex-col justify-between gap-2">
                    <p className="text-xs font-bold text-stone-900 leading-snug line-clamp-2">
                      {product.title}
                    </p>
                    <div className="space-y-2">
                      <p className="text-xs font-black text-amber-900">
                        {product.price > 0 ? `${formatToman(product.price)} تومان` : 'تماس بگیرید'}
                      </p>
                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          onClick={() => onBuyProduct(product)}
                          className="flex-1 min-h-9 bg-amber-800 hover:bg-amber-900 text-white text-[11px] font-bold rounded-xl transition"
                        >
                          خرید
                        </button>
                        {product.url !== '' && (
                          <a
                            href={product.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            aria-label={`مشاهدهٔ ${product.title} در سایت`}
                            className="min-h-9 min-w-9 flex items-center justify-center text-stone-500 hover:text-amber-900 border border-stone-200 rounded-xl transition"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {hasMore && (
              <div className="pt-2 text-center">
                <button
                  type="button"
                  onClick={loadMore}
                  disabled={loadingMore}
                  className="inline-flex min-h-11 items-center gap-2 bg-amber-800 hover:bg-amber-900 disabled:bg-amber-800/60 text-white text-xs font-bold px-5 py-2.5 rounded-xl transition"
                >
                  {loadingMore && (
                    <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  )}
                  نمایش محصولات بیشتر
                </button>
              </div>
            )}

            {!hasMore && page.products.length >= page.perPage && (
              <p className="pt-2 text-center text-[11px] text-stone-400 flex items-center justify-center gap-1">
                <PackageSearch className="w-3.5 h-3.5" />
                پایان این دسته
              </p>
            )}
          </>
        )}
      </AsyncState>
    </div>
  );
};
