/**
 * @file tests/web/color.test.ts
 * @description v1.8.1 — رنگ متن روی سطح برند
 *
 * چرا تست: از وقتی رنگ برند از پیشخوان می‌آید، کارفرما می‌تواند رنگ روشن
 * انتخاب کند. دکمه‌ها متن سفید هاردکد داشتند و روی زرد خوانا نبودند؛ این تابع
 * تنها چیزی است که جلوی آن را می‌گیرد و هیچ خطای رانتیای هم نمی‌دهد.
 */

import { describe, expect, it } from 'vitest';
import {
  contrastRatio,
  darkenUntilReadable,
  hexToRgb,
  mixHex,
  readableOn,
  relativeLuminance,
  INK,
  PAPER,
} from '../../src/utils/color';

describe('hexToRgb', () => {
  it('accepts 6-digit and 3-digit forms, with or without #', () => {
    expect(hexToRgb('#9A3412')).toEqual([154, 52, 18]);
    expect(hexToRgb('9a3412')).toEqual([154, 52, 18]);
    expect(hexToRgb('#fff')).toEqual([255, 255, 255]);
  });

  it('rejects anything that is not a colour', () => {
    expect(hexToRgb('red')).toBeNull();
    expect(hexToRgb('#12345')).toBeNull();
    expect(hexToRgb('')).toBeNull();
  });
});

describe('contrastRatio', () => {
  it('is 21 for black on white and 1 for a colour on itself', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 1);
    expect(contrastRatio('#9A3412', '#9A3412')).toBeCloseTo(1, 5);
  });
});

describe('readableOn', () => {
  it('keeps white text on the default terracotta', () => {
    expect(readableOn('#9A3412')).toBe(PAPER);
  });

  it('flips to dark text on a light primary', () => {
    // زرد روشن: سفید روی آن ~۱.۴:۱ است، جوهر ~۱۵:۱
    expect(readableOn('#FFD400')).toBe(INK);
    expect(readableOn('#FDFBF7')).toBe(INK);
  });

  it('keeps white on a dark primary of any hue', () => {
    expect(readableOn('#0F4C5C')).toBe(PAPER);
    expect(readableOn('#111827')).toBe(PAPER);
  });

  it('falls back to white for an invalid colour instead of guessing', () => {
    expect(readableOn('not-a-colour')).toBe(PAPER);
    expect(relativeLuminance('not-a-colour')).toBeNull();
  });
});

describe('darkenUntilReadable', () => {
  it('leaves a colour that already passes untouched (the default palette does not shift)', () => {
    // #9A3412 روی کرم ~۸:۱ است؛ نباید دست بخورد
    expect(darkenUntilReadable('#9A3412', '#FDFBF7', 4.5)).toBe('#9A3412');
    expect(darkenUntilReadable('#9A3412', '#FDFBF7', 7)).toBe('#9A3412');
  });

  it('darkens a light primary until the link and heading ratios pass', () => {
    const link = darkenUntilReadable('#FFD400', '#FFFFFF', 4.5);
    const heading = darkenUntilReadable('#FFD400', '#FFFFFF', 7);

    expect(contrastRatio(link, '#FFFFFF')!).toBeGreaterThanOrEqual(4.5);
    expect(contrastRatio(heading, '#FFFFFF')!).toBeGreaterThanOrEqual(7);
    // تیتر از لینک تیره‌تر است، وگرنه سلسله‌مراتب از دست می‌رود
    expect(relativeLuminance(heading)!).toBeLessThan(relativeLuminance(link)!);
  });

  it('returns the input for an invalid colour instead of guessing', () => {
    expect(darkenUntilReadable('nonsense', '#FFFFFF', 4.5)).toBe('nonsense');
  });
});

describe('mixHex', () => {
  it('interpolates and clamps', () => {
    expect(mixHex('#000000', '#FFFFFF', 0.5)).toBe('#808080');
    expect(mixHex('#000000', '#FFFFFF', 0)).toBe('#000000');
    expect(mixHex('#000000', '#FFFFFF', 5)).toBe('#FFFFFF');
  });
});
