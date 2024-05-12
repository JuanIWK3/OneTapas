package com.example.onetapas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.onetapas.ui.theme.OneTapasTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneTapasTheme {
                val state = remember { SignInState() }
                var user: GoogleUser? by remember { mutableStateOf(null) }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (user != null) {
                        Text(text = "Welcome ${user?.fullName}")
                    } else {
                        OneTapGoogleButton(
                            state = state,
                            clientId = "bytheway",
                            onTokenIdReceived = { tokenId ->
                                Log.d(TAG, "Google tokenId: $tokenId")
                                user = getUserFromTokenId(tokenId)
                            },
                            onDialogDismissed = { message ->
                                Log.d(TAG, "Dialog dismissed: $message")
                            }
                        )
                    }
                }
            }
        }
    }
}


private const val TAG = "Tapas"

@Composable
fun SingInTapas(
    state: SignInState,
    clientId: String,
    nonce: String? = null,
    onTokenIdReceived: (String) -> Unit,
    onDialogDismissed: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }

    val googleIdOption = remember {
        GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .build()
    }

    val request = remember {
        GetCredentialRequest.Builder().setCredentialOptions(listOf(googleIdOption)).build()
    }


    LaunchedEffect(key1 = state.opened) {
        if (state.opened) {
            scope.launch {
                try {
                    val response =
                        credentialManager.getCredential(request = request, context = context)
                    handleSignIn(credentialResponse = response,
                        onTokenIdReceived = { tokenId ->
                            Log.d(TAG, "Google tokenId: $tokenId")
                            onTokenIdReceived(tokenId)
                            state.close()
                        },
                        onDialogDismissed = { message ->
                            Log.d(TAG, "Dialog dismissed: $message")
                            onDialogDismissed(message)
                            state.close()
                        })
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                    state.close()
                }
            }
        }
    }
}

private fun handleSignIn(
    credentialResponse: GetCredentialResponse,
    onTokenIdReceived: (String) -> Unit,
    onDialogDismissed: (String) -> Unit,
) {
    when (val credential = credentialResponse.credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)
                    onTokenIdReceived(googleIdTokenCredential.idToken)
                } catch (e: GoogleIdTokenParsingException) {
                    onDialogDismissed("Invalid Google tokenId response: ${e.message}")
                }
            } else {
                onDialogDismissed("Unexpected Type of Credential.")
            }
        }

        else -> {
            onDialogDismissed("Unexpected Type of Credential.")
        }
    }
}


