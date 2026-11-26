package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.BusinessProfile
import com.example.data.model.LicenseState
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.export.ImageRenderer
import com.example.export.QrCodeGenerator
import com.example.i18n.AppStrings
import com.example.licensing.LicenseConfig
import com.example.licensing.LicenseDiagnosticState
import com.example.ui.PreviewImagePayload
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.PaymentPaidBg
import com.example.ui.theme.PaymentPaidFg
import com.example.ui.theme.PaymentPartialBg
import com.example.ui.theme.PaymentPartialFg
import com.example.ui.theme.PaymentUnpaidBg
import com.example.ui.theme.PaymentUnpaidFg
import com.example.ui.theme.Saffron
import com.example.ui.theme.SaffronLight
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledFg
import com.example.ui.theme.StatusDeliveredBg
import com.example.ui.theme.StatusDeliveredFg
import com.example.ui.theme.StatusPackedBg
import com.example.ui.theme.StatusPackedFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg

fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

fun openWhatsAppMessage(context: Context, phone: String, message: String) {
    val url = QrCodeGenerator.buildWhatsAppUrl(phone, message)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ArthFoodHeader(
    profile: BusinessProfile,
    strings: AppStrings,
    licenseState: LicenseState,
    appLanguage: AppLanguage,
    hasPin: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onLicenseClick: () -> Unit,
    onLockClick: () -> Unit,
    diagnosticState: LicenseDiagnosticState = LicenseDiagnosticState()
) {
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }

    Surface(
        color = BasilGreenDark,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arthfood_icon_fg),
                    contentDescription = "ArthFood Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Saffron, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.businessName.ifBlank { strings.appBrandTitle },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ArthFood · Offline Billing",
                        style = MaterialTheme.typography.labelSmall,
                        color = SaffronLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Trial / Licensed pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (licenseState.isLicensed) BasilGreenLight else SaffronLight,
                    modifier = Modifier
                        .testTag("license_badge")
                        .clickable { onLicenseClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (licenseState.isLicensed) Icons.Default.Verified else Icons.Default.Lock,
                            contentDescription = "License Status",
                            tint = if (licenseState.isLicensed) BasilGreenDark else Color(0xFF92400E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (licenseState.isLicensed) strings.licensedForever else "${strings.trialDaysLeft(licenseState.daysRemaining)} · ₹499",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (licenseState.isLicensed) BasilGreenDark else Color(0xFF92400E)
                        )
                    }
                }

                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.testTag("help_tour_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Quick Tour & Support", tint = Color.White)
                }

                if (hasPin) {
                    IconButton(
                        onClick = onLockClick,
                        modifier = Modifier.testTag("lock_app_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock App", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.appLanguageLabel}:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                AppLanguage.entries.forEach { lang ->
                    val selected = lang == appLanguage
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) Saffron else Color.White.copy(alpha = 0.14f),
                        modifier = Modifier
                            .testTag("header_lang_${lang.code}")
                            .clickable { onLanguageSelected(lang) }
                    ) {
                        Text(
                            text = lang.nativeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color(0xFF1A2419) else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("ArthFood · Quick Tour & Support") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1. Orders: Track In Process, Sent, Paid & Due orders and share HD Invoice JPGs.", style = MaterialTheme.typography.bodySmall)
                    Text("2. + New Order: Paste WhatsApp orders, pick auto-priced packs, or mark Free Samples.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Manage: Edit Rates & Stock, Profit Calculator, Vendor Payables, Customer Dues, Order QR & Excel/JSON Backup.", style = MaterialTheme.typography.bodySmall)
                    LicenseCard(
                        licenseState = licenseState,
                        strings = strings,
                        onOpenActivation = {
                            showHelpDialog = false
                            onLicenseClick()
                        },
                        diagnosticState = diagnosticState,
                        showWhatsAppButton = false,
                        actionTestTag = "help_license_action_btn"
                    )
                    Surface(color = BasilGreenLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Support & Activation (₹499 Lifetime):", style = MaterialTheme.typography.labelMedium, color = BasilGreenDark)
                            Text("${LicenseConfig.SUPPORT_OWNER_NAME} • ${LicenseConfig.SUPPORT_WHATSAPP_DISPLAY}", style = MaterialTheme.typography.bodySmall)
                            Text("UPI: ${LicenseConfig.OWNER_UPI_ID}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    openWhatsAppMessage(
                        context,
                        LicenseConfig.SUPPORT_WHATSAPP_NUMBER,
                        "Hi ${LicenseConfig.SUPPORT_OWNER_NAME}, I need help with ArthFood (${licenseState.deviceId})."
                    )
                }) { Text("WhatsApp Support") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showHelpDialog = false }) { Text(strings.close) }
            }
        )
    }
}

@Composable
fun StatusPill(status: OrderStatus, label: String) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> StatusPendingBg to StatusPendingFg
        OrderStatus.PACKED -> StatusPackedBg to StatusPackedFg
        OrderStatus.DELIVERED -> StatusDeliveredBg to StatusDeliveredFg
        OrderStatus.CANCELLED -> StatusCancelledBg to StatusCancelledFg
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PaymentPill(status: PaymentStatus, label: String) {
    val (bg, fg) = when (status) {
        PaymentStatus.PAID -> PaymentPaidBg to PaymentPaidFg
        PaymentStatus.UNPAID -> PaymentUnpaidBg to PaymentUnpaidFg
        PaymentStatus.PARTIAL -> PaymentPartialBg to PaymentPartialFg
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ImagePreviewDialog(
    payload: PreviewImagePayload,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BasilGreenDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.close, tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(
                        bitmap = payload.bitmap.asImageBitmap(),
                        contentDescription = payload.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = ImageRenderer.saveJpgToGallery(context, payload.bitmap, payload.fileName)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to Pictures/ArthFood!" else "Save failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_jpg_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.saveDownloadJpg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            val file = ImageRenderer.saveBitmapToCacheJpg(context, payload.bitmap, payload.fileName)
                            ImageRenderer.shareFile(context, file, "image/jpeg", payload.title)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("share_jpg_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.shareToWhatsAppApps, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockOverlay(
    strings: AppStrings,
    onVerifyPin: (String) -> Boolean
) {
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            listOf(-26f, 24f, -18f, 14f, -8f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 45))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BasilGreenDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BasilGreen, modifier = Modifier.size(40.dp))
                Text(strings.unlockWithPin, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        wrong -> MaterialTheme.colorScheme.error
                                        filled -> BasilGreen
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(1.dp, if (wrong) MaterialTheme.colorScheme.error else BasilGreen, CircleShape)
                        )
                    }
                }

                if (wrong) {
                    Text(strings.wrongPin, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "<"))
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { key ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable {
                                        wrong = false
                                        when (key) {
                                            "C" -> entered = ""
                                            "<" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                            else -> if (entered.length < 4) {
                                                entered += key
                                                if (entered.length == 4) {
                                                    if (!onVerifyPin(entered)) {
                                                        wrong = true
                                                        shakeTrigger++
                                                        entered = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "<") {
                                        Icon(Icons.Default.Backspace, contentDescription = "Backspace")
                                    } else {
                                        Text(key, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(dismissLabel) }
        }
    )
}
