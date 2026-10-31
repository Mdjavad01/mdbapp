package com.mrdastbaaf.mdbapp.features.appcontent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrdastbaaf.mdbapp.core.ui.theme.BrandPalette
import com.mrdastbaaf.mdbapp.features.appcontent.domain.model.AppTheme
import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The palette the panel states, as the app's live theme.
 *
 * Colours used to be four constants compiled into the app: changing the brand
 * meant a code change and a release. `GET /app-ui` now carries a `theme` block,
 * so the owner recolours the app from the WordPress panel.
 *
 * It rides on the existing `/app-ui` request rather than a new endpoint — the
 * repository caches that payload for a minute and the shell asks for it at
 * startup anyway, so this costs no extra round trip.
 *
 * Failure is deliberately silent: offline, an older bridge, or a broken
 * payload all leave [brand] at [BrandPalette.Default]. An app that cannot read
 * its palette should look like yesterday's app, not like a blank screen.
 */
@HiltViewModel
class BrandViewModel @Inject constructor(
    private val repository: AppContentRepository,
) : ViewModel() {

    private val _brand = MutableStateFlow(BrandPalette.Default)
    val brand: StateFlow<BrandPalette> = _brand.asStateFlow()

    init {
        viewModelScope.launch {
            val theme = repository.home().getOrNull()?.theme ?: return@launch
            _brand.value = theme.toPalette()
        }
    }
}

/**
 * Hex → palette, anchor by anchor.
 *
 * A single bad value falls back to that one anchor rather than to the whole
 * default palette: one typo in the panel must not repaint the app in a
 * half-new, half-old mix of colours.
 */
internal fun AppTheme.toPalette(): BrandPalette {
    val default = BrandPalette.Default
    return BrandPalette(
        primary = BrandPalette.parseHex(primary) ?: default.primary,
        accent = BrandPalette.parseHex(accent) ?: default.accent,
        canvas = BrandPalette.parseHex(canvas) ?: default.canvas,
        success = BrandPalette.parseHex(success) ?: default.success,
    )
}
