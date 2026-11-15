package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.service.DispatchNotificationHelper
import com.example.ui.MainAppScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SecurityNavyDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private var openTrackingState by mutableStateOf(false)
    private var deepLinkRequestIdState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.service.MapTileCacheManager.initializeOfflineTileCaching(applicationContext)
        handleNotificationIntent(intent)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SecurityNavyDark
                ) {
                    AppContent(
                        openTrackingOnLaunch = openTrackingState,
                        deepLinkRequestId = deepLinkRequestIdState,
                        onDeepLinkConsumed = {
                            openTrackingState = false
                            deepLinkRequestIdState = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(incoming: Intent?) {
        if (incoming == null) return
        val isPanic = incoming.getBooleanExtra("EXTRA_IS_PANIC", false)
        val openTracking = incoming.getBooleanExtra(DispatchNotificationHelper.EXTRA_OPEN_TRACKING, false)
        val requestId = incoming.getStringExtra(DispatchNotificationHelper.EXTRA_REQUEST_ID)
        if (isPanic || openTracking || !requestId.isNullOrBlank()) {
            openTrackingState = true
            deepLinkRequestIdState = requestId
        }
    }
}

@Composable
fun AppContent(
    openTrackingOnLaunch: Boolean = false,
    deepLinkRequestId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    // Listen to Firebase auth state changes in real-time
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Silent background auto-login attempt on app cold start
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            val clientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (_: Exception) {
                null
            }

            if (clientId != null) {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(clientId)
                        .setAutoSelectEnabled(true)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        Firebase.auth.signInWithCredential(authCredential).await()
                    }
                } catch (_: Exception) {
                    // Silent auto sign-in fallback: user stays on AuthScreen
                }
            }
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            MainAppScreen(
                currentUserId = uid,
                onSignOutComplete = {
                    currentUser = null
                },
                openTrackingOnLaunch = openTrackingOnLaunch,
                deepLinkRequestId = deepLinkRequestId,
                onDeepLinkConsumed = onDeepLinkConsumed
            )
        }
    }
}
