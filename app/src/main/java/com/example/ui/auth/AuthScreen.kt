package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.LegalPoliciesSheet
import com.example.ui.components.LegalPolicyTab
import com.example.ui.glass.AmbientAtelierBackdrop
import com.example.ui.glass.GlassMaterialStrength
import com.example.ui.glass.LiquidGlassSurface
import com.example.ui.motion.staggeredSpringEntrance
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.theme.BrassGold
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.data.firebase.DoorCutFirebase
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPoliciesSheet by remember { mutableStateOf(false) }

    AmbientAtelierBackdrop(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sunlit Editorial Hero Banner
            Box(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .staggeredSpringEntrance(index = 0)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_home_grooming_1793031644119),
                    contentDescription = "DoorCut Home Grooming",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrassGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCut,
                                contentDescription = null,
                                tint = EspressoHeaderBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DoorCut",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Instant Doorstep Haircuts, Beard Styling & Verified Lady Hair Stylists.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFFDF9E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Google Sign-In Entry Card
            LiquidGlassSurface(
                strength = GlassMaterialStrength.AtelierCard,
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 4.dp,
                interactivePhysics = false,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .staggeredSpringEntrance(index = 1)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sign in with Google to Get Started",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "One verified account for Customers and Partner Barbers. After sign-in, confirm your name, date of birth, and choose Customer or Barber Mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    AuthTrustRow(
                        icon = Icons.Filled.LocationOn,
                        text = "Live Uber-style barber movement tracking & doorstep ETA"
                    )
                    AuthTrustRow(
                        icon = Icons.Filled.VerifiedUser,
                        text = "Dedicated Lady Hair Stylist option for female bookings"
                    )
                    AuthTrustRow(
                        icon = Icons.Filled.GppGood,
                        text = "Pay service charges directly to your appointed barber (Cash/UPI)"
                    )

                    if (errorMessage != null) {
                        LiquidGlassSurface(
                            strength = GlassMaterialStrength.AtelierCard,
                            shape = RoundedCornerShape(14.dp),
                            accentBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            shadowElevation = 1.dp,
                            interactivePhysics = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null) {
                                errorMessage = "Unable to open Google Sign-In from current window context."
                                return@Button
                            }
                            errorMessage = null
                            isSigningIn = true
                            scope.launch {
                                try {
                                    val webClientId = context.getString(R.string.default_web_client_id)
                                    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val credentialManager = CredentialManager.create(activity)
                                    val result = credentialManager.getCredential(
                                        request = request,
                                        context = activity
                                    )
                                    val cred = result.credential
                                    if (cred is CustomCredential &&
                                        cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val tokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                                        val firebaseCred = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
                                        DoorCutFirebase.auth().signInWithCredential(firebaseCred).await()
                                        isSigningIn = false
                                        onSignedIn()
                                    } else {
                                        isSigningIn = false
                                        errorMessage = "Unsupported credential returned. Please try again."
                                    }
                                } catch (_: GetCredentialCancellationException) {
                                    isSigningIn = false
                                    errorMessage = "Sign-in was cancelled."
                                } catch (_: NoCredentialException) {
                                    isSigningIn = false
                                    errorMessage = "No Google account found on this device. Add a Google account in Android Settings and try again."
                                } catch (e: Exception) {
                                    isSigningIn = false
                                    errorMessage = e.localizedMessage ?: "Google Sign-In failed. Check your connection."
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EspressoHeaderBg,
                            contentColor = Color(0xFFFFDF9E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .tactilePressPhysics(enabled = !isSigningIn, enableHaptics = true)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = BrassGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.auth_signing_in),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.auth_btn_google),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    TextButton(
                        onClick = { showPoliciesSheet = true },
                        modifier = Modifier.testTag("auth_privacy_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Policy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy Policy, Safety Charter & Terms", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showPoliciesSheet) {
        LegalPoliciesSheet(
            initialTab = LegalPolicyTab.PRIVACY,
            onDismiss = { showPoliciesSheet = false }
        )
    }
}

@Composable
private fun AuthTrustRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SageVerified,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
