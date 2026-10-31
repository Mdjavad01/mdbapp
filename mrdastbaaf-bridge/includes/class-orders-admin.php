<?php
/**
 * صفحهٔ «سفارش‌ها» — فهرست همهٔ خریدها با منبع (اپ / وب) و خروجی CSV.
 *
 * چرا این صفحه: تا پیش از این، فروش آنلاین اشتراک هیچ صفحه‌ای در پیشخوان نداشت
 * (صفحهٔ «بررسی فیش‌ها» فقط ردیف‌های کارت‌به‌کارت را می‌آورد) و منبع خرید هم
 * ذخیره نمی‌شد، پس فروش اپلیکیشن از فروش سایت قابل تفکیک نبود.
 *
 * همه‌چیز از یک جدول خوانده می‌شود (هیچ داده‌ای کپی نمی‌شود) و با صفحه‌بندی
 * سمت سرور + ایندکس created_at کار می‌کند، تا با ۱۰۰ هزار سفارش هم صفحه سریع
 * باز شود.
 *
 * @package MrDastbaaf
 */

if (!defined('ABSPATH')) {
    exit;
}

class MrDastbaaf_Orders_Admin {

    const SLUG = 'mrdastbaaf-orders';
    const PER_PAGE = 25;
    const CSV_CHUNK = 500;

    /** ستون‌هایی که فهرست لازم دارد — نه یک ستون بیشتر. */
    const COLUMNS = 'id, order_number, user_id, user_phone, user_fullname, plan_id, item_title, amount_toman, gateway, status, source, created_at';

    public function register() {
        // اولویت ۲۰ تا زیرمنو بعد از بقیهٔ زیرمنوهای افزونه بنشیند.
        add_action('admin_menu', array($this, 'register_menu'), 20);
        add_action('admin_post_mrdastbaaf_orders_csv', array($this, 'handle_csv_export'));
    }

    public function register_menu() {
        add_submenu_page(
            MrDastbaaf_Settings_Controller::MENU_SLUG,
            'سفارش‌های مستر دستباف',
            'سفارش‌ها',
            'manage_options',
            self::SLUG,
            array($this, 'render_page')
        );
    }

    // ── فیلترها ─────────────────────────────────────────────────────────────

    /**
     * @return array{source:string,kind:string,status:string,gateway:string,search:string,page:int}
     */
    private function read_filters() {
        $source = isset($_GET['src']) ? sanitize_key(wp_unslash($_GET['src'])) : 'all';
        if (!in_array($source, array('all', 'app', 'web'), true)) {
            $source = 'all';
        }

        $kind = isset($_GET['kind']) ? sanitize_key(wp_unslash($_GET['kind'])) : 'all';
        if (!in_array($kind, array('all', 'subscription', 'course', 'shop'), true)) {
            $kind = 'all';
        }

        $status = isset($_GET['status']) ? sanitize_key(wp_unslash($_GET['status'])) : 'all';
        if (!in_array($status, array('all', 'pending', 'verifying', 'completed', 'failed', 'rejected', 'cancelled'), true)) {
            $status = 'all';
        }

        $gateway = isset($_GET['gw']) ? sanitize_key(wp_unslash($_GET['gw'])) : 'all';
        if (!in_array($gateway, array('all', 'zibal', 'card_to_card'), true)) {
            $gateway = 'all';
        }

        return array(
            'source'  => $source,
            'kind'    => $kind,
            'status'  => $status,
            'gateway' => $gateway,
            'search'  => isset($_GET['s']) ? sanitize_text_field(wp_unslash($_GET['s'])) : '',
            'page'    => max(1, isset($_GET['paged']) ? absint($_GET['paged']) : 1),
        );
    }

    /**
     * شرط WHERE و پارامترهای آماده — همهٔ مقادیر از فهرست سفید رد شده‌اند.
     *
     * @param array $f خروجی read_filters()
     * @return array{0:string,1:array}
     */
    private function build_where(array $f) {
        global $wpdb;

        $where = array();
        $params = array();

        if ('all' !== $f['source']) {
            $where[] = 'source = %s';
            $params[] = $f['source'];
        }

        if ('subscription' === $f['kind']) {
            $where[] = 'plan_id NOT LIKE %s AND plan_id NOT LIKE %s';
            $params[] = 'shop-%';
            $params[] = 'course-%';
        } elseif ('course' === $f['kind']) {
            $where[] = 'plan_id LIKE %s';
            $params[] = 'course-%';
        } elseif ('shop' === $f['kind']) {
            $where[] = 'plan_id LIKE %s';
            $params[] = 'shop-%';
        }

        if ('all' !== $f['status']) {
            $where[] = 'status = %s';
            $params[] = $f['status'];
        }

        if ('all' !== $f['gateway']) {
            $where[] = 'gateway = %s';
            $params[] = $f['gateway'];
        }

        if ('' !== $f['search']) {
            $like = '%' . $wpdb->esc_like($f['search']) . '%';
            $where[] = '(order_number LIKE %s OR user_phone LIKE %s OR user_fullname LIKE %s OR item_title LIKE %s)';
            array_push($params, $like, $like, $like, $like);
        }

        $sql = $where ? ('WHERE ' . implode(' AND ', $where)) : '';
        if ($params) {
            $sql = $wpdb->prepare($sql, $params);
        }

        return array($sql, $params);
    }

