package com.mrdastbaaf.mdbapp.features.player.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.security.SecureScreen
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.ExternalLinks
import com.mrdastbaaf.mdbapp.features.player.domain.model.PlaybackSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The lesson player.
 *
 * Wrapped in [SecureScreen], so `FLAG_SECURE` is on for exactly as long as this
 * screen is visible: no screenshots, no screen recording, and a blank recents
 * thumbnail.
 *
 * The watermark is drawn **client-side**. It deters re-recording and attributes
 * a leak, but it is not content protection — a modified client can simply not
 * draw it, and nothing here changes that. The honest limit is documented in the
 * README; the real fix is server-side signed segment URLs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SecureScreen {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.player_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding),
            ) {
                when (val current = state) {
                    PlayerUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                    )

                    is PlayerUiState.Error -> PlayerError(
                        failure = current.failure,
                        onRetry = viewModel::open,
                        onOpenMembership = {
                            ExternalLinks.open(context, ExternalLinks.MEMBERSHIP_URL)
                        },
                    )

                    is PlayerUiState.Ready -> VideoSurface(
                        session = current.session,
                        startPositionMillis = current.startPositionMillis,
                        onProgress = viewModel::onPositionChanged,
                        onStopped = viewModel::onStopped,
                    )
                }
            }
        }
    }
}

/**
 * Media3 marks part of its UI surface `@UnstableApi`, which is an opt-in error
 * rather than a warning — without this the module does not compile.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    session: PlaybackSession,
    startPositionMillis: Long,
    onProgress: (positionMillis: Long, durationMillis: Long) -> Unit,
    onStopped: (positionMillis: Long, durationMillis: Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keyed on the URL: a new source (a different quality, or a re-issued
    // ticket) builds a new player rather than reusing one that holds the old
    // media item.
    val exoPlayer = remember(session.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(session.videoUrl))
            if (startPositionMillis > 0L) seekTo(startPositionMillis)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        // A video that keeps playing in the background is both a battery problem
        // and an access problem: the ticket is 15 minutes long, and audio-only
        // playback after the user left the screen is not something they asked
        // for.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onStopped(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            delay(PROGRESS_TICK_MILLIS)
            if (exoPlayer.isPlaying) {
                onProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    useController = true
                    keepScreenOn = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            // The factory runs once; without this, a new source would keep the
            // old, already-released player attached to the view.
            update = { view -> view.player = exoPlayer },
            modifier = Modifier.fillMaxSize(),
        )

        if (session.watermarkText.isNotBlank()) {
            Text(
                text = session.watermarkText,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.md),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PlayerError(
    failure: Failure,
    onRetry: () -> Unit,
    onOpenMembership: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = failure.userMessage ?: stringResource(R.string.player_error_generic),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // A 403 is not a bug — the server is saying "this lesson needs a
        // subscription". Offering retry there would just loop.
        if (failure is Failure.Forbidden) {
            Button(
                onClick = onOpenMembership,
                modifier = Modifier.padding(top = Spacing.md),
            ) {
                Text(stringResource(R.string.access_renew))
            }
        } else if (failure.isRetryable || failure is Failure.Unauthorized) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = Spacing.md),
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

/** How often the playback position is read from the player. */
private const val PROGRESS_TICK_MILLIS = 5_000L
