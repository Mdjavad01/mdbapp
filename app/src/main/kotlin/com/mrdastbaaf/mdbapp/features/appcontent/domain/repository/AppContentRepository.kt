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

    suspend fun home(): Result<HomeConfig>

    /** Pages this user may see. The server filters by access level. */
    suspend fun pages(): Result<List<AppPage>>

    suspend fun pageContent(pageId: String): Result<AppPageContent>
}