    // ── برچسب‌ها ────────────────────────────────────────────────────────────

    private function source_label($source) {
        if ('app' === $source) {
            return 'اپلیکیشن';
        }
        if ('web' === $source) {
            return 'وب‌اپ';
        }
        return '—';
    }

    private function source_pill_class($source) {
        return ('app' === $source) ? 'mrd-pill mrd-pill--free' : 'mrd-pill mrd-pill--silver';
    }

    /** نوع خرید از پیشوند plan_id — همان قراردادی که payment controller می‌نویسد. */
    private function kind_label($plan_id) {
        $plan_id = (string) $plan_id;
        if (0 === strpos($plan_id, 'shop-')) {
            return 'محصول فیزیکی';
        }
        if (0 === strpos($plan_id, 'course-')) {
            return 'دوره';
        }
        return 'اشتراک';
    }

    private function status_label($status) {
        $map = array(
            'pending'   => 'در انتظار',
            'verifying' => 'در حال بررسی',
            'completed' => 'تاییدشده',
            'failed'    => 'ناموفق',
            'rejected'  => 'ردشده',
            'cancelled' => 'لغو‌شده',
        );
        return isset($map[$status]) ? $map[$status] : (string) $status;
    }

    private function status_pill_class($status) {
        $allowed = array('pending', 'verifying', 'completed', 'failed', 'rejected');
        $class = in_array($status, $allowed, true) ? $status : 'off';
        return 'mrd-pill mrd-pill--' . $class;
    }

    private function gateway_label($gateway) {
        return ('card_to_card' === $gateway) ? 'کارت‌به‌کارت' : 'زیبال';
    }

    private function money($amount) {
        return number_format_i18n((int) $amount);
    }

    // ── صفحه ────────────────────────────────────────────────────────────────

