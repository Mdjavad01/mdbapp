# نصب ۱.۷.۱۶ — فروشگاه برای ۱۸۰۰ محصول

این بسته دو تغییر دارد: یک فایل جدید و سه خط در فایل اصلی پلاگین.
بدون git، بدون کامند لاین. دو کار، پنج دقیقه.

نسخهٔ فعلی سایت شما **۱.۷.۱۵** است که همان آخرین نسخهٔ مخزن گیت‌هاب هم هست —
یعنی چیزی برای «آپدیت» وجود ندارد؛ این تنها تغییری است که باید دستی اضافه شود.

---

## کار ۱ — آپلود فایل جدید

فایل:

```
includes/class-shop-catalog-controller.php
```

را کپی کنید به:

```
wp-content/plugins/mrdastbaaf-bridge/includes/
```

از هر راهی که بلدید: FTP، فایل‌منیجر هاست، یا افزونهٔ File Manager در وردپرس.

**هنوز کاری نمی‌کند** — تا کار ۲ انجام نشود، فقط یک فایل بی‌استفاده است.

---

## کار ۲ — سه خط در فایل اصلی

فایل زیر را باز کنید (پیشخوان → افزونه‌ها → ویرایشگر فایل، یا FTP):

```
wp-content/plugins/mrdastbaaf-bridge/mrdastbaaf-bridge.php
```

### ۲-الف) این خط را پیدا کنید

```php
require_once MRDASTBAAF_BRIDGE_PATH . 'includes/class-shop-controller.php';
```

و **دقیقاً زیرش** این را بگذارید:

```php
require_once MRDASTBAAF_BRIDGE_PATH . 'includes/class-shop-catalog-controller.php';
MrDastbaaf_Shop_Catalog_Controller::register_cache_hooks();
```

### ۲-ب) این خط را پیدا کنید

```php
        $shop_controller->register_routes();
```

و **دقیقاً زیرش** این را بگذارید:

```php
        $shop_catalog_controller = new MrDastbaaf_Shop_Catalog_Controller();
        $shop_catalog_controller->register_routes();
```

توجه به فاصلهٔ ابتدای خط: هشت فاصله، مثل خط بالایش.

### ۲-ج) اختیاری — شمارهٔ نسخه

اگر می‌خواهید در پیشخوان ببینید که نصب شده، دو جا عدد را عوض کنید:

```php
 * Version: 1.7.15        →   * Version: 1.7.16
define('MRDASTBAAF_BRIDGE_VERSION', '1.7.15');  →  '1.7.16'
```

این فقط برای تشخیص است؛ کارکرد اپ به آن وابسته نیست.

---

## تست

این آدرس را در مرورگر باز کنید (بدون ورود):

```
https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/shop/categories
```

باید چیزی شبیه این ببینید:

```json
{
  "success": true,
  "categories": [
    { "id": 34, "slug": "handmade", "name": "دست‌ساز", "parent": 0, "count": 412 }
  ]
}
```

بعد این را هم امتحان کنید (۳۴ را با یک شناسهٔ واقعی از پاسخ بالا عوض کنید):

```
https://mrdastbaaf.com/wp-json/mrdastbaaf/v1/shop/catalog?category=34&page=1&per_page=24
```

باید ۲۴ محصول و `total` و `totalPages` ببینید.

### اگر خطا داد

| خطا | علت |
|---|---|
| `rest_no_route` | کار ۲-ب انجام نشده — مسیرها ثبت نشده‌اند |
| `Class "MrDastbaaf_Shop_Catalog_Controller" not found` | کار ۲-الف انجام نشده، یا فایل در پوشهٔ درست نیست |
| `woocommerce_missing` | ووکامرس غیرفعال است |
| صفحهٔ سفید | یک خط PHP اشتباه چسبیده — فایل را از بکاپ برگردانید |

---

## برگشت به عقب

فایل جدید را پاک کنید و سه خط را بردارید. هیچ چیز دیگری عوض نشده — نه دیتابیس،
نه قرارداد `/shop/products`، نه وب‌اپ.

---

## چرا فایل جدا

قرارداد `/shop/products` دست نخورده است. وب‌اپ همان‌طور که هست کار می‌کند و اپ
همین حالا هم به آن وصل است. این دو مسیر جدید فقط برای اپ اند، و چون در فایل
جدا هستند، هر آپدیت بعدی پلاگین با آن‌ها تصادم نمی‌کند.
