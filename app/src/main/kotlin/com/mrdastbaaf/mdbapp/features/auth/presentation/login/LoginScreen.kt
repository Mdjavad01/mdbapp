package com.mrdastbaaf.mdbapp.features.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.mrdastbaaf.mdbapp.core.ui.state.UiMessage
import com.mrdastbaaf.mdbapp.core.ui.theme.MdbTheme
import com.mrdastbaaf.mdbapp.core.ui.theme.Spacing

/**
 * Step one of sign-in: the phone number.
 *
 * The field uses `KeyboardType.Phone` and never rewrites what the user types — a
 * Persian keyboard's digits are accepted as-is and normalised on submit by
 * [com.mrdastbaaf.mdbapp.core.util.PhoneNormalizer], which is the same
 * conversion the server requires (`^09[0-9]{9}$`, ASCII only).
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

@Composable
private fun LoginContent(
    state: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit,
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
                text = stringResource(R.string.auth_login_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(R.string.auth_login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.phoneInput,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_phone_label)) },
                placeholder = { Text(stringResource(R.string.auth_phone_hint)) },
                singleLine = true,
                isError = state.errorMessage != null,
                supportingText = state.errorMessage?.let { message -> { Text(message.asString()) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

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
                Text(stringResource(R.string.auth_request_code))
            }

            Text(
                text = stringResource(R.string.auth_login_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "ورود", showBackground = true, locale = "fa")
@Composable
private fun LoginPreview() {
    MdbTheme {
        LoginContent(
            state = LoginUiState(phoneInput = "۰۹۱۲۳۴۵۶۷۸۹"),
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
