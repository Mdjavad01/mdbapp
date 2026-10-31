/**
 * @file tests/web/appUiTheme.test.ts
 * @description v1.8.0 — پالت برند از پیشخوان (GET /app-ui → theme)
 *
 * قرارداد: چهار لنگر `#RRGGBB`. مقدار نامعتبر یا غایب باید همان رنگ فعلی را
 * نگه دارد — یک تایپوی رنگ در پنل نباید سایت را بی‌رنگ یا سیاه کند.
 */

import { describe, expect, it } from 'vitest';
import { normalizeAppUi, DEFAULT_APP_UI } from '../../src/services/appContentService';

const DEFAULT_THEME = {
  primary: '#9A3412',
  accent: '#D97706',
  canvas: '#FDFBF7',
  success: '#047857',
};

describe('normalizeAppUi — theme block (v1.8.0)', () => {
  it('ships the current palette as the default', () => {
    expect(DEFAULT_APP_UI.theme).toEqual(DEFAULT_THEME);
    // پاسخ غایب (سرور قدیمی/دمو) → همان چهار رنگ
    expect(normalizeAppUi(null).theme).toEqual(DEFAULT_THEME);
    expect(normalizeAppUi({}).theme).toEqual(DEFAULT_THEME);
  });

  it('takes the panel colours and normalises the case', () => {
    const ui = normalizeAppUi({
      theme: { primary: '#1a2b3c', accent: '#ABCDEF', canvas: '#ffffff', success: '#00aa00' },
    });

    expect(ui.theme).toEqual({
      primary: '#1A2B3C',
      accent: '#ABCDEF',
      canvas: '#FFFFFF',
      success: '#00AA00',
    });
  });

  it('rejects anything that is not a hex colour, anchor by anchor', () => {
    const ui = normalizeAppUi({
      theme: {
        primary: 'red',
        accent: '#12345', // پنج رقم — رنگ نیست
        canvas: '   ', // خالی = پیش‌فرض
        success: '#047857',
      },
    });

    expect(ui.theme.primary).toBe(DEFAULT_THEME.primary);
    expect(ui.theme.accent).toBe(DEFAULT_THEME.accent);
    expect(ui.theme.canvas).toBe(DEFAULT_THEME.canvas);
    // لنگر سالم دست‌نخورده می‌ماند — فال‌بک کلی نیست
    expect(ui.theme.success).toBe('#047857');
  });

  it('accepts the three-digit form', () => {
    const ui = normalizeAppUi({ theme: { primary: '#abc' } });

    expect(ui.theme.primary).toBe('#ABC');
    expect(ui.theme.accent).toBe(DEFAULT_THEME.accent);
  });

  it('ignores a non-object theme block', () => {
    expect(normalizeAppUi({ theme: 'dark' }).theme).toEqual(DEFAULT_THEME);
    expect(normalizeAppUi({ theme: null }).theme).toEqual(DEFAULT_THEME);
  });
});