    public function render_page() {
        global $wpdb;

        if (!current_user_can('manage_options')) {
            wp_die('دسترسی مجاز نیست.');
        }

        $table_orders = $wpdb->prefix . 'mrdastbaaf_orders';
        $f = $this->read_filters();
        list($where_sql) = $this->build_where($f);

        $total_rows = (int) $wpdb->get_var("SELECT COUNT(*) FROM $table_orders $where_sql");
        $sum_toman = (int) $wpdb->get_var("SELECT COALESCE(SUM(amount_toman), 0) FROM $table_orders $where_sql");

        $total_pages = max(1, (int) ceil($total_rows / self::PER_PAGE));
        $page = min($f['page'], $total_pages);
        $offset = ($page - 1) * self::PER_PAGE;

        $rows = $wpdb->get_results($wpdb->prepare(
            'SELECT ' . self::COLUMNS . " FROM $table_orders $where_sql ORDER BY created_at DESC, id DESC LIMIT %d OFFSET %d",
            self::PER_PAGE,
            $offset
        ));

        // شمارش ۳۰ روز اخیر به‌تفکیک منبع — روی ایندکس created_at، بدون اسکن کل جدول.
        $recent = $wpdb->get_results(
            "SELECT source, COUNT(*) AS c FROM $table_orders
             WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
             GROUP BY source"
        );
        $app_30 = 0;
        $web_30 = 0;
        foreach ((array) $recent as $r) {
            if ('app' === (string) $r->source) {
                $app_30 = (int) $r->c;
            } else {
                $web_30 += (int) $r->c;
            }
        }

        $base_url = admin_url('admin.php?page=' . self::SLUG);
        $csv_url = wp_nonce_url(
            admin_url('admin-post.php?action=mrdastbaaf_orders_csv'),
            'mrdastbaaf_orders_csv'
        );
        ?>
        <div class="wrap mrd-scope mrd-scope--wide">
            <div class="mrd-hero">
                <div class="mrd-hero__icon" aria-hidden="true">📊</div>
                <div>
                    <h1 class="mrd-hero__title">سفارش‌ها</h1>
                    <p class="mrd-hero__desc">
                        همهٔ خریدها — اشتراک، دوره و محصول فیزیکی — با منبعشان: از
                        <strong>اپلیکیشن</strong> بوده یا از <strong>وب‌اپ</strong>.
                        اشتراک‌ها سفارش ووکامرس نمی‌سازند، پس این تنها جایی است که
                        فروش آنلاین اشتراک را تکی نشان می‌دهد.
                    </p>
                </div>
            </div>

            <div class="mrd-chips" style="margin-top:14px;">
                <span class="mrd-chip mrd-chip--amber">۳۰ روز اخیر — اپلیکیشن: <?php echo esc_html(number_format_i18n($app_30)); ?></span>
                <span class="mrd-chip">۳۰ روز اخیر — وب‌اپ: <?php echo esc_html(number_format_i18n($web_30)); ?></span>
                <span class="mrd-chip">نتیجهٔ این فیلتر: <?php echo esc_html(number_format_i18n($total_rows)); ?> سفارش</span>
                <span class="mrd-chip">جمع: <?php echo esc_html($this->money($sum_toman)); ?> تومان</span>
            </div>

            <div class="mrd-card" style="margin-top:14px;">
                <form method="get" action="<?php echo esc_url(admin_url('admin.php')); ?>" style="display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;">
                    <input type="hidden" name="page" value="<?php echo esc_attr(self::SLUG); ?>" />

                    <label style="display:flex;flex-direction:column;gap:4px;">
                        <span class="mrd-hint" style="margin:0;">منبع</span>
                        <select name="src">
                            <option value="all" <?php selected($f['source'], 'all'); ?>>همه</option>
                            <option value="app" <?php selected($f['source'], 'app'); ?>>اپلیکیشن</option>
                            <option value="web" <?php selected($f['source'], 'web'); ?>>وب‌اپ</option>
                        </select>
                    </label>

                    <label style="display:flex;flex-direction:column;gap:4px;">
                        <span class="mrd-hint" style="margin:0;">نوع خرید</span>
                        <select name="kind">
                            <option value="all" <?php selected($f['kind'], 'all'); ?>>همه</option>
                            <option value="subscription" <?php selected($f['kind'], 'subscription'); ?>>اشتراک</option>
                            <option value="course" <?php selected($f['kind'], 'course'); ?>>دوره</option>
                            <option value="shop" <?php selected($f['kind'], 'shop'); ?>>محصول فیزیکی</option>
                        </select>
                    </label>

                    <label style="display:flex;flex-direction:column;gap:4px;">
                        <span class="mrd-hint" style="margin:0;">وضعیت</span>
                        <select name="status">
                            <option value="all" <?php selected($f['status'], 'all'); ?>>همه</option>
                            <option value="pending" <?php selected($f['status'], 'pending'); ?>>در انتظار</option>
                            <option value="verifying" <?php selected($f['status'], 'verifying'); ?>>در حال بررسی</option>
                            <option value="completed" <?php selected($f['status'], 'completed'); ?>>تاییدشده</option>
                            <option value="failed" <?php selected($f['status'], 'failed'); ?>>ناموفق</option>
                            <option value="rejected" <?php selected($f['status'], 'rejected'); ?>>ردشده</option>
                            <option value="cancelled" <?php selected($f['status'], 'cancelled'); ?>>لغو‌شده</option>
                        </select>
                    </label>

                    <label style="display:flex;flex-direction:column;gap:4px;">
                        <span class="mrd-hint" style="margin:0;">درگاه</span>
                        <select name="gw">
                            <option value="all" <?php selected($f['gateway'], 'all'); ?>>همه</option>
                            <option value="zibal" <?php selected($f['gateway'], 'zibal'); ?>>زیبال</option>
                            <option value="card_to_card" <?php selected($f['gateway'], 'card_to_card'); ?>>کارت‌به‌کارت</option>
                        </select>
                    </label>

                    <label style="display:flex;flex-direction:column;gap:4px;min-width:220px;">
                        <span class="mrd-hint" style="margin:0;">جست‌وجو (شماره سفارش، موبایل، نام، عنوان)</span>
                        <input type="search" name="s" value="<?php echo esc_attr($f['search']); ?>" />
                    </label>

                    <button type="submit" class="button button-primary">اعمال</button>
                    <a class="button" href="<?php echo esc_url($base_url); ?>">پاک کردن فیلترها</a>
                </form>
            </div>

            <div class="mrd-table-wrap">
                <table class="widefat striped">
                    <thead>
                        <tr>
                            <th>شماره سفارش</th>
                            <th>منبع</th>
                            <th>نوع</th>
                            <th>عنوان</th>
                            <th>کاربر</th>
                            <th>مبلغ (تومان)</th>
                            <th>درگاه</th>
                            <th>وضعیت</th>
                            <th>تاریخ ثبت</th>
                        </tr>
                    </thead>
                    <tbody>
                    <?php if (empty($rows)) : ?>
                        <tr>
                            <td colspan="9">
                                <span class="mrd-hint">سفارشی با این فیلترها پیدا نشد.</span>
                            </td>
                        </tr>
                    <?php else : ?>
                        <?php foreach ($rows as $r) : ?>
                            <tr>
                                <td><code dir="ltr"><?php echo esc_html($r->order_number); ?></code></td>
                                <td>
                                    <span class="<?php echo esc_attr($this->source_pill_class($r->source)); ?>">
                                        <?php echo esc_html($this->source_label($r->source)); ?>
                                    </span>
                                </td>
                                <td><?php echo esc_html($this->kind_label($r->plan_id)); ?></td>
                                <td><?php echo esc_html($r->item_title); ?></td>
                                <td>
                                    <?php echo esc_html($r->user_fullname); ?>
                                    <br />
                                    <span class="mrd-hint" dir="ltr" style="margin:0;"><?php echo esc_html($r->user_phone); ?></span>
                                </td>
                                <td><?php echo esc_html($this->money($r->amount_toman)); ?></td>
                                <td><?php echo esc_html($this->gateway_label($r->gateway)); ?></td>
                                <td>
                                    <span class="<?php echo esc_attr($this->status_pill_class($r->status)); ?>">
                                        <?php echo esc_html($this->status_label($r->status)); ?>
                                    </span>
                                </td>
                                <td dir="ltr"><?php echo esc_html($r->created_at); ?></td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                    </tbody>
                </table>
            </div>

            <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                <div>
                    <?php
                    $links = paginate_links(array(
                        'base'      => add_query_arg('paged', '%#%', $base_url),
                        'format'    => '',
                        'current'   => $page,
                        'total'     => $total_pages,
                        'prev_text' => '‹ قبلی',
                        'next_text' => 'بعدی ›',
                    ));
                    if ($links) {
                        echo '<div class="tablenav"><div class="tablenav-pages">' . $links . '</div></div>';
                    }
                    ?>
                </div>
                <a class="button" href="<?php echo esc_url($csv_url); ?>">خروجی CSV از همین فیلتر</a>
            </div>
        </div>
        <?php
    }

