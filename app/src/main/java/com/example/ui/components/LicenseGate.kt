package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LicenseState
import com.example.i18n.AppStrings
import com.example.licensing.LicenseConfig
import com.example.licensing.LicenseDiagnosticState
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Saffron
import com.example.ui.theme.SaffronLight
import com.example.util.Formatters

/**
 * Slim 7-day trial banner shown below the header only while the app is in its active trial.
 * Tapping opens the 'Activate Full Access' modal.
 */
@Composable
fun TrialStatusBanner(
    licenseState: LicenseState,
    onActivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (licenseState.isLicensed || licenseState.isLockedOut) return

    val daysText = if (licenseState.daysRemaining == 1) "1 day left" else "${licenseState.daysRemaining} days left"

    Surface(
        color = SaffronLight,
        border = BorderStroke(1.dp, Saffron.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("trial_status_banner")
            .clickable(onClick = onActivateClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Free Trial Status",
                tint = Color(0xFF92400E),
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "7-Day Free Trial · $daysText",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to unlock lifetime offline access (₹${LicenseConfig.PLAN_PRICE_INR})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF92400E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BasilGreenDark
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Activate ₹${LicenseConfig.PLAN_PRICE_INR}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reusable License Status Card used in Manage -> Backup & Excel and Quick Tour & Support.
 * Includes an expandable Diagnostic Logs panel displaying Device ID, normalized license key,
 * current activation status, and last HMAC comparison error message.
 */
@Composable
fun LicenseCard(
    licenseState: LicenseState,
    strings: AppStrings,
    onOpenActivation: () -> Unit,
    modifier: Modifier = Modifier,
    diagnosticState: LicenseDiagnosticState = LicenseDiagnosticState(),
    defaultDiagnosticsExpanded: Boolean = false,
    showWhatsAppButton: Boolean = true,
    actionTestTag: String = "license_card_action_btn"
) {
    val context = LocalContext.current
    val isActivated = licenseState.isLicensed
    val statusBg = if (isActivated) BasilGreenLight else SaffronLight
    val statusFg = if (isActivated) BasilGreenDark else Color(0xFF78350F)
    var showDiagnostics by remember { mutableStateOf(defaultDiagnosticsExpanded) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isActivated) BasilGreen.copy(alpha = 0.35f) else Saffron.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isActivated) Icons.Default.Verified else Icons.Default.Lock,
                            contentDescription = null,
                            tint = statusFg,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isActivated) {
                                "Full Access Activated (₹${LicenseConfig.PLAN_PRICE_INR})"
                            } else {
                                "Free Trial · ${licenseState.daysRemaining} days left"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.deviceIdLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = licenseState.deviceId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { copyToClipboard(context, "Device ID", licenseState.deviceId) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = strings.copy)
                }
            }

            if (isActivated && licenseState.licenseKey.isNotBlank()) {
                Text(
                    text = "License Key: ${licenseState.licenseKey}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Support: ${LicenseConfig.SUPPORT_OWNER_NAME} • ${LicenseConfig.SUPPORT_WHATSAPP_DISPLAY}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenActivation,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag(actionTestTag)
                ) {
                    Text(
                        text = if (isActivated) "View License Details" else "Activate Full Access",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showWhatsAppButton) {
                    OutlinedButton(
                        onClick = {
                            val msg = "Hi ${LicenseConfig.SUPPORT_OWNER_NAME}, I need help with ArthFood (Device ID: ${licenseState.deviceId})."
                            openWhatsAppMessage(context, LicenseConfig.SUPPORT_WHATSAPP_NUMBER, msg)
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("WhatsApp", maxLines = 1)
                    }
                }
            }

            // Expandable Diagnostic Logs Header
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("toggle_license_diagnostics_btn")
                    .clickable { showDiagnostics = !showDiagnostics }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = BasilGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Activation Diagnostic Logs",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = BasilGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDiagnostics) "Collapse logs" else "Expand logs",
                        tint = BasilGreenDark
                    )
                }
            }

            AnimatedVisibility(visible = showDiagnostics) {
                LicenseDiagnosticLogsPanel(
                    licenseState = licenseState,
                    diagnosticState = diagnosticState
                )
            }
        }
    }
}

