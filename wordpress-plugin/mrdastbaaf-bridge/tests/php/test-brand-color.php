<?php
/**
 * تست رنگ برند (test-brand-color) — بدون وردپرس
 *
 * اجرا: php tests/php/test-brand-color.php
 *
 * چرا این تست: از v1.7.23 پالت اپ و وب از چهار گزینهٔ پیشخوان می‌آید. دو حالت
 * خطرناک دارد:
 *   ۱) مقدار نامعتبر (تایپ اشتباه ادمین) — نباید رنگ برند را عوض کند، ولی
 *      نباید هم اپ را بی‌رنگ کند؛ باید مقدار قبلی همان گزینه بماند.
 *   ۲) «حفظ مقدار قبلی» اگر گزینه را نگیرد، رنگ primary را روی accent/canvas
 *      می‌نویسد — یک باگ خاموش که فقط در ظاهر اپ دیده می‌شود.
 *
 * get_option در این تست با یک آرایهٔ درون‌حافظه‌ای جایگزین می‌شود تا هر دو
 * حالت قابل سنجش باشند.
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

require_once __DIR__ . '/../../includes/class-settings-controller.php';

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

$sanitize = static function ($value, $option, $default) {
    return MrDastbaaf_Settings_Controller::sanitize_brand_color($value, $option, $default);
};

// ── مقادیر معتبر ────────────────────────────────────────────────────────────
mrd_assert('6 رقم با حروف کوچک → حروف بزرگ', $sanitize('#abcdef', 'o', '#000000'), '#ABCDEF');
mrd_assert('فاصلهٔ اضافه حذف می‌شود', $sanitize('  #aabbcc  ', 'o', '#000000'), '#AABBCC');
mrd_assert('فرم سه‌رقمی پذیرفته می‌شود', $sanitize('#abc', 'o', '#000000'), '#ABC');
mrd_assert('بدون # رد می‌شود (فرم پنل همیشه # دارد)', $sanitize('AABBCC', 'o', '#9A3412'), '#9A3412');

// ── مقادیر نامعتبر → مقدار قبلیِ همان گزینه ─────────────────────────────────
$mrd_options['mrdastbaaf_brand_accent'] = '#123456';
mrd_assert('کلمه به‌جای رنگ → accent قبلی می‌ماند', $sanitize('red', 'mrdastbaaf_brand_accent', '#D97706'), '#123456');

$mrd_options['mrdastbaaf_brand_canvas'] = '#00FF00';
mrd_assert('پنج رقم → canvas قبلی می‌ماند', $sanitize('#12345', 'mrdastbaaf_brand_canvas', '#FDFBF7'), '#00FF00');

// ── بدون مقدار قبلی (یا مقدار قبلی خراب) → پیش‌فرض برند ─────────────────────
mrd_assert('گزینهٔ تازه و ورودی خالی → پیش‌فرض', $sanitize('', 'mrdastbaaf_brand_new', '#047857'), '#047857');

$mrd_options['mrdastbaaf_brand_success'] = 'not-a-colour';
mrd_assert('مقدار قبلی خراب → پیش‌فرض، نه رشتهٔ خراب', $sanitize('blue', 'mrdastbaaf_brand_success', '#047857'), '#047857');

// ── هر گزینه فقط مقدار خودش را می‌خواند ─────────────────────────────────────
$mrd_options = array(
    'mrdastbaaf_brand_primary' => '#111111',
    'mrdastbaaf_brand_accent'  => '#222222',
);
mrd_assert(
    'accent نامعتبر → مقدار accent (نه primary)',
    $sanitize('oops', 'mrdastbaaf_brand_accent', '#D97706'),
    '#222222'
);

echo "\n================ نتیجه: {$pass} پاس / {$fail} شکست ================\n";
exit($fail === 0 ? 0 : 1);
