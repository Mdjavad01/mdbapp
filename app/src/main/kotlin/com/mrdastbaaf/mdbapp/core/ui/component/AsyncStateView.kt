package com.mrdastbaaf.mdbapp.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.error.Failure
import com.mrdastbaaf.mdbapp.core.ui.state.UiState
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing

/**
 * Renders a [UiState] with all four required states handled.
 *
 * The point of this component is that a screen cannot accidentally skip a state:
 * the caller supplies only the success case, and loading / empty / error /
 * offline are rendered here, consistently, with the right Persian copy and a
 * retry affordance where retrying is meaningful.
 *
 * [isOffline] is a separate parameter rather than a `UiState` member so that a
 * screen showing cached data stays usable while offline — the offline notice is
 * then a banner above the content rather than a replacement for it.
 */
@Composable
fun <T> AsyncStateView(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            UiState.Loading -> LoadingState()

            is UiState.Empty -> MessageState(
                icon = null,
                title = stringResource(R.string.state_empty_title),
                message = state.message ?: emptyMessage ?: stringResource(R.string.state_empty_message),
                onRetry = null,
            )

            is UiState.Error -> MessageState(
                icon = null,
                title = if (isOffline) {
                    stringResource(R.string.state_offline_title)
                } else {
                    stringResource(R.string.state_error_title)
                },
                message = state.failure.userMessage
                    ?: if (isOffline) {
                        stringResource(R.string.state_offline_message)
                    } else {
                        stringResource(R.string.state_error_message)
                    },
                onRetry = onRetry.takeIf { state.failure.isRetryable || isOffline },
            )

            is UiState.Success -> content(state.data)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.state_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageState(
    icon: ImageVector?,
    title: String,
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

@Preview(name = "خطا", showBackground = true)
@Composable
private fun AsyncStateErrorPreview() {
    MdbTheme {
        AsyncStateView(
            state = UiState.Error(Failure.NoConnection()),
            onRetry = {},
            content = {},
        )
    }
}

@Preview(name = "خالی", showBackground = true)
@Composable
private fun AsyncStateEmptyPreview() {
    MdbTheme {
        AsyncStateView(
            state = UiState.Empty(),
            content = {},
        )
    }
}
