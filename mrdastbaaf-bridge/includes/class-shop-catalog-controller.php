<?php
/**
 * فروشگاه مقیاس‌پذیر (v1.7.16 — فایل NEW)
 *
 * چرا فایل جدا: فروشگاه سایت بیش از ۱۰ دسته و ۱۸۰۰ محصول دارد. قرارداد قبلی
 * (GET /shop/products) همهٔ محصولات را در یک پاسخ می‌فرستاد و برای هر کدام
 * wc_get_product() صدا می‌زد؛ یعنی ~۶۰۰ کیلوبایت JSON و ۱۸۰۰ آبجکت محصول در
 * یک درخواست. با ۱۸۰۰ محصول این نه کند است، نه قابل کش.
 *
 * این کنترلر دو endpoint جدید و مستقل اضافه می‌کند و به هیچ قرارداد موجودی دست
 * نمی‌زند؛ GET /shop/products عیناً سر جایش می‌ماند تا وب‌اپ نشکند.
 *
 *   GET /shop/categories
 *     → { success, categories: [ {id, slug, name, parent, count}, … ] }
 *     درخت کامل، بدون هیچ محصولی. تعداد ردیف = تعداد دسته (ده‌تا)، حجم < ۲KB.
 *     نتیجه ۱۰ دقیقه در transient کش می‌شود؛ در حالت کش هیچ کوئری‌ای اجرا نمی‌شود.
 *
 *   GET /shop/catalog?category=34&page=1&per_page=24
 *     → { success, category, page, perPage, total, totalPages, products: [ … ] }
 *     فقط محصولات همان دسته (با زیرشاخه‌ها، از include_children خود ووکامرس) و
 *     فقط همان یک صفحه. ۲۴ آبجکت محصول، نه ۱۸۰۰.
 *
 * کدام محصولات: به‌جای فهرست تیک‌خوردهٔ دستی (که با ۱۸۰۰ محصول عملاً غیرممکن
 * است)، از خود ووکامرس استفاده می‌شود — محصول «منتشرشده» که در کاتالوگ پنهان
 * نشده باشد. یعنی همان تیک «پنهان» در ویرایش محصول، که ادمین از قبل می‌شناسد.
 *
 * شمارش: از pad_counts خود get_terms می‌آید (شامل زیرشاخه‌ها). شمارش تزئینی است
 * و ممکن است محصول «پنهان» را هم بشمارد؛ برای همین عدد دسته ممکن است یک واحد
 * از تعداد واقعی ردیف‌ها بیشتر باشد. عمداً: شمارش دقیق به یک کوئری تجمیعی
 * جداگانه نیاز داشت و شرط این پلاگین «کوئری اضافه نزن» است.
 */

if (!defined('ABSPATH')) {
    exit;
}

class MrDastbaaf_Shop_Catalog_Controller {

    const CATEGORIES_CACHE_KEY = 'mrdastbaaf_shop_categories_tree';
    const CATEGORIES_CACHE_TTL = 600;
    const DEFAULT_PER_PAGE = 24;
    const MAX_PER_PAGE = 60;

    public function register_routes() {
        register_rest_route(MRDASTBAAF_API_NAMESPACE, '/shop/categories', array(
            'methods'             => 'GET',
            'callback'            => array($this, 'handle_categories'),
            'permission_callback' => '__return_true',
        ));

        register_rest_route(MRDASTBAAF_API_NAMESPACE, '/shop/catalog', array(
            'methods'             => 'GET',
            'callback'            => array($this, 'handle_catalog'),
            'permission_callback' => '__return_true',
        ));
    }

    /**
     * درخت دسته‌ها — بدون محصول.
     *
     * دو کوئری سبک در بدترین حالت (get_terms + شمارش خودش) و صفر کوئری وقتی
     * کش گرم است. ترتیب: نام صعودی، تا خروجی بین دو درخواست پایدار بماند.
     */
    public function handle_categories() {
        if (!class_exists('WooCommerce')) {
            return new WP_Error('woocommerce_missing', 'ووکامرس فعال نیست؛ امکان نمایش فروشگاه وجود ندارد.', array('status' => 503));
        }

        $cached = get_transient(self::CATEGORIES_CACHE_KEY);
        if (is_array($cached)) {
            return rest_ensure_response(array(
                'success'    => true,
                'categories' => $cached,
            ));
        }

        $terms = get_terms(array(
            'taxonomy'   => 'product_cat',
            // دستهٔ خالی در منو چیزی برای نشان دادن ندارد.
            'hide_empty' => true,
            // شمارش شامل زیرشاخه‌ها؛ بدون این، والدِ فقط-گروهی «۰» نشان می‌دهد.
            'pad_counts' => true,
            'orderby'    => 'name',
            'order'      => 'ASC',
        ));

        if (is_wp_error($terms) || !is_array($terms)) {
            return rest_ensure_response(array('success' => true, 'categories' => array()));
        }

        $excluded = self::excluded_slugs();

        $categories = array();
        foreach ($terms as $term) {
            if (in_array((string) $term->slug, $excluded, true)) {
                continue;
            }
            $categories[] = array(
                'id'     => (int) $term->term_id,
                'slug'   => (string) $term->slug,
                'name'   => (string) $term->name,
                'parent' => (int) $term->parent,
                'count'  => (int) $term->count,
            );
        }

        set_transient(self::CATEGORIES_CACHE_KEY, $categories, self::CATEGORIES_CACHE_TTL);

        return rest_ensure_response(array(
            'success'    => true,
            'categories' => $categories,
        ));
    }

