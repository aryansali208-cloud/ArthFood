package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.DateRangePreset
import com.example.data.model.Vendor
import com.example.data.model.VendorPayableSummary
import com.example.data.model.VendorPayment
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.SaffronLight
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageVendorsTab(
    strings: AppStrings,
    vendors: List<Vendor>,
    catalogItems: List<CatalogItem>,
    vendorPayments: List<VendorPayment>,
    onSaveVendor: (Vendor) -> Unit,
    onDeleteVendor: (Long) -> Unit,
    onAssignVendorToItem: (Long, Vendor?) -> Unit,
    onMarkVendorLinesPaid: (Vendor, Long, Long) -> Unit,
    onRecordPayment: (VendorPayment) -> Unit,
    dateRangeBounds: (DateRangePreset, Long, Long) -> Pair<Long, Long>,
    dateRangeLabel: (DateRangePreset, Long, Long) -> String,
    computePayables: (Long, Long) -> List<VendorPayableSummary>,
    onShareVendorStatement: (VendorPayableSummary, String) -> Unit
) {
    // Vendor directory form
    var vendorName by remember { mutableStateOf("") }
    var vendorNameErr by remember { mutableStateOf<String?>(null) }
    var vendorPhone by remember { mutableStateOf("") }
    var pendingDeleteVendor by remember { mutableStateOf<Vendor?>(null) }
    var showItemMapping by remember { mutableStateOf(true) }

    // Draft & Applied Date Range with explicit Calculate button
    var draftPreset by remember { mutableStateOf(DateRangePreset.THIS_MONTH) }
    var draftCustomStart by remember { mutableLongStateOf(System.currentTimeMillis() - 7L * 86_400_000L) }
    var draftCustomEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    var appliedPreset by remember { mutableStateOf(DateRangePreset.THIS_MONTH) }
    var appliedCustomStart by remember { mutableLongStateOf(draftCustomStart) }
    var appliedCustomEnd by remember { mutableLongStateOf(draftCustomEnd) }

    // Record Payment modal state
    var payDialogVendor by remember { mutableStateOf<VendorPayableSummary?>(null) }
    var payAmount by remember { mutableStateOf("") }
    var payAmountErr by remember { mutableStateOf<String?>(null) }
    var payMode by remember { mutableStateOf("UPI") }
    var payNote by remember { mutableStateOf("") }
    var payDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showPayDatePicker by remember { mutableStateOf(false) }

    // Mark order lines Paid confirmation
    var pendingMarkLinesPaidVendor by remember { mutableStateOf<Vendor?>(null) }

    val (appliedStartMs, appliedEndMs) = dateRangeBounds(appliedPreset, appliedCustomStart, appliedCustomEnd)
    val appliedRangeLabel = dateRangeLabel(appliedPreset, appliedCustomStart, appliedCustomEnd)
    val draftRangeLabel = dateRangeLabel(draftPreset, draftCustomStart, draftCustomEnd)
    val summaries = computePayables(appliedStartMs, appliedEndMs)

    val needsAttentionItems = catalogItems.filter {
        it.vendorCostPerKg <= 0.0 || (it.vendorId == null && it.vendorName.isBlank())
    }

    val totalPayableAll = Formatters.round2(summaries.sumOf { it.totalPayable })
    val totalPaidAll = Formatters.round2(summaries.sumOf { it.totalPaid })
    val totalPendingAll = Formatters.round2(summaries.sumOf { it.balanceDue })

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Needs Attention Alert Banner
            if (needsAttentionItems.isNotEmpty()) {
                Surface(
                    color = SaffronLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                strings.needsAttentionTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                "Missing vendor or cost ₹/kg on: ${needsAttentionItems.joinToString(", ") { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // 1. Vendor Directory & Catalog Item Mapping Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.addVendor, style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vendorName,
                            onValueChange = { vendorName = it; vendorNameErr = null },
                            label = { Text(strings.vendorNameLabel) },
                            isError = vendorNameErr != null,
                            supportingText = vendorNameErr?.let { err -> { Text(err) } },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = vendorPhone,
                            onValueChange = { vendorPhone = it },
                            label = { Text(strings.phoneNumber) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            if (vendorName.isBlank()) {
                                vendorNameErr = "Enter vendor name"
                            } else {
                                onSaveVendor(Vendor(name = vendorName.trim(), phone = vendorPhone.trim()))
                                vendorName = ""
                                vendorPhone = ""
                                vendorNameErr = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_vendor_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addVendor)
                    }

                    if (vendors.isNotEmpty()) {
                        HorizontalDivider()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            vendors.forEach { v ->
                                InputChip(
                                    selected = false,
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (v.phone.isNotBlank()) "${v.name} · ${v.phone}" else v.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete ${v.name}",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { pendingDeleteVendor = v }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (catalogItems.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showItemMapping = !showItemMapping }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Catalog Item → Default Vendor (${catalogItems.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (showItemMapping) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                        if (showItemMapping) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                catalogItems.forEach { item ->
                                    var expanded by remember(item.id) { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                "Cost ${Formatters.formatINR(item.vendorCostPerKg)}/kg",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                color = if (item.vendorCostPerKg <= 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box {
                                            OutlinedButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Text(
                                                    item.vendorName.ifBlank { "Assign Vendor" },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1
                                                )
                                                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("None (Unassigned)") },
                                                    onClick = {
                                                        onAssignVendorToItem(item.id, null)
                                                        expanded = false
                                                    }
                                                )
                                                vendors.forEach { v ->
                                                    DropdownMenuItem(
                                                        text = { Text(v.name) },
                                                        onClick = {
                                                            onAssignVendorToItem(item.id, v)
                                                            expanded = false
                                                        }
                                                    )
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

            // 2. Date Range Selector + Calculate CTA
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Payable Period: $appliedRangeLabel", style = MaterialTheme.typography.titleSmall, color = BasilGreenDark)
                        OutlinedButton(
                            onClick = { showDateRangePicker = true },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(draftRangeLabel, style = MaterialTheme.typography.labelSmall)
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
                                selected = draftPreset == p,
                                onClick = {
                                    draftPreset = p
                                    if (p == DateRangePreset.CUSTOM) {
                                        showDateRangePicker = true
                                    } else {
                                        appliedPreset = p
                                    }
                                },
                                label = { Text(lbl) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            appliedPreset = draftPreset
                            appliedCustomStart = draftCustomStart
                            appliedCustomEnd = draftCustomEnd
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("vendor_calculate_btn")
                    ) {
                        Text("Calculate Payables ($draftRangeLabel)")
                    }
                }
            }

            // 3. Vendor Ledger Cards
            if (summaries.isEmpty()) {
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
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = BasilGreen, modifier = Modifier.size(40.dp))
                        Text("No vendors configured yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add vendors above and map them to your catalog items to track payables automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            summaries.forEach { summary ->
                val recentVendorPayments = remember(vendorPayments, summary.vendor.id, appliedStartMs, appliedEndMs) {
                    vendorPayments.filter {
                        (it.vendorId == summary.vendor.id || it.vendorName.equals(summary.vendor.name, ignoreCase = true)) &&
                            it.paymentDate in appliedStartMs..appliedEndMs
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header + Paid / Pending pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.vendor.name, style = MaterialTheme.typography.titleMedium)
                                if (summary.vendor.phone.isNotBlank()) {
                                    Text(
                                        summary.vendor.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(color = BasilGreenLight, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "${strings.totalPaidLabel}: ${Formatters.formatINR(summary.totalPaid)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = BasilGreenDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = if (summary.balanceDue > 0) MaterialTheme.colorScheme.errorContainer else BasilGreenLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Pending: ${Formatters.formatINR(summary.balanceDue)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.balanceDue > 0) MaterialTheme.colorScheme.error else BasilGreenDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Itemized table
                        if (summary.packBreakdown.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                "${strings.packWiseSummary} (${Formatters.formatGrams(summary.totalGrams)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = BasilGreenDark
                            )
                            summary.packBreakdown.forEach { pack ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${pack.itemName} (${Formatters.formatGrams(pack.packGrams)} × ${pack.quantity})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "@ ${Formatters.formatINR(pack.vendorCostPerKg)}/kg",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        Formatters.formatINR(pack.costTotal),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Surface(
                                        color = if (pack.allPaid) BasilGreenLight else SaffronLight,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            if (pack.allPaid) "PAID" else "PENDING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pack.allPaid) BasilGreenDark else Color(0xFF92400E),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (recentVendorPayments.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Recorded Payments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            recentVendorPayments.take(4).forEach { vp ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "${Formatters.formatDate(vp.paymentDate)} · ${vp.mode} ${vp.note}".trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        Formatters.formatINR(vp.amount),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        color = BasilGreenDark
                                    )
                                }
                            }
                        }

                        // Action Buttons Row
                        if (summary.hasUnpaidLines) {
                            OutlinedButton(
                                onClick = { pendingMarkLinesPaidVendor = summary.vendor },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.markOrderLinesPaid)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    payDialogVendor = summary
                                    payAmount = if (summary.balanceDue > 0) Formatters.round2(summary.balanceDue).toString() else ""
                                    payAmountErr = null
                                    payDateMs = System.currentTimeMillis()
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.recordPayment, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { onShareVendorStatement(summary, appliedRangeLabel) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.jpgForThisVendor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Aggregate Totals Bar
        if (summaries.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.totalPayable, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatINR(totalPayableAll),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(strings.totalPaidLabel, style = MaterialTheme.typography.labelSmall, color = BasilGreenDark)
                        Text(
                            Formatters.formatINR(totalPaidAll),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = BasilGreenDark
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pending", style = MaterialTheme.typography.labelSmall, color = if (totalPendingAll > 0) MaterialTheme.colorScheme.error else BasilGreenDark)
                        Text(
                            Formatters.formatINR(totalPendingAll),
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = if (totalPendingAll > 0) MaterialTheme.colorScheme.error else BasilGreenDark
                        )
                    }
                }
            }
        }
    }

    // Record Payment Dialog with Payment Date Picker
    payDialogVendor?.let { target ->
        AlertDialog(
            onDismissRequest = { payDialogVendor = null; payAmountErr = null },
            title = { Text("${strings.recordPayment}: ${target.vendor.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it; payAmountErr = null },
                        label = { Text(strings.partialPaidAmount) },
                        isError = payAmountErr != null,
                        supportingText = payAmountErr?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { showPayDatePicker = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Payment Date: ${Formatters.formatDate(payDateMs)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("UPI", "Cash", "Bank").forEach { modeOpt ->
                            FilterChip(
                                selected = payMode.equals(modeOpt, ignoreCase = true),
                                onClick = { payMode = modeOpt },
                                label = { Text(modeOpt) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Note / Adjustment (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = payAmount.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        payAmountErr = "Enter valid payment amount (> ₹0)"
                    } else {
                        onRecordPayment(
                            VendorPayment(
                                vendorId = target.vendor.id,
                                vendorName = target.vendor.name,
                                amount = Formatters.round2(amt),
                                paymentDate = payDateMs,
                                mode = payMode.ifBlank { "UPI" },
                                note = payNote.trim()
                            )
                        )
                        payDialogVendor = null
                        payAmountErr = null
                        payNote = ""
                    }
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { payDialogVendor = null; payAmountErr = null }) { Text(strings.close) }
            }
        )
    }

    if (showPayDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = payDateMs)
        DatePickerDialog(
            onDismissRequest = { showPayDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { payDateMs = it }
                    showPayDatePicker = false
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showPayDatePicker = false }) { Text(strings.close) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showDateRangePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = draftCustomStart,
            initialSelectedEndDateMillis = draftCustomEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedStartDateMillis?.let { draftCustomStart = it }
                    state.selectedEndDateMillis?.let { draftCustomEnd = it }
                    draftPreset = DateRangePreset.CUSTOM
                    appliedPreset = DateRangePreset.CUSTOM
                    appliedCustomStart = draftCustomStart
                    appliedCustomEnd = draftCustomEnd
                    showDateRangePicker = false
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text(strings.close) }
            }
        ) {
            DateRangePicker(state = state)
        }
    }

    pendingDeleteVendor?.let { target ->
        ConfirmDialog(
            title = "Delete Vendor ${target.name}?",
            message = "Historical saved orders keep their recorded vendor cost and name. Catalog items linked to this vendor can be reassigned.",
            confirmLabel = "Delete",
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onDeleteVendor(target.id)
                pendingDeleteVendor = null
            },
            onDismiss = { pendingDeleteVendor = null }
        )
    }

    pendingMarkLinesPaidVendor?.let { target ->
        ConfirmDialog(
            title = "Mark all ${target.name} order lines PAID?",
            message = "All active order item lines for ${target.name} in \"$appliedRangeLabel\" will be marked PAID.",
            confirmLabel = "Mark Paid",
            dismissLabel = strings.close,
            onConfirm = {
                onMarkVendorLinesPaid(target, appliedStartMs, appliedEndMs)
                pendingMarkLinesPaidVendor = null
            },
            onDismiss = { pendingMarkLinesPaidVendor = null }
        )
    }
}
