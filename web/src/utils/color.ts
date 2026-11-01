/**
 * @file utils/color.ts
 * @description v1.8.1 — ابزارهای رنگ برای پالت پنل
 *
 * چرا لازم شد: از ۱.۸.۰ رنگ برند از پیشخوان می‌آید. دکمه‌های سایت متن سفید
 * دارند؛ اگر کارفرما رنگ روشن انتخاب کند (زرد/نارنجی کمرنگ) متن ناخوانا
 * می‌شود و هیچ خطایی هم رخ نمی‌دهد. اینجا از روشنایی رنگ، رنگ متن مناسب
 * انتخاب می‌شود.
 */

/** `#RGB` یا `#RRGGBB` → [r, g, b] در بازهٔ ۰..۲۵۵؛ ورودی نامعتبر → null. */
export function hexToRgb(hex: string): [number, number, number] | null {
  if (typeof hex !== 'string') return null;
  const raw = hex.trim().replace(/^#/, '');
  const full = raw.length === 3 ? raw.split('').map((c) => c + c).join('') : raw;
  if (!/^[0-9a-fA-F]{6}$/.test(full)) return null;
  return [
    parseInt(full.slice(0, 2), 16),
    parseInt(full.slice(2, 4), 16),
    parseInt(full.slice(4, 6), 16),
  ];
}

/** روشنایی نسبی WCAG (۰ = سیاه، ۱ = سفید). */
export function relativeLuminance(hex: string): number | null {
  const rgb = hexToRgb(hex);
  if (!rgb) return null;
  const [r, g, b] = rgb.map((v) => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** نسبت کنتراست دو رنگ (۱..۲۱). */
export function contrastRatio(a: string, b: string): number | null {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  if (la === null || lb === null) return null;
  const light = Math.max(la, lb);
  const dark = Math.min(la, lb);
  return (light + 0.05) / (dark + 0.05);
}

/** متن روی سطح روشن/تیره — همان کرم/جوهر برند، نه سیاه مطلق. */
export const INK = '#1C1917';
export const PAPER = '#FFFFFF';

/** ترکیب خطی دو رنگ hex با وزن t (۰ = a، ۱ = b). */
export function mixHex(a: string, b: string, t: number): string {
  const ra = hexToRgb(a);
  const rb = hexToRgb(b);
  if (!ra || !rb) return a;
  const w = Math.min(1, Math.max(0, t));
  const out = ra.map((v, i) => Math.round(v * (1 - w) + rb[i] * w));
  return '#' + out.map((v) => v.toString(16).padStart(2, '0')).join('').toUpperCase();
}

/**
 * رنگ را به سمت جوهر تیره می‌کند تا روی `surface` به نسبت کنتراست خواسته‌شده
 * برسد. اگر از اول کافی باشد، همان رنگ دست‌نخورده برمی‌گردد — یعنی پالت
 * پیش‌فرض هیچ تغییری نمی‌بیند و فقط رنگ‌های روشن اصلاح می‌شوند.
 */
export function darkenUntilReadable(colour: string, surface: string, minRatio = 4.5): string {
  if (contrastRatio(colour, surface) === null) return colour;
  for (let w = 0; w <= 1.0001; w += 0.02) {
    const candidate = mixHex(colour, INK, w);
    const ratio = contrastRatio(candidate, surface);
    if (ratio !== null && ratio >= minRatio) return candidate;
  }
  return INK;
}

/**
 * رنگ متن روی یک سطح: هر کدام از سفید/جوهر که کنتراست بیشتری دارد.
 * ورودی نامعتبر → سفید (همان رفتار پیش‌فرضِ امروز، نه یک رنگ تصادفی).
 */
export function readableOn(background: string): string {
  const withWhite = contrastRatio(background, PAPER);
  const withInk = contrastRatio(background, INK);
  if (withWhite === null || withInk === null) return PAPER;
  return withWhite >= withInk ? PAPER : INK;
}
