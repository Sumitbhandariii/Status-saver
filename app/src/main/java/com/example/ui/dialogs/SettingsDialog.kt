package com.example.ui.dialogs

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
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.AdMobConfig
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SecondaryText

@Composable
fun SettingsDialog(
    adConfig: AdMobConfig,
    onSaveAdConfig: (AdMobConfig) -> Unit,
    onOpenPermissionGuide: () -> Unit,
    onDismiss: () -> Unit
) {
    var appId by remember { mutableStateOf(adConfig.appId) }
    var bannerId by remember { mutableStateOf(adConfig.bannerAdUnitId) }
    var interstitialId by remember { mutableStateOf(adConfig.interstitialAdUnitId) }
    var rewardedId by remember { mutableStateOf(adConfig.rewardedAdUnitId) }
    var isTestMode by remember { mutableStateOf(adConfig.isTestMode) }

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
                            text = "Settings & Ads",
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

                // AdMob Configuration Section
                Text(
                    text = "Google AdMob Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )
                Text(
                    text = "Enter your live AdMob IDs or keep default Google test unit IDs.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Test Ads Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        )
                        Text(
                            text = "Use official Google test ads for safety",
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
                        )
                    }

                    Switch(
                        checked = isTestMode,
                        onCheckedChange = { isTestMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryDeepPurple)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AdSettingTextField(
                    label = "AdMob App ID",
                    value = appId,
                    onValueChange = { appId = it }
                )

                AdSettingTextField(
                    label = "Banner Unit ID",
                    value = bannerId,
                    onValueChange = { bannerId = it }
                )

                AdSettingTextField(
                    label = "Interstitial Unit ID",
                    value = interstitialId,
                    onValueChange = { interstitialId = it }
                )

                AdSettingTextField(
                    label = "Rewarded Unit ID",
                    value = rewardedId,
                    onValueChange = { rewardedId = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reset & Save AdMob Config Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            appId = "ca-app-pub-3940256099942544~3347511713"
                            bannerId = "ca-app-pub-3940256099942544/6300978111"
                            interstitialId = "ca-app-pub-3940256099942544/1033173712"
                            rewardedId = "ca-app-pub-3940256099942544/5224354917"
                            isTestMode = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Test IDs", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onSaveAdConfig(
                                AdMobConfig(
                                    appId = appId,
                                    bannerAdUnitId = bannerId,
                                    interstitialAdUnitId = interstitialId,
                                    rewardedAdUnitId = rewardedId,
                                    isTestMode = isTestMode
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Config", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Ads Preview Panel
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdsClick,
                                contentDescription = null,
                                tint = PrimaryDeepPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Instant Test Ads Preview",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to simulate and verify Google AdMob test ad displays immediately:",
                            style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText, fontSize = 11.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    com.example.ads.AdMobManager.showTestInterstitial()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepPurple),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("btn_test_interstitial")
                            ) {
                                Text("Interstitial", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    com.example.ads.AdMobManager.showTestRewarded("AdMob Test Unit")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("btn_test_rewarded")
                            ) {
                                Text("Rewarded", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = "Grant or reconfigure folder access",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SecondaryText)
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
                            text = "Version 1.0.0 • Production Build",
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

@Composable
private fun AdSettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryDeepPurple,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = Color(0xFFFAFAFA),
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
