package com.sashtech.mehndidesignsimple.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import com.sashtech.mehndidesignsimple.notifications.OneSignalConfig
import com.sashtech.mehndidesignsimple.notifications.OneSignalNotificationManager
import com.sashtech.mehndidesignsimple.ui.components.MehndiTopBar
import com.sashtech.mehndidesignsimple.utils.AppLinksHelper

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MehndiTopBar(
            title = "Settings",
            subtitle = "Preferences & App Information",
            showSearch = false
        )

        val isSubscribed by OneSignalNotificationManager.isSubscribed.collectAsStateWithLifecycle()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Push Notifications Section
            item {
                Text(
                    text = "Push Notifications",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = "Daily Inspiration & Trends",
                            subtitle = if (isSubscribed) "Notifications active" else "Tap to enable notifications",
                            testTag = "settings_notifications",
                            onClick = {
                                if (!OneSignalConfig.isConfigured()) {
                                    Toast.makeText(
                                        context,
                                        "Please configure your OneSignal App ID in .env to enable notifications.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    OneSignalNotificationManager.promptForPushPermission(fallbackToSettings = true) { granted ->
                                        if (granted) {
                                            Toast.makeText(context, "Notifications enabled successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Notification permission was not granted.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Downloads & Storage Section
            item {
                Text(
                    text = "Storage & Downloads",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Folder,
                            title = "Download Location",
                            subtitle = "Pictures/MehndiStudio (Gallery)",
                            testTag = "settings_download_location",
                            onClick = {
                                Toast.makeText(context, "Saved images are stored in your Gallery", Toast.LENGTH_SHORT).show()
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.CleaningServices,
                            title = "Clear Image Cache",
                            subtitle = "Free up temporary cache storage",
                            testTag = "settings_clear_cache",
                            onClick = {
                                context.imageLoader.diskCache?.clear()
                                context.imageLoader.memoryCache?.clear()
                                Toast.makeText(context, "Image cache cleared successfully", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // About & Support Section
            item {
                Text(
                    text = "About & Community",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Star,
                            title = "Rate App",
                            subtitle = "Support us with a 5-star rating",
                            testTag = "settings_rate_app",
                            onClick = {
                                AppLinksHelper.launchInAppReview(context)
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Share,
                            title = "Share App",
                            subtitle = "Recommend Mehndi Design to friends and family",
                            testTag = "settings_share_app",
                            onClick = {
                                AppLinksHelper.shareApp(context)
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Email,
                            title = "Contact Us",
                            subtitle = "Get in touch with our team",
                            testTag = "settings_contact_us",
                            onClick = {
                                AppLinksHelper.contactUs(context)
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Feedback,
                            title = "Feedback",
                            subtitle = "Send us ideas, suggestions or report issues",
                            testTag = "settings_feedback",
                            onClick = {
                                AppLinksHelper.sendFeedback(context)
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Policy,
                            title = "Privacy Policy",
                            subtitle = "Read our terms and data policy",
                            testTag = "settings_privacy_policy",
                            onClick = {
                                AppLinksHelper.openPrivacyPolicy(context)
                            }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "About Mehndi Design",
                            subtitle = "Version 1.0.0",
                            testTag = "settings_about",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Mehndi Design v1.0", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Mehndi Design is a lightweight, mobile-first henna inspiration app designed for artists and enthusiasts worldwide. Browse high-definition curated bridal, Arabic, front hand, and festive patterns, or follow step-by-step interactive drawing tutorials.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}
