package com.mrdastbaaf.mdbapp.features.appcontent.domain.repository

import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPage
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPageContent
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.HomeConfig

/**
 * Content the site's owner edits in the WordPress panel: the home hero and the
 * guide pages. None of it is hardcoded in the app, which is the point — the copy
 * changes without shipping a build.
 */
interface AppContentRepository {

    /**
     * The home payload, including the bottom-bar switches.
     *
     * Cached for a minute in memory because three callers ask for it at startup
     * (the shell, the home tab, the account card) and they all want the same
     * bytes. [force] is the pull-to-refresh path.
     */
    suspend fun home(force: Boolean = false): Result<HomeConfig>

    /** Pages this user may see. The server filters by access level. */
    suspend fun pages(): Result<List<AppPage>>

    suspend fun pageContent(pageId: String): Result<AppPageContent>
}
