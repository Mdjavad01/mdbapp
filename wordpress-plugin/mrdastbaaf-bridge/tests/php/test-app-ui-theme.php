<?php
/**
 * تست بلاک theme در GET /app-ui (test-app-ui-theme) — بدون وردپرس
 *
 * اجرا: php tests/php/test-app-ui-theme.php
 *
 * چرا این تست: `get_defaults()` بلاک theme را می‌ساخت، ولی `get_app_ui()`
 * آرایهٔ خروجی را دستی می‌چیند و آن یک خط جا افتاده بود. نتیجه: پلاگین ۱.۷.۲۳
 * روی سایت مستقر شد، فیلدهای رنگ در پیشخوان کار می‌کردند، آپشن‌ها درست ذخیره
 * می‌شدند — و هیچ‌کدام به کلاینت نمی‌رسید. چون هر دو کلاینت «غایب = پیش‌فرض»
 * را درست پیاده کرده‌اند، هیچ خطایی هم دیده نمی‌شد؛ فقط تغییر رنگ بی‌اثر بود.
 *
 * این تست دقیقاً همان مسیر را می‌سنجد: خروجی get_app_ui() باید theme داشته
 * باشد، مقدار آپشن را منعکس کند و مقدار خراب را به پیش‌فرض برگرداند.
 */

define('ABSPATH', '/tmp/');

/** @var array<string,mixed> */
$mrd_options = array();

if (!function_exists('get_option')) {
    function get_option($name, $default = false) {
        global $mrd_options;
        return array_key_exists($name, $mrd_options) ? $mrd_options[$name] : $default;
    }
}

require_once __DIR__ . '/../../includes/class-app-ui-controller.php';

$pass = 0;
$fail = 0;

function mrd_assert($label, $actual, $expected) {
    global $pass, $fail;
    if ($actual === $expected) {
        $pass++;
        echo "PASS: $label\n";
        return;
    }
    $fail++;
    echo "FAIL: $label — expected " . var_export($expected, true) . ', got ' . var_export($actual, true) . "\n";
}

$ui = MrDastbaaf_App_UI_Controller::get_app_ui();

// ── ۱) بلاک theme در خروجی هست ──────────────────────────────────────────────
mrd_assert('پاسخ theme دارد', isset($ui['theme']), true);
mrd_assert(
    'چهار لنگر',
    is_array($ui['theme']) ? array_keys($ui['theme']) : array(),
    array('primary', 'accent', 'canvas', 'success')
);
mrd_assert('primary پیش‌فرض', $ui['theme']['primary'], '#9A3412');
mrd_assert('canvas پیش‌فرض', $ui['theme']['canvas'], '#FDFBF7');

// ── ۲) مقدار پیشخوان منعکس می‌شود ───────────────────────────────────────────
$mrd_options['mrdastbaaf_brand_primary'] = '#1A2B3C';
$ui = MrDastbaaf_App_UI_Controller::get_app_ui();
mrd_assert('رنگ ذخیره‌شده در پاسخ می‌آید', $ui['theme']['primary'], '#1A2B3C');
// بقیه دست‌نخورده
mrd_assert('بقیهٔ لنگرها پیش‌فرض می‌مانند', $ui['theme']['accent'], '#D97706');

// ── ۳) مقدار خراب به پیش‌فرض برمی‌گردد (نه به کلاینت) ───────────────────────
$mrd_options['mrdastbaaf_brand_accent'] = 'red';
$ui = MrDastbaaf_App_UI_Controller::get_app_ui();
mrd_assert('رنگ خراب → پیش‌فرض برند', $ui['theme']['accent'], '#D97706');

// ── ۴) بقیهٔ قرارداد دست‌نخورده است ─────────────────────────────────────────
mrd_assert('hero هست', isset($ui['hero']['headline']), true);
mrd_assert('sections آرایه است', is_array($ui['sections']), true);
mrd_assert('menu.dock آرایه است', is_array($ui['menu']['dock']), true);
mrd_assert('featuredCourseIds آرایه است', is_array($ui['featuredCourseIds']), true);

echo "\n================ نتیجه: {$pass} پاس / {$fail} شکست ================\n";
exit($fail === 0 ? 0 : 1);
