package com.mrdastbaaf.mdbapp.features.auth.presentation.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.component.AuthHeader
import com.mrdastbaaf.mdbapp.core.ui.component.GradientButton
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat
import kotlin.math.roundToInt

/**
 * Step two of sign-in: the OTP, plus a name when the account is new.
 *
 * The code is typed into a hidden field behind [DigitRow] rather than into six
 * separate boxes: one field keeps paste, autofill and IME composition working,
 * which six focus-chasing fields quietly break. The name fields appear only for
 * a new account — the server tells us which case we are in via `isRegistered`,
 * and it will *not* reject a request without a name (it silently falls back to
 * «هنرجو NNNN»), so the client is the only place that can insist.
 */
@Composable
fun OtpScreen(
    onSignedIn: () -> Unit,
    onChangePhone: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OtpEvent.SignedIn -> {
                    keyboard?.hide()
                    onSignedIn()
                }

                OtpEvent.ChangePhone -> {
                    keyboard?.hide()
                    onChangePhone()
                }
            }
        }
    }

    OtpContent(
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onSubmit = {
            keyboard?.hide()
            viewModel.onSubmit()
        },
        onResend = viewModel::onResend,
        onChangePhone = viewModel::onChangePhone,
    )
}

@Composable
private fun OtpContent(
    state: OtpUiState,
    onCodeChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onChangePhone: () -> Unit,
) {
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
            title = if (state.isNewAccount) {
                stringResource(R.string.auth_signup_title)
            } else {
                stringResource(R.string.auth_otp_label)
            },
            subtitle = if (state.isNewAccount) {
                stringResource(R.string.auth_signup_subtitle)
            } else {
                stringResource(R.string.auth_otp_sent_to, state.displayPhone)
            },
            showLogo = false,
            onBack = onChangePhone,
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
                        DigitRow(
                            code = state.code,
                            length = OtpViewModel.OTP_LENGTH,
                            shakeOn = state.errorMessage,
                            onCodeChange = onCodeChange,
                            onSubmit = onSubmit,
                        )

                        if (state.isNewAccount) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                AuthTextField(
                                    value = state.firstName,
                                    onValueChange = onFirstNameChange,
                                    label = stringResource(R.string.auth_first_name_label),
                                    modifier = Modifier.weight(1f),
                                )
                                AuthTextField(
                                    value = state.lastName,
                                    onValueChange = onLastNameChange,
                                    label = stringResource(R.string.auth_last_name_label),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        state.smsWarning?.let { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        AnimatedVisibility(visible = state.errorMessage != null) {
                            Text(
                                text = state.errorMessage?.asString().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        GradientButton(
                            text = stringResource(R.string.auth_verify),
                            onClick = onSubmit,
                            enabled = state.canSubmit,
                            isLoading = state.isSubmitting,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onResend, enabled = state.canResend) {
                                Text(
                                    text = if (state.canResend) {
                                        stringResource(R.string.auth_resend_code)
                                    } else {
                                        stringResource(
                                            R.string.auth_resend_wait,
                                            PersianFormat.formatNumber(
                                                state.resendSecondsRemaining.toLong(),
                                            ),
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            TextButton(onClick = onChangePhone) {
                                Text(
                                    text = stringResource(R.string.auth_change_phone),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The six boxes.
 *
 * One hidden [BasicTextField] holds the value and draws the boxes in its
 * `decorationBox`. The active box takes a thicker border, and a wrong code
 * shakes the row — the shake is the only error signal that reaches the user
 * before they read the message.
 */
@Composable
private fun DigitRow(
    code: String,
    length: Int,
    shakeOn: UiMessage?,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(shakeOn) {
        if (shakeOn != null) {
            repeat(3) {
                shake.animateTo(10f, tween(durationMillis = 55))
                shake.animateTo(-10f, tween(durationMillis = 55))
            }
            shake.animateTo(0f, tween(durationMillis = 55))
        }
    }

    BasicTextField(
        value = code,
        onValueChange = { value ->
            if (value.length <= length) onCodeChange(value)
        },
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .focusRequester(focusRequester),
        // The real field is invisible; the boxes are the UI.
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                repeat(length) { index ->
                    val active = index == code.length
                    val borderColor by animateColorAsState(
                        targetValue = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "digit-border",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = code.getOrNull(index)
                                ?.let { digit -> PersianFormat.toPersianDigits(digit.toString()) }
                                .orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

@Preview(name = "کد تأیید — کاربر موجود", showBackground = true, locale = "fa")
@Composable
private fun OtpPreview() {
    MdbTheme {
        OtpContent(
            state = OtpUiState(displayPhone = "۰۹۱۲۳۴۵۶۷۸۹", code = "1234"),
            onCodeChange = {},
            onFirstNameChange = {},
            onLastNameChange = {},
            onSubmit = {},
            onResend = {},
            onChangePhone = {},
        )
    }
}

@Preview(name = "کد تأیید — حساب جدید", showBackground = true, locale = "fa")
@Composable
private fun OtpSignupPreview() {
    MdbTheme {
        OtpContent(
            state = OtpUiState(
                displayPhone = "۰۹۱۲۳۴۵۶۷۸۹",
                isNewAccount = true,
                code = "123456",
                errorMessage = UiMessage.Res(R.string.auth_otp_incomplete),
            ),
            onCodeChange = {},
            onFirstNameChange = {},
            onLastNameChange = {},
            onSubmit = {},
            onResend = {},
            onChangePhone = {},
        )
    }
}
