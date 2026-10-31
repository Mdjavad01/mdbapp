import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { PWA_VERSION } from '../../version';

/**
 * تست خودکار پلیر — از داخل خودِ اپ، بدون احراز هویت.
 *
 * چرا این صفحه: پلیر داخل درس به بلیط، خرید و دسترسی وابسته است، و وقتی خراب
 * می‌شود معلوم نیست تقصیر پلیر است یا جریان بلیط. اینجا یک منیفست عمومی با
 * همان hls.js و همان تنظیمات اجرا می‌شود و هر رویداد در یک لیست ثبت می‌شود.
 *
 * اگر اینجا پخش شود، پلیر سالم است و مشکل در مسیر بلیط/دسترسی است.
 */
const TEST_URL = 'https://dll.mrdastbaaf.com/FA/jetBaft/jetbaft-30/master.m3u8';

export const PlayerSelfTest: React.FC = () => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [lines, setLines] = useState<string[]>([]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      setLines(['video element missing']);
      return;
    }

    const push = (line: string) => setLines((prev) => [...prev, line].slice(-20));

    push(`v${PWA_VERSION} · Hls.isSupported()=${Hls.isSupported()}`);
    push(`native HLS=${video.canPlayType('application/vnd.apple.mpegurl') || 'no'}`);

    if (!Hls.isSupported()) return;

    const hls = new Hls({ enableWorker: false });

    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      push(`MANIFEST_PARSED lv=${hls.levels.length}`);
      video
        .play()
        .then(() => push('play() ok'))
        .catch((error: Error) => push(`play() rejected ${error.name}`));
    });
    hls.on(Hls.Events.ERROR, (_event, data) => {
      push(
        `ERROR ${data.type}/${data.details} fatal=${data.fatal}` +
          (data.response ? ` http=${data.response.code}` : ''),
      );
    });

    hls.loadSource(TEST_URL);
    hls.attachMedia(video);

    const timer = setInterval(() => {
      const duration = Number.isFinite(video.duration) ? video.duration.toFixed(1) : 'inf';
      push(
        `rs=${video.readyState} t=${video.currentTime.toFixed(1)} d=${duration} ` +
          `src=${video.currentSrc ? video.currentSrc.slice(0, 12) : 'none'}`,
      );
    }, 3000);

    return () => {
      clearInterval(timer);
      hls.destroy();
    };
  }, []);

  return (
    <div className="mb-4 rounded-2xl border border-amber-300 bg-amber-50 p-4">
      <p className="mb-2 text-sm font-semibold text-amber-900">
        تست پلیر (منیفست عمومی، بدون بلیط)
      </p>
      <video
        ref={videoRef}
        controls
        muted
        playsInline
        className="mb-2 w-full rounded-xl bg-black"
        style={{ maxHeight: 220 }}
      />
      <pre
        dir="ltr"
        className="max-h-56 overflow-auto rounded-lg bg-stone-900 p-2 text-[10px] leading-relaxed text-stone-200"
      >
        {lines.join('\n')}
      </pre>
    </div>
  );
};
