package com.mrdastbaaf.mdbapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Deliberately minimal: all wiring lives in Hilt modules so that nothing here
 * needs to change as features land. No analytics, no crash reporter and no
 * third-party SDK is initialised — the security brief rules out anything that
 * could exfiltrate a token, a playback ticket or a video URL.
 */
@HiltAndroidApp
class MdbApplication : Application()
