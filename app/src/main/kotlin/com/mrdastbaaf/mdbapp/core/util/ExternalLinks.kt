package com.mrdastbaaf.mdbapp.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Destinations that leave the app.
 *
 * Subscriptions are sold on the website, not through Play, so every purchase
 * and renewal action has to hand off to the browser. Keeping the URLs here
 * means a change of host or route is one edit, not a search across screens.
 */
object ExternalLinks {

    /**
     * Where "تمدید یا خرید اشتراک" goes. Fixed by the brief: the app never sells
     * a subscription itself.
     */
    const val MEMBERSHIP_URL = "https://app.mrdastbaaf.com/#/membership"

    /** Opens [url] in whatever the user has configured as a browser. */
    fun open(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // A device with no browser at all is rare but possible; failing to open
        // one must not crash the screen the user is looking at.
        runCatching { context.startActivity(intent) }
    }
}
