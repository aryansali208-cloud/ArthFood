package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.DateRangePreset
import com.example.data.model.ProfitOrderScope
import com.example.data.model.ProfitSummary
import com.example.data.model.Vendor
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.SaffronLight
import com.example.util.Formatters
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManageRatesProfitTab(
    strings: AppStrings,
    lockedRates: Boolean,
    stockTracking: Boolean,
    packSizes: List<Int>,
    catalogItems: List<CatalogItem>,
    vendors: List<Vendor>,
    onToggleLockedRates: (Boolean) -> Unit,
    onToggleStockTracking: (Boolean) -> Unit,
    onSaveCatalogItem: (CatalogItem) -> Unit,
    onDeleteCatalogItem: (Long) -> Unit,
    onAddStockToItem: (Long, Int) -> Unit,
    dateRangeBounds: (DateRangePreset, Long, Long) -> Pair<Long, Long>,
    dateRangeLabel: (DateRangePreset, Long, Long) -> String,
    computeProfit: (Long, Long, ProfitOrderScope) -> ProfitSummary,
    onShareProfitJpg: (ProfitSummary, String) -> Unit
) {
    // Catalog Editor state
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    var itemName by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var vendorCost by remember { mutableStateOf("") }
    var selectedVendorId by remember { mutableStateOf<Long?>(null) }
    var vendorName by remember { mutableStateOf("") }
    var showVendorDropdown by remember { mutableStateOf(false) }
    var itemTrackStock by remember { mutableStateOf(false) }
    var stockGrams by remember { mutableStateOf("") }
    var lowAlertGrams by remember { mutableStateOf("1000") }
    var showPackOverrides by remember { mutableStateOf(false) }
    val packPriceInputs = remember { mutableStateMapOf<Int, String>() }

    var itemNameErr by remember { mutableStateOf<String?>(null) }
    var sellPriceErr by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<CatalogItem?>(null) }
    var restockTarget by remember { mutableStateOf<CatalogItem?>(null) }
    var restockDeltaText by remember { mutableStateOf("") }
    var restockErr by remember { mutableStateOf<String?>(null) }

    // Profit Calculator state
    var preset by remember { mutableStateOf(DateRangePreset.THIS_MONTH) }
    var profitScope by remember { mutableStateOf(ProfitOrderScope.ALL_ACTIVE) }
    var customStart by remember { mutableLongStateOf(System.currentTimeMillis() - 7L * 86_400_000L) }
    var customEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val (startMs, endMs) = dateRangeBounds(preset, customStart, customEnd)
    val profit = computeProfit(startMs, endMs, profitScope)
    val rangeLbl = dateRangeLabel(preset, customStart, customEnd)
    val missingCostCount = catalogItems.count { it.vendorCostPerKg <= 0.0 }

    fun resetItemForm() {
        editingItem = null
        itemName = ""
        sellPrice = ""
        vendorCost = ""
        selectedVendorId = null
        vendorName = ""
        itemTrackStock = false
        stockGrams = ""
        lowAlertGrams = "1000"
        showPackOverrides = false
        packPriceInputs.clear()
        itemNameErr = null
        sellPriceErr = null
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Profit Analytics Card
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
                    Text(strings.profitCalculator, style = MaterialTheme.typography.titleMedium, color = BasilGreenDark)
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(rangeLbl, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Date presets
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
                            selected = preset == p,
                            onClick = {
                                preset = p
                                if (p == DateRangePreset.CUSTOM) showDatePicker = true
                            },
                            label = { Text(lbl) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                // Which orders to count scope chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        strings.whichOrdersToCount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ProfitOrderScope.ALL_ACTIVE to strings.allOrdersExclCancelled,
                            ProfitOrderScope.SENT_ONLY to strings.orderSentOnly,
                            ProfitOrderScope.PAID_ONLY to strings.paymentReceivedOnly
                        ).forEach { (scope, label) ->
                            FilterChip(
                                selected = profitScope == scope,
                                onClick = { profitScope = scope },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                if (missingCostCount > 0 || profit.missingVendorOrCostLines > 0) {
                    Surface(color = SaffronLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            val msg = if (missingCostCount > 0) {
                                strings.missingCostAlert(missingCostCount)
                            } else {
                                "${profit.missingVendorOrCostLines} order item line(s) in this period are missing vendor or vendor cost."
                            }
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(
                        strings.totalOrdersLabel,
                        "${profit.orderCount} (+${profit.freeSampleCount} free)",
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                    StatBox(
                        strings.marginPercent,
                        String.format(Locale.getDefault(), "%.1f%%", profit.marginPercent),
                        BasilGreenLight,
                        BasilGreenDark,
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(strings.totalSales, Formatters.formatINR(profit.totalSales), BasilGreenLight, BasilGreenDark, Modifier.weight(1f))
                    StatBox(strings.totalVendorCost, Formatters.formatINR(profit.totalVendorCost), SaffronLight, Color(0xFF92400E), Modifier.weight(1f))
                }
                StatBox(
                    strings.netProfit,
                    Formatters.formatINR(profit.netProfit),
                    if (profit.netProfit >= 0) BasilGreenLight else MaterialTheme.colorScheme.errorContainer,
                    if (profit.netProfit >= 0) BasilGreenDark else MaterialTheme.colorScheme.error,
                    Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onShareProfitJpg(profit, rangeLbl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_profit_jpg_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.profitReportJpgBtn)
                }
            }
        }

        // 2. Catalog, Rates Lock & Stock Control Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.catalogItemsTitle, style = MaterialTheme.typography.titleMedium, color = BasilGreenDark)
                        Text(
                            if (lockedRates) strings.ratesLockedBanner else "Unlocked — tap + Add Item or edit items below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (!lockedRates) resetItemForm()
                            onToggleLockedRates(!lockedRates)
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("toggle_lock_rates_btn")
                    ) {
                        Icon(
                            imageVector = if (lockedRates) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (lockedRates) strings.unlockToEdit else strings.lockRatesBtn)
                    }
                }

                Surface(color = BasilGreenLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = BasilGreenDark, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.ratesOnlyNewOrdersNotice, style = MaterialTheme.typography.bodySmall, color = BasilGreenDark)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.stockTrackingToggle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = stockTracking, onCheckedChange = onToggleStockTracking)
                }
            }
        }

        // 3. Add / Edit Catalog Item Card (when unlocked)
        if (!lockedRates) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BasilGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (editingItem != null) "${strings.edit}: ${editingItem!!.name}" else strings.addItem,
                        style = MaterialTheme.typography.titleSmall,
                        color = BasilGreenDark
                    )
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it; itemNameErr = null },
                        label = { Text(strings.itemName) },
                        isError = itemNameErr != null,
                        supportingText = itemNameErr?.let { err -> { Text(err) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellPrice,
                            onValueChange = { sellPrice = it; sellPriceErr = null },
                            label = { Text(strings.sellingPricePerKg) },
                            isError = sellPriceErr != null,
                            supportingText = sellPriceErr?.let { err -> { Text(err) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vendorCost,
                            onValueChange = { vendorCost = it },
                            label = { Text(strings.vendorCostPerKg) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Vendor input + quick selector from existing Vendors
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = vendorName,
                            onValueChange = { vendorName = it; selectedVendorId = null },
                            label = { Text(strings.vendorNameLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (vendors.isNotEmpty()) {
                            androidx.compose.foundation.layout.Box {
                                OutlinedButton(
                                    onClick = { showVendorDropdown = true },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Pick")
                                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showVendorDropdown,
                                    onDismissRequest = { showVendorDropdown = false }
                                ) {
                                    vendors.forEach { v ->
                                        DropdownMenuItem(
                                            text = { Text(v.name) },
                                            onClick = {
                                                selectedVendorId = v.id
                                                vendorName = v.name
                                                showVendorDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Per-item Track Stock toggle
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.trackStockPerItem, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = itemTrackStock, onCheckedChange = { itemTrackStock = it })
                    }

                    if (stockTracking || itemTrackStock) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stockGrams,
                                onValueChange = { stockGrams = it.filter { c -> c.isDigit() } },
                                label = { Text(strings.stockGramsLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lowAlertGrams,
                                onValueChange = { lowAlertGrams = it.filter { c -> c.isDigit() } },
                                label = { Text(strings.lowStockAlertLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Collapsible Pack prices (optional)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPackOverrides = !showPackOverrides }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.packPricesOptional, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (showPackOverrides) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    AnimatedVisibility(visible = showPackOverrides) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Leave blank to auto-calculate from ₹/kg rate.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            packSizes.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { g ->
                                        OutlinedTextField(
                                            value = packPriceInputs[g] ?: "",
                                            onValueChange = { packPriceInputs[g] = it },
                                            label = { Text("${Formatters.formatGrams(g)} (₹)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingItem != null) {
                            OutlinedButton(
                                onClick = { resetItemForm() },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text(strings.cancelOrder) }
                        }
                        Button(
                            onClick = {
                                val sell = sellPrice.toDoubleOrNull() ?: 0.0
                                val cost = vendorCost.toDoubleOrNull() ?: 0.0
                                var valid = true
                                if (itemName.isBlank()) {
                                    itemNameErr = "Enter item name"
                                    valid = false
                                }
                                if (sell <= 0.0) {
                                    sellPriceErr = "Rate must be > ₹0"
                                    valid = false
                                }
                                if (valid) {
                                    val overrideJson = buildPackOverrideJson(packPriceInputs)
                                    onSaveCatalogItem(
                                        CatalogItem(
                                            id = editingItem?.id ?: 0L,
                                            name = itemName.trim(),
                                            sellingPricePerKg = Formatters.round2(sell),
                                            vendorCostPerKg = Formatters.round2(cost),
                                            vendorId = selectedVendorId ?: editingItem?.vendorId,
                                            vendorName = vendorName.trim(),
                                            stockGrams = stockGrams.toIntOrNull() ?: (editingItem?.stockGrams ?: 0),
                                            lowStockThresholdGrams = lowAlertGrams.toIntOrNull() ?: 1000,
                                            trackStock = itemTrackStock,
                                            packPriceOverridesJson = overrideJson
                                        )
                                    )
                                    resetItemForm()
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("save_catalog_item_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (editingItem != null) strings.saveChanges else strings.addItem)
                        }
                    }
                }
            }
        }

        // 4. Catalog Items List
        catalogItems.forEach { item ->
            val tracksThisItem = stockTracking || item.trackStock
            val isLowStock = tracksThisItem && item.stockGrams <= item.lowStockThresholdGrams
            val overrides = remember(item.packPriceOverridesJson) {
                CatalogItem.parsePackOverrides(item.packPriceOverridesJson)
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                if (isLowStock) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            strings.lowStockBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "Sell ${Formatters.formatINR(item.sellingPricePerKg)}/kg • Cost ${Formatters.formatINR(item.vendorCostPerKg)}/kg" +
                                    if (item.vendorName.isNotBlank()) " • ${item.vendorName}" else "",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!lockedRates) {
                            IconButton(
                                onClick = {
                                    editingItem = item
                                    itemName = item.name
                                    sellPrice = item.sellingPricePerKg.toString()
                                    vendorCost = item.vendorCostPerKg.toString()
                                    selectedVendorId = item.vendorId
                                    vendorName = item.vendorName
                                    itemTrackStock = item.trackStock
                                    stockGrams = item.stockGrams.toString()
                                    lowAlertGrams = item.lowStockThresholdGrams.toString()
                                    packPriceInputs.clear()
                                    CatalogItem.parsePackOverrides(item.packPriceOverridesJson).forEach { (g, price) ->
                                        packPriceInputs[g] = price.toString()
                                    }
                                    showPackOverrides = packPriceInputs.isNotEmpty()
                                    itemNameErr = null
                                    sellPriceErr = null
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                            }
                            IconButton(onClick = { pendingDeleteItem = item }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                            }
                        }
                    }

                    if (overrides.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            overrides.entries.sortedBy { it.key }.forEach { (g, price) ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        "${Formatters.formatGrams(g)}: ${Formatters.formatINR(price)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (tracksThisItem) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isLowStock) MaterialTheme.colorScheme.error else BasilGreenDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "${strings.availableStockLabel}: ${Formatters.formatGrams(item.stockGrams)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLowStock) MaterialTheme.colorScheme.error else BasilGreenDark
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    restockTarget = item
                                    restockDeltaText = ""
                                    restockErr = null
                                },
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text(strings.addStockBtn, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Restock Modal
    restockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restockTarget = null; restockErr = null },
            title = { Text("${strings.addStockBtn}: ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${strings.availableStockLabel}: ${Formatters.formatGrams(target.stockGrams)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(500 to "+500 g", 1000 to "+1 kg", 5000 to "+5 kg").forEach { (delta, lbl) ->
                            OutlinedButton(
                                onClick = {
                                    val cur = restockDeltaText.toIntOrNull() ?: 0
                                    restockDeltaText = (cur + delta).toString()
                                    restockErr = null
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                            ) { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    OutlinedTextField(
                        value = restockDeltaText,
                        onValueChange = { restockDeltaText = it.filter { c -> c.isDigit() }; restockErr = null },
                        label = { Text("Add Weight (grams)") },
                        isError = restockErr != null,
                        supportingText = restockErr?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val delta = restockDeltaText.toIntOrNull()
                    if (delta == null || delta <= 0) {
                        restockErr = "Enter grams > 0"
                    } else {
                        onAddStockToItem(target.id, delta)
                        restockTarget = null
                        restockErr = null
                    }
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { restockTarget = null; restockErr = null }) { Text(strings.close) }
            }
        )
    }

    pendingDeleteItem?.let { target ->
        ConfirmDialog(
            title = "Remove ${target.name}?",
            message = "This removes the item from your catalog. Existing saved orders keep their historical item rates and vendor.",
            confirmLabel = "Remove",
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onDeleteCatalogItem(target.id)
                if (editingItem?.id == target.id) resetItemForm()
                pendingDeleteItem = null
            },
            onDismiss = { pendingDeleteItem = null }
        )
    }

    if (showDatePicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart,
            initialSelectedEndDateMillis = customEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedStartDateMillis?.let { customStart = it }
                    state.selectedEndDateMillis?.let { customEnd = it }
                    preset = DateRangePreset.CUSTOM
                    showDatePicker = false
                }) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.close) }
            }
        ) {
            DateRangePicker(state = state)
        }
    }
}

private fun buildPackOverrideJson(inputs: Map<Int, String>): String {
    val obj = JSONObject()
    inputs.forEach { (g, v) ->
        val num = v.toDoubleOrNull()
        if (num != null && num > 0.0) {
            obj.put(g.toString(), Formatters.round2(num))
        }
    }
    return if (obj.length() == 0) "" else obj.toString()
}

@Composable
private fun StatBox(title: String, value: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}
