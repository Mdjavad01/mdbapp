package com.mrdastbaaf.mdbapp.core.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.mrdastbaaf.mdbapp.core.config.FeatureFlags

/**
 * Applies `FLAG_SECURE` for as long as this composable is in the composition.
 *
 * `FLAG_SECURE` does three things at once, all of which the brief requires:
 *  - blocks screenshots and screen recording,
 *  - blanks the app's thumbnail in the recents switcher,
 *  - prevents the content appearing on a non-secure external display.
 *
 * Usage — wrap the content of any screen showing paid material:
 * ```kotlin
 * SecureScreen {
 *     PlayerScreen()
 * }
 * ```
 *
 * The flag is cleared in `onDispose`, so navigating away restores normal
 * behaviour. That matters: forcing it app-wide would break screenshots on the
 * public catalogue too.
 *
 * This is deliberately a `DisposableEffect` rather than a Hilt-managed
 * controller. The flag belongs to a *window*, and the only reliable owner of the
 * current window is the composition that is on screen. A singleton holding an
 * Activity reference would leak and would break the moment the user rotates the
 * device or arrives through a deep link.
 */
@Composable
fun SecureScreen(
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    DisposableEffect(enabled) {
        val window = view.context.findActivity()?.window
        val shouldApply = enabled && FeatureFlags.SECURE_SCREEN_ENABLED && window != null

        if (shouldApply) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        onDispose {
            if (shouldApply) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    content()
}

/**
 * Walks up the `ContextWrapper` chain to find the hosting [Activity].
 *
 * `LocalView.current.context` is not guaranteed to be the Activity — Compose
 * wraps it in themed and lifecycle-aware wrappers depending on the entry point,
 * and a naive `as? Activity` cast silently returns `null` in exactly the cases
 * where protection matters most (an Activity nested in a wrapper, a deep link
 * entry). Unwrapping is the only reliable way to reach the window.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