@Composable
fun LicenseDiagnosticLogsPanel(
    licenseState: LicenseState,
    diagnosticState: LicenseDiagnosticState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activationStatusLabel = when {
        licenseState.isLicensed -> "ACTIVATED (Lifetime ₹${LicenseConfig.PLAN_PRICE_INR})"
        licenseState.isClockTampered -> "LOCKED_OUT (Clock Tamper Detected)"
        licenseState.isTrialExpired -> "LOCKED_OUT (7-Day Trial Expired)"
        else -> "TRIAL_ACTIVE (${licenseState.daysRemaining} day(s) left)"
    }
    val displayedNormalizedKey = diagnosticState.lastNormalizedKey.ifBlank {
        licenseState.licenseKey.ifBlank { "None attempted yet" }
    }
    val lastAttemptLabel = if (diagnosticState.lastAttemptAtMillis > 0L) {
        Formatters.formatDateTime(diagnosticState.lastAttemptAtMillis)
    } else {
        "No attempt yet"
    }

    val errorContainerColor = when (diagnosticState.lastSucceeded) {
        true -> BasilGreenLight
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val errorTextColor = when (diagnosticState.lastSucceeded) {
        true -> BasilGreenDark
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val diagnosticSummaryText = buildString {
        appendLine("ArthFood License Diagnostics")
        appendLine("Device ID: ${licenseState.deviceId}")
        appendLine("Activation Status: $activationStatusLabel")
        appendLine("Normalized Key: $displayedNormalizedKey")
        appendLine("Last Attempt: $lastAttemptLabel")
        appendLine("Last HMAC Result: ${diagnosticState.lastErrorMessage}")
        if (diagnosticState.recentLogs.isNotEmpty()) {
            appendLine("Recent Attempts:")
            diagnosticState.recentLogs.forEach { entry ->
                appendLine("- [${Formatters.formatDateTime(entry.timestampMs)}] ${entry.outcome} | ${entry.normalizedKey} | ${entry.errorMessage}")
            }
        }
    }.trim()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("license_diagnostic_logs_panel")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiagnosticFieldRow(
                label = "Device ID",
                value = licenseState.deviceId.ifBlank { "Unknown" },
                mono = true
            )
            DiagnosticFieldRow(
                label = "Normalized License Key",
                value = displayedNormalizedKey,
                mono = true
            )
            DiagnosticFieldRow(
                label = "Activation Status",
                value = activationStatusLabel,
                mono = false
            )
            DiagnosticFieldRow(
                label = "Last Attempt",
                value = lastAttemptLabel,
                mono = false
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = errorContainerColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Last HMAC Comparison Result",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = errorTextColor
                    )
                    Text(
                        text = diagnosticState.lastErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = errorTextColor,
                        modifier = Modifier.testTag("license_diagnostic_last_error")
                    )
                }
            }

            if (diagnosticState.recentLogs.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Recent Verification Log (${diagnosticState.recentLogs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                diagnosticState.recentLogs.forEach { entry ->
                    Text(
                        text = "${Formatters.formatDateTime(entry.timestampMs)} • ${entry.outcome} • ${entry.normalizedKey}\n${entry.errorMessage}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = if (entry.outcome == "VERIFIED") BasilGreenDark else MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedButton(
                onClick = { copyToClipboard(context, "License Diagnostics", diagnosticSummaryText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("copy_license_diagnostics_btn")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy Diagnostic Logs")
            }
        }
    }
}

@Composable
private fun DiagnosticFieldRow(
    label: String,
    value: String,
    mono: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (mono) JetBrainsMonoFontFamily else MaterialTheme.typography.bodySmall.fontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Full-screen 'Activate Full Access' modal (when isHardLock = false) or hard-lock access gate
 * (when isHardLock = true after 7-day trial expiration or trial clock rollback).
 */
@Composable
fun LicenseActivationScreen(
    licenseState: LicenseState,
    strings: AppStrings,
    isHardLock: Boolean,
    onVerifyKey: (String, (Boolean) -> Unit) -> Unit,
    onExportBackup: () -> Unit,
    onExportDeviceReport: () -> Unit,
    onDismissIfOptional: () -> Unit
) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BasilGreenDark)
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.arthfood_icon_fg),
                        contentDescription = "ArthFood Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHardLock) strings.trialExpiredTitle else "Activate Full Access",
                            style = MaterialTheme.typography.titleLarge,
                            color = BasilGreenDark
                        )
                        Text(
                            text = strings.oneTimePlanLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isHardLock) {
                        IconButton(
                            onClick = onDismissIfOptional,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close)
                        }
                    }
                }

                if (licenseState.isClockTampered && !licenseState.isLicensed) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.clockTamperWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (licenseState.isLicensed) {
                    Surface(
                        color = BasilGreenLight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BasilGreen)
                            Spacer(Modifier.width(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Full Access Activated (₹${LicenseConfig.PLAN_PRICE_INR})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BasilGreenDark
                                )
                                Text(
                                    text = "Device: ${licenseState.deviceId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                                if (licenseState.licenseKey.isNotBlank()) {
                                    Text(
                                        text = "Key: ${licenseState.licenseKey}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = SaffronLight,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Saffron, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.unlockFullAccess,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "${strings.deviceIdLabel}:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = licenseState.deviceId,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = { copyToClipboard(context, "Device ID", licenseState.deviceId) },
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.copy)
                                }
                            }

                            Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.payViaUpiLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = LicenseConfig.OWNER_UPI_ID,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Pay ₹${LicenseConfig.PLAN_PRICE_INR} & send Device ID to ${LicenseConfig.SUPPORT_OWNER_NAME}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { copyToClipboard(context, "UPI ID", LicenseConfig.OWNER_UPI_ID) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI")
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val msg = "Hi ${LicenseConfig.SUPPORT_OWNER_NAME}, I want to activate ArthFood (₹${LicenseConfig.PLAN_PRICE_INR} Lifetime). My Device ID is: ${licenseState.deviceId}"
                                    openWhatsAppMessage(context, LicenseConfig.SUPPORT_WHATSAPP_NUMBER, msg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BasilGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("whatsapp_request_key_button")
                            ) {
                                Text("${strings.requestKeyWhatsApp} (${LicenseConfig.SUPPORT_WHATSAPP_DISPLAY})")
                            }
                        }
                    }

                    val isFormatValid = remember(keyInput) { LicenseConfig.isWellFormedKey(keyInput) }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = LicenseConfig.sanitizeKeyInput(it)
                            errorMsg = null
                        },
                        label = { Text(strings.enterLicenseKey) },
                        placeholder = { Text("AF499-XXXX-XXXX-XXXX") },
                        singleLine = true,
                        isError = errorMsg != null,
                        supportingText = {
                            when {
                                errorMsg != null -> Text(errorMsg!!)
                                keyInput.isBlank() -> Text(
                                    "Paste the AF499 key or full WhatsApp activation message.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                isFormatValid -> Text(
                                    "Key format looks valid — tap Activate to verify.",
                                    color = BasilGreenDark,
                                    fontWeight = FontWeight.Medium
                                )
                                else -> Text(
                                    "Expected format: AF499-XXXX-XXXX-XXXX (12 characters after AF499-).",
                                    color = Color(0xFF92400E)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("license_key_input")
                    )

                    Button(
                        onClick = {
                            val sanitized = LicenseConfig.sanitizeKeyInput(keyInput)
                            keyInput = sanitized
                            onVerifyKey(sanitized) { ok ->
                                if (!ok) {
                                    val diag = LicenseConfig.verifyKeyWithDiagnostics(licenseState.deviceId, sanitized)
                                    errorMsg = diag.errorMessage
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_license_button")
                    ) {
                        Text(strings.verifyAndUnlock)
                    }
                }

                HorizontalDivider()

                // Always allow full JSON data & signed device report export even if locked out!
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("lock_screen_export_backup_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download Full Backup (.json)")
                }
                OutlinedButton(
                    onClick = onExportDeviceReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_device_report_btn")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download Device Report")
                }
                Text(
                    text = "${strings.exportBackupSafeNote}\nSupport: ${LicenseConfig.SUPPORT_OWNER_NAME} (${LicenseConfig.SUPPORT_WHATSAPP_DISPLAY})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