    // ── خروجی CSV ───────────────────────────────────────────────────────────

    /**
     * استریم تکه‌تکه — هرگز همهٔ سفارش‌ها در حافظه بارگذاری نمی‌شوند.
     */
    public function handle_csv_export() {
        global $wpdb;

        if (!current_user_can('manage_options')) {
            wp_die('دسترسی مجاز نیست.');
        }
        check_admin_referer('mrdastbaaf_orders_csv');

        $table_orders = $wpdb->prefix . 'mrdastbaaf_orders';
        $f = $this->read_filters();
        list($where_sql) = $this->build_where($f);

        if (function_exists('set_time_limit')) {
            @set_time_limit(0);
        }

        nocache_headers();
        header('Content-Type: text/csv; charset=utf-8');
        header('Content-Disposition: attachment; filename="mrdastbaaf-orders-' . gmdate('Ymd-His') . '.csv"');

        $out = fopen('php://output', 'w');
        // BOM تا اکسل فارسی را درست باز کند.
        fwrite($out, "\xEF\xBB\xBF");
        fputcsv($out, array(
            'شماره سفارش',
            'منبع',
            'نوع',
            'عنوان',
            'کاربر',
            'موبایل',
            'مبلغ (تومان)',
            'درگاه',
            'وضعیت',
            'تاریخ ثبت',
        ));

        $offset = 0;
        do {
            $rows = $wpdb->get_results($wpdb->prepare(
                'SELECT ' . self::COLUMNS . " FROM $table_orders $where_sql ORDER BY created_at DESC, id DESC LIMIT %d OFFSET %d",
                self::CSV_CHUNK,
                $offset
            ));

            foreach ((array) $rows as $r) {
                fputcsv($out, array(
                    $r->order_number,
                    $this->source_label($r->source),
                    $this->kind_label($r->plan_id),
                    $r->item_title,
                    $r->user_fullname,
                    $r->user_phone,
                    (int) $r->amount_toman,
                    $this->gateway_label($r->gateway),
                    $this->status_label($r->status),
                    $r->created_at,
                ));
            }

            $count = count((array) $rows);
            $offset += self::CSV_CHUNK;
        } while ($count === self::CSV_CHUNK);

        fclose($out);
        exit;
    }
}
