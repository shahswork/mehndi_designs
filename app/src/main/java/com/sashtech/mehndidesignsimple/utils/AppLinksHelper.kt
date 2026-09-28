package com.sashtech.mehndidesignsimple.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Helper utility for application-level external intents:
 * - Google Play In-App Review API dialogue
 * - Dynamic Google Play Store URLs and market intents
 * - App sharing sheet
 * - Contact Us & Feedback emails (mailto)
 * - Privacy Policy web browser viewing
 */
object AppLinksHelper {

    const val SUPPORT_EMAIL = "hello@sashtech.site"
    const val PRIVACY_POLICY_URL = "https://sashtech.site/apps/Mehndi/privacy_policy.html"

    /**
     * Unwraps an Activity from a given Context hierarchy.
     */
    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Requests and presents the Google Play In-App Review API dialogue.
     * Follows official Google Play guidelines with graceful fallback to the Play Store page.
     *
     * @param context Android Context (Activity or wrapped Context)
     * @param fallbackToPlayStore Whether to open Play Store if In-App Review flow request fails
     * @param onComplete Callback invoked when review flow finishes
     */
    fun launchInAppReview(
        context: Context,
        fallbackToPlayStore: Boolean = true,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            if (fallbackToPlayStore) {
                rateApp(context)
            }
            onComplete(false)
            return
        }

        try {
            val manager: ReviewManager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        // In-App Review dialog finished
                        onComplete(true)
                    }
                } else {
                    if (fallbackToPlayStore) {
                        rateApp(context)
                    }
                    onComplete(false)
                }
            }
        } catch (_: Exception) {
            if (fallbackToPlayStore) {
                rateApp(context)
            }
            onComplete(false)
        }
    }

    /**
     * Dynamically generates the official Google Play Store URL for this application.
     */
    fun getPlayStoreUrl(context: Context): String {
        return "https://play.google.com/store/apps/details?id=${context.packageName}"
    }

    /**
     * Returns the Android market URI for direct opening in the Play Store app.
     */
    fun getMarketUri(context: Context): Uri {
        return Uri.parse("market://details?id=${context.packageName}")
    }

    /**
     * Opens the Android system share sheet to share the official Google Play Store link.
     */
    fun shareApp(context: Context) {
        try {
            val playStoreUrl = getPlayStoreUrl(context)
            val shareText = "Check out Mehndi Studio on Google Play:\n$playStoreUrl"

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Mehndi Studio")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share App via")
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens this application's Google Play Store page.
     * Tries market:// first, falling back to the browser Play Store URL if Play Store is unavailable.
     */
    fun rateApp(context: Context) {
        try {
            val marketUri = getMarketUri(context)
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            try {
                val webUri = Uri.parse(getPlayStoreUrl(context))
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens the default email app with pre-filled recipient and subject for general queries.
     */
    fun contactUs(context: Context) {
        val subject = "Mehndi App - Contact Us"
        val body = "Hello,\n\nI would like to contact you regarding the Mehndi app.\n\nThank you."
        sendEmail(context, subject, body)
    }

    /**
     * Opens the default email app with pre-filled recipient and subject for user feedback.
     */
    fun sendFeedback(context: Context) {
        val subject = "Mehndi App - Feedback"
        val body = "Hello,\n\nI would like to share feedback about the Mehndi app.\n\nFeedback:\n"
        sendEmail(context, subject, body)
    }

    /**
     * Launches an ACTION_SENDTO intent targeted at the support mailto URI.
     */
    fun sendEmail(context: Context, subject: String, body: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            handleNoEmailApp(context)
        }
    }

    /**
     * Gracefully copies the support email address to the clipboard if no email client is found.
     */
    fun handleNoEmailApp(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Support Email", SUPPORT_EMAIL)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(
                context,
                "No email app is installed on this device. Email copied to clipboard: $SUPPORT_EMAIL",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "No email app is installed on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Opens the official privacy policy in the device's web browser.
     */
    fun openPrivacyPolicy(context: Context) {
        try {
            val uri = Uri.parse(PRIVACY_POLICY_URL)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open web browser", Toast.LENGTH_SHORT).show()
        }
    }
}
