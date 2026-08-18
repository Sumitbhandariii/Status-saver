package com.example.ui.dialogs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.ads.AdMobManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SecondaryText

@Composable
fun SettingsDialog(
    onOpenPermissionGuide: () -> Unit,
    onOpenCleaner: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("status_vault_prefs", Context.MODE_PRIVATE) }
    
    var highQualitySave by remember { 
        mutableStateOf(prefs.getBoolean("pref_high_quality_save", true)) 
    }
    var notifyNewStatuses by remember { 
        mutableStateOf(prefs.getBoolean("pref_status_saved_alerts", false)) 
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PrimaryDeepPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage & Media Preferences
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High Quality Preservation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save Original Quality",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        )
                        Text(
                            text = "Keep maximum resolution and audio bitrate",
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                        )
                    }

                    Switch(
                        checked = highQualitySave,
                        onCheckedChange = { 
                            highQualitySave = it
                            prefs.edit().putBoolean("pref_high_quality_save", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryDeepPurple,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }

                // Auto Save Notification
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Saved Alerts",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        )
                        Text(
                            text = "Show confirmation toasts when statuses are saved",
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                        )
                    }

                    Switch(
                        checked = notifyNewStatuses,
                        onCheckedChange = { 
                            notifyNewStatuses = it
                            prefs.edit().putBoolean("pref_status_saved_alerts", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryDeepPurple,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp Folder Access Help
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = PrimaryDeepPurple
                            )
                            Column {
                                Text(
                                    text = "WhatsApp Folder Access",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "Grant or reconfigure status directory access",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText, fontSize = 11.sp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onOpenPermissionGuide()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Guide", fontSize = 11.sp)
                        }
                    }
                }

                if (onOpenCleaner != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706)
                                )
                                Column {
                                    Text(
                                        text = "Storage & Cache Cleaner",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText,
                                            fontSize = 13.sp
                                        )
                                    )
                                    Text(
                                        text = "Free up memory and delete duplicate files",
                                        style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText, fontSize = 11.sp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenCleaner()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Clean", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rewarded Ad / Support Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = PrimaryDeepPurple,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Watch Rewarded Video Ad",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText,
                                        fontSize = 13.sp
                                    )
                                )
                                Text(
                                    text = "Watch short video ad & earn free bonus access",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText, fontSize = 11.sp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                AdMobManager.showRewardedAd(reason = "Bonus Feature")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Watch", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Privacy & Security Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Your statuses and media stay securely on your device.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText, fontSize = 11.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // About Info
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "StatusVault – Status Saver",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                        )
                        Text(
                            text = "Version 1.0.0 • Production Release",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SecondaryText
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "StatusVault is an independent status management tool and is not affiliated with WhatsApp LLC.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SecondaryText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
