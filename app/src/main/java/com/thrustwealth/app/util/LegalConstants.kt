package com.thrustwealth.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object LegalConstants {
    const val PRIVACY_POLICY_URL = "https://www.thrustwealth.com/legal/privacy-policy"
    const val TERMS_OF_USE_URL = "https://www.thrustwealth.com/legal/terms-of-use"
}

fun Context.openWebUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show()
    }
}
