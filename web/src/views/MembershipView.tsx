/**
 * @file views/MembershipView.tsx
 * @description Configurable membership plans view with manual renewal transparency and tier matrix
 */

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Crown, CheckCircle, HelpCircle, ArrowLeft, AlertCircle, RefreshCw } from 'lucide-react';
import { MembershipPlan, UserProfile } from '../types';
import { membershipService, planDurationToDays } from '../services/membershipService';
import { formatJalali, daysUntil, formatToman } from '../utils/format';
import { AsyncState } from '../components/common/AsyncState';
import { useOnlineRefetch } from '../hooks/useOnlineRefetch';

interface MembershipViewProps {
  onSelectPlan: (plan: MembershipPlan) => void;
  currentUser: UserProfile | null;
  // BUG-47: widened so editorial navigation can carry a slug param
  // (onNavigate('editorial', { slug: 'guide-membership' })).
  onNavigate: (view: string, params?: { slug?: string }) => void;
}

export const MembershipView: React.FC<MembershipViewProps> = ({
  onSelectPlan,
  currentUser,
  onNavigate,
}) => {
  const [plans, setPlans] = useState<MembershipPlan[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const loadPlans = useCallback(() => {
    setIsLoading(true);
    setLoadError(false);
    // BUG-43c: a rejected load previously left the plans section blank forever
    // (no catch) — now it lands on an error card with a retry button.
    membershipService
      .getActivePlans()
      .then((res) => {
        setPlans(res);
        setIsLoading(false);
      })
      .catch(() => {
        setLoadError(true);
        setIsLoading(false);
      });
  }, []);

  useEffect(() => {
    loadPlans();
  }, [loadPlans]);

  // v1.3.0 — بازگشت اتصال → یک refetch بی‌صدا وقتی آخرین بار خطا خورده بود
  useOnlineRefetch(loadError, loadPlans);

  const formatPrice = (p: number) => p.toLocaleString('fa-IR');

  /**
   * v1.8.0 — نردبان مدت، همان الگوی اپ اندروید.
   *
   * پلن‌ها فقط در «مدت» تفاوت دارند، پس مدت سرتیتر کارت است و عنوان پنل زیرنویس.
   * ترتیب بر اساس روز (نه قیمت) است: یک نردبان ۱۲ ← ۱ ← ۶ ماهه خوانا نیست.
   * «٪ ارزان‌تر» نسبت به کوتاه‌ترین پلن محاسبه می‌شود، نه به یک عدد ثابت.
   */
  const ladder = useMemo(() => {
    return plans
      .map((plan) => ({ plan, days: planDurationToDays(plan.duration) }))
      .sort((a, b) => a.days - b.days);
  }, [plans]);

  const perMonthOf = (priceToman: number, days: number): number | null =>
    days > 0 ? Math.round((priceToman * 30) / days) : null;

  // مبنا = کوتاه‌ترین پلن **پرداختی**، نه پلن رایگان: رایگان قیمت صفر دارد و
  // ماهانه‌اش صفر می‌شود، پس هر درصدی نسبت به آن بی‌معنا (و تقسیم بر صفر) است.
  const baseline = ladder.find((row) => row.days > 0 && row.plan.priceToman > 0);
  const baselinePerMonth = baseline ? perMonthOf(baseline.plan.priceToman, baseline.days) : null;

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 space-y-10 pb-28">
      {/* Page Header */}
      <div className="text-center max-w-2xl mx-auto space-y-3">
        <div className="inline-flex items-center gap-2 bg-amber-100 text-amber-900 px-3 py-1 rounded-full text-xs font-semibold">
          <Crown className="w-3.5 h-3.5 text-amber-700" />
          <span>پلن‌های عضویت و دسترسی به ویدیوها</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-amber-950">
          سرمایه‌گذاری روی هنر دست و مهارت فردی
        </h1>
        <p className="text-xs sm:text-sm text-stone-600 leading-relaxed">
          با انتخاب یکی از پلن‌های زیر، دسترسی نامحدود به درس‌های ویدیویی، فایل‌های نقشه‌ها و پشتیبانی اساتید را دریافت کنید.
        </p>
      </div>

      {/* Manual Renewal Transparency Banner */}
      <div className="bg-amber-50/80 border border-amber-300/80 rounded-2xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-800 text-white flex items-center justify-center shrink-0">
            <RefreshCw className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-xs sm:text-sm text-amber-950">
              قانون تمدید دستی مستر دستباف (بدون تمدید خودکار)
            </h3>
            <p className="text-[11px] sm:text-xs text-stone-600 mt-0.5 leading-relaxed">
              تمدید عضویت کاملاً وابسته به اراده شماست. هیچ وجهی پس از پایان دوره به صورت خودکار از کارت شما برداشت نخواهد شد.
            </p>
          </div>
        </div>
        <button
          // BUG-47: «مطالعه راهنما» previously navigated to the bare 'editorial'
          // view (defaulting to about-us). It now carries the correct slug.
          onClick={() => onNavigate('editorial', { slug: 'guide-membership' })}
          className="text-xs font-semibold text-amber-900 underline whitespace-nowrap"
        >
          مطالعه راهنما
        </button>
      </div>

      {/* Active User Status Banner if already subscribed */}
      {currentUser?.membership && (
        <div className="bg-white border border-stone-200 rounded-2xl p-4 flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-3">
            {/* LIVE STATUS FIX (BUG-18): expired memberships no longer show a
                pulsing green dot and «فعال» — they show an amber expired state. */}
            {membershipService.isMembershipLive(currentUser) ? (
              <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse" />
            ) : (
              <span className="w-3 h-3 rounded-full bg-amber-500" />
            )}
            <div>
              <p className="font-bold text-xs text-stone-900">
                اشتراک فعلی شما: <span className="text-amber-800">{currentUser.membership.planName}</span>
              </p>
              {membershipService.isMembershipLive(currentUser) ? (
                <p className="text-[11px] text-stone-500">
                  {/* BUG-48: daysUntil returns number|null — null renders as «—»,
                      never as «۰ روز». */}
                  تاریخ انقضا: {formatJalali(currentUser.membership.expiresAt)} ({daysUntil(currentUser.membership.expiresAt) ?? '—'} روز باقی‌مانده)
                </p>
              ) : (
                <p className="text-[11px] text-amber-700 font-semibold">
                  این اشتراک منقضی شده است — برای دسترسی مجدد تمدید کنید.
                </p>
              )}
            </div>
          </div>
          <button
            onClick={() => onNavigate('dashboard')}
            className="text-xs text-stone-700 bg-stone-100 hover:bg-stone-200 px-3 py-1.5 rounded-xl transition font-medium"
          >
            مشاهده داشبورد
          </button>
        </div>
      )}

      {/* Plans Grid */}
      {isLoading ? (
        <AsyncState status="loading" loadingLabel="در حال بارگذاری پلن‌ها..." />
      ) : loadError ? (
        // BUG-43c: explicit error card with retry
        <AsyncState
          status="error"
          error="بارگذاری پلن‌های عضویت با خطا مواجه شد."
          onRetry={loadPlans}
        />
      ) : plans.length === 0 ? (
        // BUG-43c: an empty plans store is an explicit empty state, not a blank page
        <AsyncState
          status="empty"
          emptyTitle="در حال حاضر پلنی فعال نیست"
          emptyDescription="پلن‌های عضویت به‌زودی اعلام می‌شوند؛ لطفاً بعداً مراجعه کنید یا با پشتیبانی در تماس باشید."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {ladder.map(({ plan, days }) => {
            const isFree = plan.priceToman === 0;
            const isUserCurrentTier = currentUser?.membership?.tier === plan.tier;
            const perMonth = perMonthOf(plan.priceToman, days);
            const savings =
              !isFree && baseline && baselinePerMonth && perMonth && days > baseline.days
                ? Math.round((1 - perMonth / baselinePerMonth) * 100)
                : null;
            // مدت، سرتیتر است؛ اگر پنل همان واژه را در هر دو فیلد نوشته باشد،
            // تکرار چاپ نمی‌شود (همان قاعدهٔ PlanCard اپ).
            const headline = plan.durationLabel.trim() !== '' ? plan.durationLabel : plan.title;
            const subtitle = plan.title !== headline ? plan.title : null;

            return (
              <div
                key={plan.id}
                className={`bg-white rounded-3xl p-5 border flex flex-col justify-between transition duration-200 ${
                  plan.isPopular
                    ? 'border-amber-600 shadow-lg ring-1 ring-amber-600/30'
                    : 'border-stone-200 hover:border-stone-300 shadow-xs'
                }`}
              >
                <div>
                  {plan.isPopular && (
                    <div className="bg-amber-700 text-white text-[10px] font-bold px-2.5 py-0.5 rounded-full inline-block mb-3">
                      محبوب‌ترین انتخاب هنرجویان
                    </div>
                  )}

                  <h3 className="font-black text-lg text-amber-950 leading-tight">{headline}</h3>
                  {subtitle && (
                    <p className="text-[11px] text-stone-500 mt-0.5 leading-snug">{subtitle}</p>
                  )}

                  {plan.description && (
                    <p className="text-xs text-stone-500 mt-2 min-h-10 leading-relaxed">
                      {plan.description}
                    </p>
                  )}

                  {/* Price */}
                  <div className="my-4 py-3 border-y border-stone-100">
                    {plan.originalPriceToman && (
                      <div className="text-[11px] text-stone-400 line-through font-mono">
                        {formatPrice(plan.originalPriceToman)} تومان
                      </div>
                    )}
                    <div className="flex items-baseline gap-1 mt-0.5">
                      <span className="text-xl sm:text-2xl font-black text-amber-900 font-mono">
                        {isFree ? 'رایگان' : formatPrice(plan.priceToman)}
                      </span>
                      {!isFree && <span className="text-xs text-stone-500">تومان</span>}
                    </div>
                    {/* ماهانه — تنها راه مقایسهٔ منصفانهٔ پلن‌های مختلف‌مدت */}
                    {!isFree && perMonth && (
                      <p className="text-[11px] text-stone-500 mt-1">
                        معادل {formatToman(perMonth)} تومان در ماه
                      </p>
                    )}
                    {savings !== null && savings >= 1 && (
                      <span className="inline-block mt-2 text-[10px] font-bold text-emerald-800 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full">
                        {savings.toLocaleString('fa-IR')}٪ ارزان‌تر از پلن یک‌ماهه
                      </span>
                    )}
                  </div>

                  {/* Feature Checklist */}
                  {plan.features.length > 0 && (
                    <div className="space-y-2 text-xs text-stone-700">
                      <p className="font-semibold text-[11px] text-stone-500">امکانات دسترسی:</p>
                      {plan.features.map((feat, idx) => (
                        <div key={idx} className="flex items-start gap-2">
                          <CheckCircle className="w-3.5 h-3.5 text-amber-700 shrink-0 mt-0.5" />
                          <span className="leading-snug">{feat}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Purchase Button */}
                <div className="pt-6">
                  <button
                    onClick={() => onSelectPlan(plan)}
                    className={`w-full py-2.5 rounded-xl text-xs font-bold transition shadow-xs ${
                      plan.isPopular
                        ? 'bg-amber-800 hover:bg-amber-900 text-white'
                        : 'bg-amber-800 hover:bg-amber-900 text-white'
                    }`}
                  >
                    {isFree
                      ? 'عضویت رایگان'
                      : isUserCurrentTier
                      ? 'تمدید مجدد دستی'
                      : 'خرید و ارتقای عضویت'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/*
        v1.8.0 — جدول مقایسهٔ برنزی/نقره‌ای/طلایی حذف شد (خواستهٔ کارفرما؛ همان
        تغییری که ۱.۴.۰ فقط برای اندروید انجام داده بود). پلن‌ها اکنون فقط در
        «مدت» تفاوت دارند و همین بالاتر با نردبان مدت نمایش داده می‌شود؛ جدولی
        که سه سطح را مقایسه می‌کرد با فروش پلن‌های مدت‌دار نمی‌خواند.
      */}
    </div>
  );
};
