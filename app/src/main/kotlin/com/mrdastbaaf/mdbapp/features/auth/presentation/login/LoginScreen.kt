package com.mrdastbaaf.mdbapp.features.auth.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AuthHeader
import com.mrdastbaaf.mdbapp.core.ui.component.GradientButton
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing

/**
 * Step one of sign-in: the phone number.
 *
 * The field never rewrites what the user types — Persian digits are accepted
 * as-is and normalised on submit by `PhoneNormalizer`, which is the same
 * conversion the server requires (`^09[0-9]{9}$`, ASCII only).
 *
 * The `contentType = PhoneNumber` hint lets the platform offer the user's own
 * number from the autofill sheet instead of making them type it.
 */
@Composable
fun LoginScreen(
    onOtpRequested: (phone: String, isRegistered: Boolean, smsWarning: String?) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.OtpRequested -> {
                    keyboard?.hide()
                    onOtpRequested(event.phone, event.isRegistered, event.smsWarning)
                }
            }
        }
    }

    LoginContent(
        state = state,
        onPhoneChange = viewModel::onPhoneChange,
        onSubmit = {
            keyboard?.hide()
            viewModel.onSubmit()
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContent(
    state: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    // One entrance animation, played once. The card rises into place rather
    // than appearing, which is what makes the first frame feel finished.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        AuthHeader(
            title = stringResource(R.string.auth_brand_name),
            subtitle = stringResource(R.string.auth_login_subtitle),
        )

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(durationMillis = 320)) +
                slideInVertically(tween(durationMillis = 420)) { height -> height / 8 },
        ) {
            Column(
                modifier = Modifier
                    .offset(y = (-Spacing.xxl))
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Text(
                            text = stringResource(R.string.auth_login_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )

                        OutlinedTextField(
                            value = state.phoneInput,
                            onValueChange = onPhoneChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentType = ContentType.PhoneNumber },
                            label = { Text(stringResource(R.string.auth_phone_label)) },
                            placeholder = { Text(stringResource(R.string.auth_phone_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            isError = state.errorMessage != null,
                            textStyle = MaterialTheme.typography.titleMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                        )

                        AnimatedVisibility(visible = state.errorMessage != null) {
                            Text(
                                text = state.errorMessage?.asString().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        GradientButton(
                            text = stringResource(R.string.auth_request_code),
                            onClick = onSubmit,
                            enabled = state.canSubmit,
                            isLoading = state.isSubmitting,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.auth_login_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm),
                )

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Preview(name = "ورود", showBackground = true, locale = "fa")
@Composable
private fun LoginPreview() {
    MdbTheme {
        LoginContent(
            state = LoginUiState(phoneInput = "09123456789"),
            onPhoneChange = {},
            onSubmit = {},
        )
    }
}

@Preview(name = "ورود — خطا", showBackground = true, locale = "fa")
@Composable
private fun LoginErrorPreview() {
    MdbTheme {
        LoginContent(
            state = LoginUiState(
                phoneInput = "123",
                errorMessage = UiMessage.Res(R.string.auth_phone_invalid),
            ),
            onPhoneChange = {},
            onSubmit = {},
        )
    }
}
