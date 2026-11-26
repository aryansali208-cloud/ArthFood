package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BackupInspectionSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CustomerSummary
import com.example.data.model.DateRangePreset
import com.example.data.model.LicenseState
import com.example.export.QrCodeGenerator
import com.example.i18n.AppStrings
import com.example.licensing.LicenseDiagnosticState
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.LicenseCard
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openWhatsAppMessage
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.util.Formatters

private enum class CustomerSortOption(val label: String) {
    PENDING_DUE("Pending Due"),
    TOTAL_BILLED("Total Billed"),
    ORDER_COUNT("Orders"),
    RECENT("Recent")
}

@Composable
fun ManageCustomersTab(
    strings: AppStrings,
    profile: BusinessProfile,
    customers: List<CustomerSummary>,
    onViewCustomerOrders: (CustomerSummary) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(CustomerSortOption.PENDING_DUE) }

    val visibleCustomers = remember(customers, search, sortBy) {
        val q = search.trim().lowercase()
        customers
            .filter { c ->
                q.isBlank() || c.name.lowercase().contains(q) || c.phone.lowercase().contains(q)
            }
            .sortedWith(
                when (sortBy) {
                    CustomerSortOption.PENDING_DUE -> compareByDescending<CustomerSummary> { it.pendingDue }.thenByDescending { it.totalSpent }
                    CustomerSortOption.TOTAL_BILLED -> compareByDescending { it.totalSpent }
                    CustomerSortOption.ORDER_COUNT -> compareByDescending { it.orderCount }
                    CustomerSortOption.RECENT -> compareByDescending { it.lastOrderAt }
                }
            )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search customer name or mobile...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (search.isNotBlank()) {
                    IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomerSortOption.entries.forEach { opt ->
                FilterChip(
                    selected = sortBy == opt,
                    onClick = { sortBy = opt },
                    label = { Text(opt.label) },
                    modifier = Modifier.heightIn(min = 44.dp)
                )
            }
        }

        if (visibleCustomers.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = BasilGreen, modifier = Modifier.size(44.dp))
                    Text(strings.noOrdersYet, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Customer totals, paid amounts, and pending dues appear here automatically once orders are saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            visibleCustomers.forEach { c ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOfNotNull(
                                        c.phone.takeIf { it.isNotBlank() },
                                        "Last: ${Formatters.formatDate(c.lastOrderAt)}"
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(strings.pendingDueLabel, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    Formatters.formatINR(c.pendingDue),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (c.pendingDue > 0) MaterialTheme.colorScheme.error else BasilGreenDark
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "${strings.totalOrdersLabel}: ${c.orderCount}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                "${strings.totalSpentLabel}: ${Formatters.formatINR(c.totalSpent)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                "${strings.totalPaidLabel}: ${Formatters.formatINR(c.totalPaid)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = BasilGreenDark
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (c.pendingDue > 0 && c.phone.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val msg = "Hi ${c.name}, gentle reminder from ${profile.businessName}: pending balance is ${Formatters.formatINR(c.pendingDue)}." +
                                            if (profile.upiId.isNotBlank()) " UPI: ${profile.upiId}" else ""
                                        openWhatsAppMessage(context, c.phone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BasilGreen),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text(strings.sendWhatsAppReminder, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            OutlinedButton(
                                onClick = { onViewCustomerOrders(c) },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(strings.viewOrdersArrow, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageOrderQrTab(
    strings: AppStrings,
    profile: BusinessProfile,
    onShareQrPosterJpg: () -> Unit
) {
    val waUrl = remember(profile) {
        QrCodeGenerator.buildWhatsAppUrl(profile.effectiveWhatsApp, "Hi ${profile.businessName}, I want to place an order!")
    }
    val waQr = remember(waUrl) { QrCodeGenerator.generateQrBitmap(waUrl, 360) }
    val vCardQr = remember(profile) { QrCodeGenerator.generateQrBitmap(QrCodeGenerator.buildVCardString(profile), 320) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(profile.businessName.ifBlank { "ArthFood Kitchen" }, style = MaterialTheme.typography.titleMedium, color = BasilGreenDark)
                if (profile.tagline.isNotBlank()) {
                    Text(profile.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Text(strings.scanToOrderWhatsApp, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                Image(bitmap = waQr.asImageBitmap(), contentDescription = strings.scanToOrderWhatsApp, modifier = Modifier.size(200.dp))
                Text(profile.effectiveWhatsApp.ifBlank { "Set WhatsApp number in Business tab" }, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.scanToSaveContact, style = MaterialTheme.typography.titleSmall)
                Image(bitmap = vCardQr.asImageBitmap(), contentDescription = strings.scanToSaveContact, modifier = Modifier.size(180.dp))
                Text(
                    profile.qrContactName.ifBlank { profile.businessName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onShareQrPosterJpg,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_qr_poster_btn")
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.downloadQrPosterJpg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBackupExcelTab(
    strings: AppStrings,
    licenseState: LicenseState,
    diagnosticState: LicenseDiagnosticState = LicenseDiagnosticState(),
    autoBackupFolderUri: String,
    hasPin: Boolean,
    onExportJsonFile: () -> Unit,
    onCopyBackupText: ((String) -> Unit) -> Unit,
    onInspectBackupJson: (String, (Result<BackupInspectionSummary>) -> Unit) -> Unit,
    onReadUriText: (Uri, (String?) -> Unit) -> Unit,
    onConfirmMergeRestore: (String) -> Unit,
    dateRangeBounds: (DateRangePreset, Long, Long) -> Pair<Long, Long>,
    dateRangeLabel: (DateRangePreset, Long, Long) -> String,
    onExportExcel: (Long, Long, String) -> Unit,
    onPickAutoBackupFolder: (Uri) -> Unit,
    onTriggerAutoBackupNow: () -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onOpenLicenseModal: () -> Unit
) {
    val context = LocalContext.current
    var pasteJson by remember { mutableStateOf("") }
    var pasteJsonErr by remember { mutableStateOf<String?>(null) }
    var inspectionSummary by remember { mutableStateOf<BackupInspectionSummary?>(null) }

    var excelPreset by remember { mutableStateOf(DateRangePreset.ALL_TIME) }
    var excelCustomStart by remember { mutableLongStateOf(System.currentTimeMillis() - 30L * 86_400_000L) }
    var excelCustomEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExcelDatePicker by remember { mutableStateOf(false) }

    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var pinErr by remember { mutableStateOf<String?>(null) }
    var showDisablePinConfirm by remember { mutableStateOf(false) }

    fun inspectAndShowSummary(raw: String) {
        if (raw.isBlank()) {
            pasteJsonErr = "Paste valid backup JSON first"
            return
        }
        onInspectBackupJson(raw) { res ->
            res.onSuccess { summary ->
                pasteJsonErr = null
                inspectionSummary = summary
            }.onFailure { err ->
                pasteJsonErr = "Invalid backup JSON: ${err.message ?: "format error"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onReadUriText(uri) { text ->
                if (!text.isNullOrBlank()) {
                    inspectAndShowSummary(text)
                }
            }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) onPickAutoBackupFolder(uri) }

    val (excelStartMs, excelEndMs) = dateRangeBounds(excelPreset, excelCustomStart, excelCustomEnd)
    val excelRangeLbl = dateRangeLabel(excelPreset, excelCustomStart, excelCustomEnd)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. JSON Backup & Restore (with Check Restore pre-merge inspection)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.manageBackupExcel, style = MaterialTheme.typography.titleMedium, color = BasilGreenDark)
                Text(strings.restoreMergeNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = onExportJsonFile,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_json_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportJsonBackup)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onCopyBackupText { json -> copyToClipboard(context, "ArthFood Backup JSON", json) }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.copyBackupText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.importBackupFile, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                HorizontalDivider()
                OutlinedTextField(
                    value = pasteJson,
                    onValueChange = { pasteJson = it; pasteJsonErr = null },
                    label = { Text("Paste Backup text here") },
                    minLines = 3,
                    maxLines = 6,
                    isError = pasteJsonErr != null,
                    supportingText = pasteJsonErr?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth()
                )
                FilledTonalButton(
                    onClick = { inspectAndShowSummary(pasteJson) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("check_restore_btn")
                ) {
                    Text(strings.checkRestore)
                }
            }
        }

        // 2. 4-Sheet Excel Export with Date Range selector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("4-Sheet Excel Report (.xlsx)", style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                    OutlinedButton(
                        onClick = { showExcelDatePicker = true },
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(excelRangeLbl, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        DateRangePreset.TODAY to strings.today,
                        DateRangePreset.THIS_WEEK to strings.thisWeek,
                        DateRangePreset.THIS_MONTH to strings.thisMonth,
                        DateRangePreset.ALL_TIME to strings.allTime,
                        DateRangePreset.CUSTOM to strings.customRange
                    ).forEach { (p, lbl) ->
                        FilterChip(
                            selected = excelPreset == p,
                            onClick = {
                                excelPreset = p
                                if (p == DateRangePreset.CUSTOM) showExcelDatePicker = true
                            },
                            label = { Text(lbl) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
                Button(
                    onClick = { onExportExcel(excelStartMs, excelEndMs, excelRangeLbl) },
                    colors = ButtonDefaults.buttonColors(containerColor = BasilGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_xlsx_btn")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${strings.downloadExcelReport} · $excelRangeLbl")
                }
            }
        }

        // 3. Auto-Backup Folder (SAF)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.autoBackupFolder, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                Text(
                    text = if (autoBackupFolderUri.isNotBlank()) "Connected folder active (ArthFood_AutoBackup.json)" else "No folder selected — pick a folder for automatic JSON backups after every order",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.chooseFolder)
                    }
                    if (autoBackupFolderUri.isNotBlank()) {
                        FilledTonalButton(
                            onClick = onTriggerAutoBackupNow,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(strings.backupNowToFolder)
                        }
                    }
                }
            }
        }

        // 4. 4-Digit PIN Lock
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = BasilGreenDark)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.pinLockTitle, style = MaterialTheme.typography.titleSmall)
                        Text(strings.enablePinLock, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hasPin,
                        onCheckedChange = { enable ->
                            if (enable) {
                                pinErr = null
                                showPinDialog = true
                            } else {
                                showDisablePinConfirm = true
                            }
                        }
                    )
                }
                if (hasPin) {
                    OutlinedButton(
                        onClick = { pinErr = null; newPin = ""; showPinDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Change 4-Digit PIN")
                    }
                }
            }
        }

        // 5. License Status & Diagnostic Logs Card (Part 4)
        LicenseCard(
            licenseState = licenseState,
            strings = strings,
            onOpenActivation = onOpenLicenseModal,
            diagnosticState = diagnosticState,
            defaultDiagnosticsExpanded = true,
            actionTestTag = "manage_license_btn"
        )
    }

    // Pre-Restore Merge Summary Modal
    inspectionSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { inspectionSummary = null },
            title = { Text("Backup Ready to Merge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = BasilGreenLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Found ${summary.totalOrdersInBackup} order(s) (${summary.newOrdersToImport} new orders will be imported)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BasilGreenDark
                            )
                            Text(
                                "${summary.itemsInBackup} catalog item(s) (${summary.newItemsToImport} new)",
                                style = MaterialTheme.typography.bodySmall,
                                color = BasilGreenDark
                            )
                            Text(
                                "${summary.vendorsInBackup} vendor(s) (${summary.newVendorsToImport} new) • ${summary.packSizesInBackup} pack sizes",
                                style = MaterialTheme.typography.bodySmall,
                                color = BasilGreenDark
                            )
                        }
                    }
                    Text(
                        strings.restoreMergeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onConfirmMergeRestore(summary.jsonText)
                    inspectionSummary = null
                    pasteJson = ""
                }) { Text("Confirm & Merge") }
            },
            dismissButton = {
                TextButton(onClick = { inspectionSummary = null }) { Text(strings.close) }
            }
        )
    }

    if (showExcelDatePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = excelCustomStart,
            initialSelectedEndDateMillis = excelCustomEnd
        )
        DatePickerDialog(
            onDismissRequest = { showExcelDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedStartDateMillis?.let { excelCustomStart = it }
                    state.selectedEndDateMillis?.let { excelCustomEnd = it }
                    excelPreset = DateRangePreset.CUSTOM
                    showExcelDatePicker = false
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showExcelDatePicker = false }) { Text(strings.close) }
            }
        ) {
            DateRangePicker(state = state)
        }
    }

    if (showDisablePinConfirm) {
        ConfirmDialog(
            title = "Disable 4-Digit PIN Lock?",
            message = "Anyone opening this device will be able to access your billing ledger without entering a PIN.",
            confirmLabel = "Disable PIN",
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onDisablePin()
                showDisablePinConfirm = false
            },
            onDismiss = { showDisablePinConfirm = false }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinErr = null },
            title = { Text(strings.enter4DigitPin) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) { newPin = it.filter { c -> c.isDigit() }; pinErr = null } },
                    isError = pinErr != null,
                    supportingText = pinErr?.let { err -> { Text(err) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            pinErr = "Enter all 4 digits"
                        } else {
                            onSetPin(newPin)
                            newPin = ""
                            pinErr = null
                            showPinDialog = false
                        }
                    }
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinErr = null }) { Text(strings.close) }
            }
        )
    }
}
