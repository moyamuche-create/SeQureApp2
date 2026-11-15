package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.foundation.rememberScrollState
import com.example.R
import com.example.ui.components.AnimatedSequreLogo
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.PurpleStripedHeaderBadge
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * SeQure Mobile Login Screen Layout
 * Built with Jetpack Compose following Material 3 guidelines & UX Best Practices:
 * 1. Responsive & Adaptive Centered Layout (widthIn max 480dp for phones, foldables & tablets)
 * 2. Outlined TextFields for Email and Password with leading icons
 * 3. Password Visibility Toggle (IconButton with eye icon)
 * 4. 'Forgot password?' right-aligned recovery link
 * 5. High-contrast primary 'Log In' button
 * 6. Social Sign-In Divider & One-Tap Google Authentication
 * 7. Anchored 'Sign up' link at the bottom
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberMeChecked by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val triggerAuthentication = {
        keyboardController?.hide()
        var valid = true
        if (emailInput.isBlank()) {
            emailError = "Please enter your email or username"
            valid = false
        } else {
            emailError = null
        }

        if (passwordInput.isBlank()) {
            passwordError = "Please enter your password"
            valid = false
        } else if (passwordInput.length < 6) {
            passwordError = "Password must be at least 6 characters"
            valid = false
        } else {
            passwordError = null
        }

        if (valid) {
            isLoading = true
            errorMessage = null
            signInWithGoogle(
                context = context,
                credentialManager = credentialManager,
                scope = coroutineScope,
                onSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onError = { err ->
                    isLoading = false
                    errorMessage = err
                },
                onCancelled = {
                    isLoading = false
                }
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        PurpleSurfaceTint,
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. BRAND HEADER (Animated Logo, Tactical Badge, Heading & Subtitle)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedSequreLogo(
                        size = 104.dp,
                        showOrbitalRing = true,
                        showStripedRim = true,
                        modifier = Modifier.testTag("animated_brand_logo")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PurpleStripedHeaderBadge(
                        text = "ENCRYPTED DISPATCH PORTAL",
                        modifier = Modifier.testTag("auth_tactical_badge")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isSignUpMode) "Create Account" else "Welcome back",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = if (isSignUpMode) {
                            "Sign up for instant on-demand security guard dispatch"
                        } else {
                            "Log in to SeQure for active real-time protection"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. MATERIAL 3 INPUT FORM CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PurpleStripedBanner(
                            height = 6.dp,
                            stripeWidthDp = 9f,
                            gapWidthDp = 9f,
                            animated = true
                        )
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .testTag("auth_error_text")
                            )
                        }

                        // Outlined TextField for Email
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                emailError = null
                                errorMessage = null
                            },
                            label = { Text("Email or Username") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = SecurityBlueLight
                                )
                            },
                            isError = emailError != null,
                            supportingText = emailError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = SecurityBlueLight,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = SecurityBlueLight,
                                unfocusedLabelColor = Color(0xFF64748B)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Outlined TextField for Password with Visibility Toggle
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                passwordError = null
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SecurityBlueLight
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.testTag("password_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            isError = passwordError != null,
                            supportingText = passwordError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { triggerAuthentication() }
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = SecurityBlueLight,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = SecurityBlueLight,
                                unfocusedLabelColor = Color(0xFF64748B)
                            )
                        )

                        // 3. REMEMBER ME & FORGOT PASSWORD LINK
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.testTag("remember_me_row")
                            ) {
                                Checkbox(
                                    checked = rememberMeChecked,
                                    onCheckedChange = { rememberMeChecked = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SecurityBlueLight,
                                        uncheckedColor = Color(0xFF64748B),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Text(
                                    text = "Remember me",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }

                            if (!isSignUpMode) {
                                TextButton(
                                    onClick = { showForgotPasswordDialog = true },
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text(
                                        text = "Forgot password?",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = SecurityBlueLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. PRIMARY 'LOG IN' BUTTON
                        Button(
                            onClick = triggerAuthentication,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("primary_login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecurityBlueLight,
                                contentColor = Color.White
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) "Sign Up" else "Log In",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 5. SOCIAL SIGN-IN DIVIDER & GOOGLE BUTTON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "Or continue with",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                isLoading = true
                                errorMessage = null
                                signInWithGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = coroutineScope,
                                    onSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    onCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF8FAFC),
                                contentColor = Color(0xFF0F172A)
                            ),
                            enabled = !isLoading
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleIcon(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. ANCHORED 'SIGN UP' LINK AT BOTTOM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )
                    TextButton(
                        onClick = {
                            isSignUpMode = !isSignUpMode
                            errorMessage = null
                            emailError = null
                            passwordError = null
                        },
                        modifier = Modifier.testTag("toggle_signup_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) "Log in" else "Sign up",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SecurityBlueLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Account Recovery Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Account Recovery",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "SeQure uses secure account verification. Simply tap 'Continue with Google' to sign in or recover your account instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgotPasswordDialog = false
                        isLoading = true
                        signInWithGoogle(
                            context = context,
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            onCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecurityBlueLight,
                        contentColor = Color.White
                    )
                ) {
                    Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close", color = Color(0xFF475569))
                }
            },
            containerColor = Color.White
        )
    }
}

fun signInWithGoogle(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onSuccess()
            } else {
                onError("Unexpected credential returned by Google Identity.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Authentication failed. Please try again.")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onComplete()
            }
        }
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

            // Blue arc (Right/Top)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -45f,
                sweepAngle = 135f,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
            // Green arc (Bottom)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
            // Yellow arc (Bottom-Left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
            // Red arc (Top-Left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(size.width, size.height)
            )
            // Inner white circle to form the 'G' hole
            drawCircle(
                color = Color.White,
                radius = radius * 0.55f,
                center = center
            )
            // Inner blue horizontal bar for the 'G'
            drawRect(
                color = Color(0xFF4285F4),
                topLeft = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.18f),
                size = androidx.compose.ui.geometry.Size(radius * 0.8f, radius * 0.36f)
            )
        }
    }
}