    /**
     * یک صفحه از محصولات یک دسته.
     *
     * @param WP_REST_Request $request
     */
    public function handle_catalog($request) {
        if (!class_exists('WooCommerce')) {
            return new WP_Error('woocommerce_missing', 'ووکامرس فعال نیست؛ امکان نمایش فروشگاه وجود ندارد.', array('status' => 503));
        }

        $category = absint($request->get_param('category'));
        $page = max(1, absint($request->get_param('page')));
        if (0 === $page) {
            $page = 1;
        }
        $per_page = absint($request->get_param('per_page'));
        if ($per_page < 1) {
            $per_page = self::DEFAULT_PER_PAGE;
        }
        $per_page = min($per_page, self::MAX_PER_PAGE);

        $tax_query = array('relation' => 'AND');
        if ($category > 0) {
            $tax_query[] = array(
                'taxonomy'         => 'product_cat',
                'field'            => 'term_id',
                'terms'            => array($category),
                // ووکامرس خودش زیرشاخه‌ها را هم می‌آورد؛ والد خالی نمی‌ماند.
                'include_children' => true,
            );
        }
        if (taxonomy_exists('product_visibility')) {
            // همان فیلتری که کاتالوگ خود ووکامرس می‌زند: محصول «پنهان» نمی‌آید.
            $tax_query[] = array(
                'taxonomy' => 'product_visibility',
                'field'    => 'name',
                'terms'    => array('exclude-from-catalog'),
                'operator' => 'NOT IN',
            );
        }

        $query = new WP_Query(array(
            'post_type'           => 'product',
            'post_status'         => 'publish',
            'posts_per_page'      => $per_page,
            'paged'               => $page,
            'orderby'             => 'title',
            'order'               => 'ASC',
            // برخلاف مسیر قبلی، اینجا شمارش لازم است تا کلاینت بداند چند صفحه
            // مانده — و SQL_CALC_FOUND_ROWS فقط یک بار اجرا می‌شود.
            'no_found_rows'       => false,
            'ignore_sticky_posts' => true,
            'tax_query'           => $tax_query,
        ));

        $products = array();
        foreach ($query->posts as $post) {
            $item = self::shape_product((int) $post->ID);
            if (null !== $item) {
                $products[] = $item;
            }
        }

        $category_row = null;
        if ($category > 0) {
            $term = get_term($category, 'product_cat');
            if ($term && !is_wp_error($term)) {
                $category_row = array(
                    'id'     => (int) $term->term_id,
                    'slug'   => (string) $term->slug,
                    'name'   => (string) $term->name,
                    'parent' => (int) $term->parent,
                );
            }
        }

        return rest_ensure_response(array(
            'success'    => true,
            'category'   => $category_row,
            'page'       => $page,
            'perPage'    => $per_page,
            'total'      => (int) $query->found_posts,
            'totalPages' => (int) $query->max_num_pages,
            'products'   => $products,
        ));
    }

    /**
     * یک محصول به شکل قراردادی — همان کلیدهای /shop/products، تا کلاینت یک
     * مدل داشته باشد.
     *
     * @param int $product_id
     * @return array|null
     */
    private static function shape_product($product_id) {
        $product = function_exists('wc_get_product') ? wc_get_product($product_id) : null;
        if (!$product) {
            return null;
        }

        $price = $product->get_price();
        $thumbnail = get_the_post_thumbnail_url($product_id, 'medium');

        return array(
            'id'       => (int) $product_id,
            'title'    => (string) get_the_title($product_id),
            'price'    => ('' !== $price && null !== $price) ? (int) $price : 0,
            'imageUrl' => (is_string($thumbnail) && '' !== $thumbnail) ? $thumbnail : null,
            'url'      => (string) get_permalink($product_id),
        );
    }

    /**
     * پاک کردن کش درخت — بعد از هر تغییری که دسته‌ها را جابه‌جا می‌کند.
     * ادمین در کنترلر صفحات ادمین و کنترلر خود محصولات صدا می‌زند.
     */
    public static function flush_cache() {
        delete_transient(self::CATEGORIES_CACHE_KEY);
    }

    /**
     * ابطال کش روی هر تغییری که ترکیب دسته‌ها را عوض می‌کند.
     *
     * بدون این، محصول تازه تا ده دقیقه در منو دیده نمی‌شود و ادمین فکر می‌کند
     * اپ خراب است. هوک‌ها باید در بارگذاری پلاگین ثبت شوند، نه در
     * rest_api_init — ذخیرهٔ محصول در پیشخوان یک POST معمولی است.
     */
    public static function register_cache_hooks() {
        $events = array(
            'save_post_product',
            'deleted_post',
            'created_product_cat',
            'edited_product_cat',
            'delete_product_cat',
            'woocommerce_update_product',
            'woocommerce_new_product',
            'woocommerce_delete_product',
        );
        foreach ($events as $event) {
            add_action($event, array(__CLASS__, 'flush_cache'));
        }
    }

    /**
     * Shop categories that must not appear in the app, by slug.
     *
     * The course category lives in this same taxonomy, but a course product is
     * not a physical item: left in, the shop menu shows the course category
     * between the looms and the yarn. Extend it with a filter rather than by
     * editing this file:
     *
     *   add_filter('mrdastbaaf_shop_excluded_categories', function ($list) {
     *       $list[] = 'all-products';
     *       return $list;
     *   });
     *
     * @return string[]
     */
    private static function excluded_slugs() {
        $default = array('courses');
        $list = apply_filters('mrdastbaaf_shop_excluded_categories', $default);
        if (!is_array($list)) {
            return $default;
        }
        $clean = array();
        foreach ($list as $slug) {
            $slug = sanitize_title((string) $slug);
            if ('' !== $slug) {
                $clean[] = $slug;
            }
        }
        return $clean;
    }
}
