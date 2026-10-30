package com.mrdastbaaf.mdbapp.features.appcontent.data

import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.network.MdbApi
import com.mrdastbaaf.mdbapp.core.network.safeApiCall
import com.mrdastbaaf.mdbapp.core.util.HtmlText
import com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto.AppPageContentDto
import com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto.AppUiDto
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppMenuItem
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPage
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppPageContent
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.Hero
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.HeroStat
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.HomeConfig
import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The home configuration and the guide pages.
 *
 * The page body is HTML from the WordPress editor. It is stripped to plain text
 * rather than rendered in a WebView: a page is prose, the app has no business
 * executing the site's scripts, and a text view keeps the page inside the app's
 * own typography and dark theme.
 */
/** One minute: long enough to deduplicate a launch, short enough to feel live. */
private const val HOME_TTL_MILLIS = 60_000L

@Singleton
class AppContentRepositoryImpl @Inject constructor(
    private val api: MdbApi,
    private val json: Json,
) : AppContentRepository {

    /**
     * Guards against a thundering herd on launch.
     *
     * The shell, the home tab and the account card all ask for `/app-ui` within
     * the same second. Without this lock each one would open its own request —
     * three times the load on the server for one screen, multiplied by every
     * install. The second and third callers wait, then read the cache.
     */
    private val homeMutex = Mutex()
    @Volatile private var cachedHome: HomeConfig? = null
    @Volatile private var cachedAtMillis: Long = 0L

    override suspend fun home(force: Boolean): Result<HomeConfig> = homeMutex.withLock {
        val fresh = cachedHome
        val age = System.currentTimeMillis() - cachedAtMillis
        if (!force && fresh != null && age in 0 until HOME_TTL_MILLIS) {
            return Result.success(fresh)
        }

        safeApiCall(json) { api.getAppUi() }
            .mapCatching { dto -> dto.toDomain() }
            .mapCatching { config ->
                // A home with no headline is not a home — treat it as a broken
                // response rather than drawing an empty gradient block.
                if (config.hero == null) {
                    throw Failure.InvalidResponse("محتوای صفحهٔ اصلی از سرور دریافت نشد.")
                }
                config
            }
            .onSuccess {
                cachedHome = it
                cachedAtMillis = System.currentTimeMillis()
            }
    }

    override suspend fun pages(): Result<List<AppPage>> =
        safeApiCall(json) { api.getAppPages() }
            .mapCatching { dtos ->
                dtos.mapNotNull { dto ->
                    val id = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val title = HtmlText.strip(dto.title).takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null

                    AppPage(
                        id = id,
                        title = title,
                        access = dto.access.orEmpty(),
                    )
                }
            }

    override suspend fun pageContent(pageId: String): Result<AppPageContent> =
        safeApiCall(json) { api.getAppPageContent(pageId) }
            .mapCatching { dto ->
                dto.toDomain() ?: throw Failure.InvalidResponse(
                    "محتوای این صفحه از سرور دریافت نشد.",
                )
            }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun AppUiDto.toDomain(): HomeConfig {
        val heroDto = hero
        val heroDomain = heroDto?.let { dto ->
            val headline = HtmlText.strip(dto.headline)
            if (headline.isEmpty()) return@let null

            Hero(
                badgeText = HtmlText.strip(dto.badgeText),
                headline = headline,
                subheadline = HtmlText.strip(dto.subheadline),
                ctaPrimaryText = HtmlText.strip(dto.ctaPrimaryText),
                ctaSecondaryText = HtmlText.strip(dto.ctaSecondaryText),
                stats = listOfNotNull(
                    stat(dto.stat1Value, dto.stat1Label),
                    stat(dto.stat2Value, dto.stat2Label),
                    stat(dto.stat3Value, dto.stat3Label),
                ),
            )
        }

        val featured = sections.firstOrNull { it.key == "featured_courses" && it.enabled != false }
        val physical = sections.firstOrNull { it.key == "physical_products" }

        return HomeConfig(
            hero = heroDomain,
            featuredTitle = HtmlText.strip(featured?.title).takeIf { it.isNotEmpty() },
            featuredSubtitle = HtmlText.strip(featured?.subtitle).takeIf { it.isNotEmpty() },
            featuredCourseIds = featuredCourseIds.filter { it > 0 },
            dock = (menu?.dock ?: emptyList()).mapNotNull { item ->
                val key = item.key?.trim().orEmpty()
                if (key.isEmpty()) return@mapNotNull null
                AppMenuItem(
                    key = key,
                    label = HtmlText.strip(item.label),
                    // Absent means "not switched off": an older bridge that
                    // does not send the flag must not hide a tab.
                    enabled = item.enabled != false,
                )
            },
            shopEnabled = physical?.enabled != false,
        )
    }

    private fun stat(value: String?, label: String?): HeroStat? {
        val cleanValue = HtmlText.strip(value)
        val cleanLabel = HtmlText.strip(label)
        if (cleanValue.isEmpty() && cleanLabel.isEmpty()) return null
        return HeroStat(value = cleanValue, label = cleanLabel)
    }

    private fun AppPageContentDto.toDomain(): AppPageContent? {
        val pageId = id?.takeIf { it.isNotBlank() } ?: return null
        val body = HtmlText.strip(html)
        if (body.isEmpty()) return null

        return AppPageContent(
            id = pageId,
            title = HtmlText.strip(title),
            body = body,
            updatedAtJalali = updatedAtJalali.orEmpty(),
        )
    }
}
