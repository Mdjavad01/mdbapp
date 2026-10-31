<?php
/**
 * تست تشخیص HLS (test-hls-url) — بدون وردپرس
 *
 * اجرا: php tests/php/test-hls-url.php
 *
 * چرا این تست: نوع استریم تعیین می‌کند کلاینت کیفیت را چطور عوض کند. اگر
 * master.m3u8 به‌اشتباه mp4 تشخیص داده شود، اپ سعی می‌کند فایل پلی‌لیست را
 * مثل یک ویدیو پخش کند؛ و اگر mp4 به‌اشتباه hls تشخیص داده شود، منوی کیفیت
 * خالی می‌ماند. حالت ظریف ماجرا query string است: `master.m3u8?token=…` هم
 * HLS است، ولی نام فایل می‌تواند شامل «.m3u8» باشد بدون اینکه HLS باشد.
 */

define('ABSPATH', '/tmp/');

require_once __DIR__ . '/../../includes/class-courses-controller.php';

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

$method = new ReflectionMethod('MrDastbaaf_Courses_Controller', 'is_hls_url');
$method->setAccessible(true);

$is_hls = static function ($url) use ($method) {
    return $method->invoke(null, $url);
};

// ── HLS ─────────────────────────────────────────────────────────────────────
mrd_assert('master.m3u8', $is_hls('https://dll.mrdastbaaf.com/FA/jetBaft/jetbaft-30/master.m3u8'), true);
mrd_assert('uppercase extension', $is_hls('https://dll.mrdastbaaf.com/FA/x/MASTER.M3U8'), true);
mrd_assert('with query string', $is_hls('https://dll.mrdastbaaf.com/FA/x/master.m3u8?token=abc'), true);
mrd_assert('with fragment', $is_hls('https://dll.mrdastbaaf.com/FA/x/master.m3u8#t=10'), true);

// ── not HLS ─────────────────────────────────────────────────────────────────
mrd_assert('mp4', $is_hls('https://dll.mrdastbaaf.com/FA/x/lesson.mp4'), false);
mrd_assert('mp4 with query', $is_hls('https://dll.mrdastbaaf.com/FA/x/lesson.mp4?token=abc'), false);
mrd_assert('m3u8 only in the query', $is_hls('https://dll.mrdastbaaf.com/FA/x/lesson.mp4?name=master.m3u8'), false);
mrd_assert('m3u8 in the middle of the path', $is_hls('https://dll.mrdastbaaf.com/FA/master.m3u8/lesson.mp4'), false);
mrd_assert('empty', $is_hls(''), false);
mrd_assert('proxy url with no extension', $is_hls('https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/lessons/12/stream?token=x'), false);

echo "\n=== نتیجه: $pass پاس / $fail خطا ===\n";
exit($fail > 0 ? 1 : 0);
