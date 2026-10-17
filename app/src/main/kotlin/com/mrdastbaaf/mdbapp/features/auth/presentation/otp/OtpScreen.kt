package com.mrdastbaaf.mdbapp.features.auth.presentation.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdastbaaf.mdbapp.R
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing
import com.mrdastbaaf.mdbapp.core.util.PersianFormat

/**
 * Step two of sign-in: the OTP, plus a name when the account is new.
 *
 * The name fields appear only for a new account — the server tells us which case
 * we are in via `isRegistered`, and it will *not* reject a request without a name
 * (it silently falls back to `هنرجو NNNN`). So the client is the only place that
 * can insist, which is why the validation lives here.
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
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = if (state.isNewAccount) {
                    stringResource(R.string.auth_signup_title)
                } else {
                    stringResource(R.string.auth_login_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = if (state.isNewAccount) {
                    stringResource(R.string.auth_signup_subtitle)
                } else {
                    stringResource(R.string.auth_otp_sent_to, state.displayPhone)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // The server only sets this when the SMS provider is unconfigured:
            // the code is still valid, it just was not delivered. Surfacing it
            // avoids a user waiting forever for a message that is not coming.
            state.smsWarning?.let { warning ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = warning,
                        modifier = Modifier.padding(Spacing.md),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            OutlinedTextField(
                value = PersianFormat.toPersianDigits(state.code),
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_otp_label)) },
                placeholder = { Text(stringResource(R.string.auth_otp_hint)) },
                singleLine = true,
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message -> { Text(message.asString()) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            if (state.isNewAccount) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = onFirstNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_first_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = onLastNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_last_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                )
            }

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = Spacing.sm),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(stringResource(R.string.auth_verify))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onResend, enabled = state.canResend) {
                    Text(
                        text = if (state.canResend) {
                            stringResource(R.string.auth_resend_code)
                        } else {
                            stringResource(
                                R.string.auth_resend_wait,
                                PersianFormat.formatNumber(state.resendSecondsRemaining.toLong()),
                            )
                        },
                    )
                }

                TextButton(onClick = onChangePhone) {
                    Text(stringResource(R.string.auth_change_phone))
                }
            }
        }
    }
}

@Preview(name = "کد تأیید — کاربر موجود", showBackground = true, locale = "fa")
@Composable
private fun OtpExistingUserPreview() {
    MdbTheme {
        OtpContent(
            state = OtpUiState(
                phone = "09123456789",
                displayPhone = "۰۹۱۲۳۴۵۶۷۸۹",
                isNewAccount = false,
                code = "1234",
                resendSecondsRemaining = 97,
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

@Preview(name = "کد تأیید — کاربر جدید", showBackground = true, locale = "fa")
@Composable
private fun OtpNewUserPreview() {
    MdbTheme {
        OtpContent(
            state = OtpUiState(
                phone = "09123456789",
                displayPhone = "۰۹۱۲۳۴۵۶۷۸۹",
                isNewAccount = true,
                code = "123456",
                firstName = "محمدجواد",
                lastName = "فلان",
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
