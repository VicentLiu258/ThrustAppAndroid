package com.thrustwealth.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.theme.ThrustTheme
import com.thrustwealth.app.util.LegalConstants
import com.thrustwealth.app.util.openWebUrl

/**
 * First-launch privacy consent. SaleSmartly SDK must not initialize until the user accepts.
 */
@Composable
fun PrivacyConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colors = ThrustTheme.colors
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* require explicit choice */ },
        title = {
            Text(
                "Privacy Notice & Terms",
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Thrust Wealth stores calculation and journal data locally on your device.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "If you use Customer Support, the SaleSmartly chat SDK will connect to the network and may collect device/system information and IP address to provide live chat. Optional media uploads only occur when you choose to send files.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "After you agree, AppsFlyer may collect device identifiers and app-open data to measure installs and in-app actions such as search navigation and submitting a phone number in support chat. The phone number itself is not sent to AppsFlyer.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "By tapping Agree, you confirm you have read and agree to our Privacy Policy and Terms of Use.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Privacy Policy",
                        color = colors.brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            context.openWebUrl(LegalConstants.PRIVACY_POLICY_URL)
                        }
                    )
                    Text(
                        text = "Terms of Use",
                        color = colors.brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            context.openWebUrl(LegalConstants.TERMS_OF_USE_URL)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Agree", color = colors.brand, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Exit", color = colors.textTertiary)
            }
        }
    )
}
