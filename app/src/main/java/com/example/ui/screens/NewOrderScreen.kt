package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.CatalogItem
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.ParsedWhatsAppOrder
import com.example.data.model.PaymentStatus
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.SaffronLight
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewOrderScreen(
    strings: AppStrings,
    profile: BusinessProfile,
    catalogItems: List<CatalogItem>,
    packSizes: List<Int>,
    editingOrder: OrderRecord?,
    onParseWhatsApp: (String) -> ParsedWhatsAppOrder,
    onShowToast: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveOrder: (OrderRecord, Boolean) -> Unit
) {
    val isEditing = editingOrder != null
    BackHandler(enabled = isEditing) {
        onCancelEdit()
    }

    var showWhatsAppModal by remember { mutableStateOf(false) }
    var waRawText by remember { mutableStateOf("") }
    var parsedPreview by remember { mutableStateOf<ParsedWhatsAppOrder?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    var customerName by remember(editingOrder?.id) { mutableStateOf(editingOrder?.customerName ?: "") }
    var customerNameErr by remember(editingOrder?.id) { mutableStateOf<String?>(null) }
    var customerPhone by remember(editingOrder?.id) { mutableStateOf(editingOrder?.customerPhone ?: "") }
    var customerAddress by remember(editingOrder?.id) { mutableStateOf(editingOrder?.customerAddress ?: "") }
    var orderDateMs by remember(editingOrder?.id) { mutableLongStateOf(editingOrder?.createdAt ?: System.currentTimeMillis()) }
    var notes by remember(editingOrder?.id) { mutableStateOf(editingOrder?.notes ?: "") }

    var deliveryText by remember(editingOrder?.id) {
        mutableStateOf(if ((editingOrder?.deliveryCharge ?: 0.0) > 0) editingOrder!!.deliveryCharge.toString() else "")
    }
    var otherText by remember(editingOrder?.id) {
        mutableStateOf(if ((editingOrder?.otherCharge ?: 0.0) > 0) editingOrder!!.otherCharge.toString() else "")
    }
    var discountText by remember(editingOrder?.id) {
        mutableStateOf(if ((editingOrder?.discount ?: 0.0) > 0) editingOrder!!.discount.toString() else "")
    }
    var paymentStatus by remember(editingOrder?.id) {
        mutableStateOf(editingOrder?.paymentStatus ?: PaymentStatus.UNPAID)
    }
    var partialPaidText by remember(editingOrder?.id) {
        mutableStateOf(if ((editingOrder?.paidAmount ?: 0.0) > 0) editingOrder!!.paidAmount.toString() else "")
    }

    val lineItems = remember(editingOrder?.id) {
        mutableStateListOf<OrderLineItem>().apply {
            if (editingOrder != null) addAll(editingOrder.items)
        }
    }

    // Item Picker state
    var itemMenuExpanded by remember { mutableStateOf(false) }
    var selectedCatalogItem by remember(catalogItems) { mutableStateOf(catalogItems.firstOrNull()) }
    var selectedPackGrams by remember(packSizes) {
        mutableIntStateOf(packSizes.firstOrNull { it == 250 } ?: packSizes.firstOrNull() ?: 250)
    }
    var qty by remember { mutableIntStateOf(1) }
    var isFreeSample by remember { mutableStateOf(false) }

    val autoPackPrice = remember(selectedCatalogItem, selectedPackGrams) {
        selectedCatalogItem?.priceForPack(selectedPackGrams) ?: 0.0
    }
    var customUnitPriceText by remember(selectedCatalogItem, selectedPackGrams) {
        mutableStateOf(if (autoPackPrice > 0) autoPackPrice.toString() else "")
    }
    var unitPriceErr by remember { mutableStateOf<String?>(null) }

    val subtotal = Formatters.round2(lineItems.sumOf { it.lineSaleTotal })
    val delivery = Formatters.round2(deliveryText.toDoubleOrNull() ?: 0.0)
    val other = Formatters.round2(otherText.toDoubleOrNull() ?: 0.0)
    val discount = Formatters.round2(discountText.toDoubleOrNull() ?: 0.0)
    val taxPercent = editingOrder?.taxPercent ?: profile.taxPercent
    val taxBase = Formatters.round2((subtotal + delivery + other - discount).coerceAtLeast(0.0))
    val taxAmount = Formatters.round2((taxBase * taxPercent) / 100.0)
    val grandTotal = Formatters.round2((taxBase + taxAmount).coerceAtLeast(0.0))
    val totalVendorCost = Formatters.round2(lineItems.sumOf { it.lineCostTotal })

    // Free sample summary math
    val freeSampleItems = lineItems.filter { it.isFreeSample }
    val freeSamplePacks = freeSampleItems.sumOf { it.quantity }
    val freeSampleRateWorth = Formatters.round2(
        freeSampleItems.sumOf { ((it.sellingPricePerKg * it.packGrams) / 1000.0) * it.quantity }
    )
    val freeSampleCost = Formatters.round2(freeSampleItems.sumOf { it.lineCostTotal })

    fun resetDraft() {
        customerName = ""
        customerNameErr = null
        customerPhone = ""
        customerAddress = ""
        orderDateMs = System.currentTimeMillis()
        notes = ""
        deliveryText = ""
        otherText = ""
        discountText = ""
        paymentStatus = PaymentStatus.UNPAID
        partialPaidText = ""
        lineItems.clear()
        qty = 1
        isFreeSample = false
        unitPriceErr = null
    }

    fun buildOrderRecord(): OrderRecord = OrderRecord(
        id = editingOrder?.id ?: 0L,
        invoiceNumber = editingOrder?.invoiceNumber ?: "",
        customerName = customerName.trim(),
        customerPhone = customerPhone.trim(),
        customerAddress = customerAddress.trim(),
        notes = notes.trim(),
        status = editingOrder?.status ?: OrderStatus.PENDING,
        paymentStatus = paymentStatus,
        paidAmount = when (paymentStatus) {
            PaymentStatus.PAID -> grandTotal
            PaymentStatus.UNPAID -> 0.0
            PaymentStatus.PARTIAL -> Formatters.round2(partialPaidText.toDoubleOrNull() ?: 0.0)
        },
        deliveryCharge = delivery,
        otherCharge = other,
        discount = discount,
        taxPercent = taxPercent,
        subtotal = subtotal,
        taxAmount = taxAmount,
        grandTotal = grandTotal,
        totalVendorCost = totalVendorCost,
        sentAt = editingOrder?.sentAt ?: 0L,
        paidAt = editingOrder?.paidAt ?: 0L,
        createdAt = orderDateMs,
        items = lineItems.toList()
    )

    fun attemptSave(generateInvoiceAfter: Boolean) {
        if (customerName.isBlank()) {
            customerNameErr = "Customer name is required"
            onShowToast("Please enter customer name")
            return
        }
        if (lineItems.isEmpty()) {
            onShowToast("Add at least 1 item to save the order")
            return
        }
        onSaveOrder(buildOrderRecord(), generateInvoiceAfter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Edit-mode Banner
            if (isEditing && editingOrder != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SaffronLight,
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().testTag("editing_order_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.editingOrderBanner("#${editingOrder.invoiceNumber}"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onCancelEdit) {
                            Text(strings.cancelOrder)
                        }
                    }
                }
            } else {
                Text(
                    text = strings.newOrderTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Section 1: Customer Details + Import WhatsApp Order Modal Trigger
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
                        StepNumberBadge(1)
                        Text(
                            text = strings.customerDetailsSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FilledTonalButton(
                        onClick = { showWhatsAppModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("toggle_whatsapp_parser")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.importWhatsAppOrder)
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it; customerNameErr = null },
                        label = { Text("${strings.customerName} *") },
                        isError = customerNameErr != null,
                        supportingText = customerNameErr?.let { err -> { Text(err) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("order_customer_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(strings.customerPhone) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("order_customer_phone")
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = Formatters.formatDate(orderDateMs),
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text(strings.orderDateLabel) },
                                trailingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick order date", modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("order_date_field")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text(strings.customerAddress) },
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("order_customer_address")
                    )
                }
            }

            // Section 2: Choose Items & Packs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StepNumberBadge(2)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.chooseItemsSection,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.ratesPerKgHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (catalogItems.isEmpty()) {
                        Text(
                            text = "No catalog items yet. Add items in Manage → Rates & Profit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = itemMenuExpanded,
                            onExpandedChange = { itemMenuExpanded = !itemMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCatalogItem?.let {
                                    "${it.name} (${Formatters.formatINR(it.sellingPricePerKg)}/kg)"
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.selectItem) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = itemMenuExpanded,
                                onDismissRequest = { itemMenuExpanded = false }
                            ) {
                                catalogItems.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.name} — ${Formatters.formatINR(item.sellingPricePerKg)}/kg") },
                                        onClick = {
                                            selectedCatalogItem = item
                                            itemMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(strings.selectPack, style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            packSizes.forEach { g ->
                                val packRate = selectedCatalogItem?.priceForPack(g) ?: 0.0
                                FilterChip(
                                    selected = selectedPackGrams == g,
                                    onClick = { selectedPackGrams = g },
                                    label = {
                                        Text(
                                            text = if (packRate > 0) {
                                                "${Formatters.formatGrams(g)} • ${Formatters.formatINR(packRate)}"
                                            } else {
                                                Formatters.formatGrams(g)
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customUnitPriceText,
                                onValueChange = { customUnitPriceText = it; unitPriceErr = null },
                                label = { Text("Pack ₹") },
                                isError = unitPriceErr != null,
                                supportingText = unitPriceErr?.let { err -> { Text(err) } },
                                enabled = !isFreeSample,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (qty > 1) qty-- },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease qty")
                                }
                                Text(
                                    text = "$qty",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { qty++ },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase qty")
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { isFreeSample = !isFreeSample; unitPriceErr = null }
                        ) {
                            Checkbox(checked = isFreeSample, onCheckedChange = { isFreeSample = it; unitPriceErr = null })
                            Text(strings.freeSampleCheckbox, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val cat = selectedCatalogItem ?: return@FilledTonalButton
                                    val parsedPrice = customUnitPriceText.toDoubleOrNull()
                                    if (!isFreeSample && (parsedPrice == null || parsedPrice <= 0.0)) {
                                        unitPriceErr = "Enter valid pack price (> ₹0)"
                                        return@FilledTonalButton
                                    }
                                    val unitPrice = if (isFreeSample) 0.0 else Formatters.round2(parsedPrice ?: autoPackPrice)
                                    lineItems.add(
                                        OrderLineItem(
                                            catalogItemId = cat.id,
                                            itemName = cat.name,
                                            packGrams = selectedPackGrams,
                                            quantity = qty,
                                            unitPrice = unitPrice,
                                            sellingPricePerKg = cat.sellingPricePerKg,
                                            vendorCostPerKg = cat.vendorCostPerKg,
                                            vendorId = cat.vendorId,
                                            vendorName = cat.vendorName,
                                            isFreeSample = isFreeSample
                                        )
                                    )
                                    qty = 1
                                    isFreeSample = false
                                    unitPriceErr = null
                                },
                                modifier = Modifier.weight(1.4f).heightIn(min = 48.dp).testTag("add_item_to_order_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.addToOrder)
                            }

                            OutlinedButton(
                                onClick = {
                                    val hasUnsaved = customerName.isNotBlank() || customerPhone.isNotBlank() ||
                                        customerAddress.isNotBlank() || lineItems.isNotEmpty()
                                    if (hasUnsaved) {
                                        showClearConfirm = true
                                    } else {
                                        resetDraft()
                                    }
                                },
                                modifier = Modifier.weight(0.75f).heightIn(min = 48.dp).testTag("clear_order_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.clearOrderBtn)
                            }
                        }
                    }

                    // Added Line Items List
                    if (lineItems.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lineItems.forEachIndexed { idx, li ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${li.itemName} (${Formatters.formatGrams(li.packGrams)} × ${li.quantity})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (li.isFreeSample) {
                                                    "${strings.freeSampleBadge} • ₹0.00 (Cost ${Formatters.formatINR(li.lineCostTotal)})"
                                                } else {
                                                    "${Formatters.formatINR(li.unitPrice)} × ${li.quantity} = ${Formatters.formatINR(li.lineSaleTotal)}"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                                color = if (li.isFreeSample) Color(0xFF92400E) else BasilGreenDark
                                            )
                                        }
                                        IconButton(
                                            onClick = { lineItems.removeAt(idx) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove line item")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Extra Charges & Grand Total
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StepNumberBadge(3)
                        Text(
                            text = strings.extraChargesSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deliveryText,
                            onValueChange = { deliveryText = it },
                            label = { Text(strings.deliveryCharge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = otherText,
                            onValueChange = { otherText = it },
                            label = { Text(strings.otherCharge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text(strings.discountLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(strings.paymentLabel, style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentStatus.entries.forEach { ps ->
                            FilterChip(
                                selected = paymentStatus == ps,
                                onClick = { paymentStatus = ps },
                                label = { Text(strings.paymentName(ps)) }
                            )
                        }
                    }
                    if (paymentStatus == PaymentStatus.PARTIAL) {
                        OutlinedTextField(
                            value = partialPaidText,
                            onValueChange = { partialPaidText = it },
                            label = { Text(strings.partialPaidAmount) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.orderNotes) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Financial Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BasilGreenLight.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, BasilGreen.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SummaryLine(strings.subtotal, Formatters.formatINR(subtotal))
                            if (delivery > 0) SummaryLine(strings.invDelivery, Formatters.formatINR(delivery))
                            if (other > 0) SummaryLine(strings.invOtherCharges, Formatters.formatINR(other))
                            if (discount > 0) SummaryLine(strings.invDiscount, "-${Formatters.formatINR(discount)}")
                            if (taxAmount > 0) SummaryLine("${strings.taxLabel} (${taxPercent}%)", Formatters.formatINR(taxAmount))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BasilGreen.copy(alpha = 0.25f))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(strings.grandTotal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    text = Formatters.formatINR(grandTotal),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = BasilGreenDark
                                )
                            }
                        }
                    }

                    // Free Samples Transparency Callout
                    if (freeSamplePacks > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SaffronLight,
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.freeSampleNotice(
                                        freeSamplePacks,
                                        Formatters.formatINR(freeSampleRateWorth),
                                        Formatters.formatINR(freeSampleCost)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Grand Total & POS Action Bar
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lineItems.size} item(s) · ${lineItems.sumOf { it.quantity }} pack(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${strings.grandTotal}: ${Formatters.formatINR(grandTotal)}",
                                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = BasilGreenDark
                            )
                        }
                        Text(
                            text = "Cost ${Formatters.formatINR(totalVendorCost)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { attemptSave(true) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.invoiceJpgShareBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { attemptSave(false) },
                            modifier = Modifier.weight(1.2f).heightIn(min = 48.dp).testTag("save_order_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isEditing) strings.updateOrder else strings.saveOrder)
                        }
                    }
                }
            }
        }
    }

    // Dedicated Import WhatsApp Order Modal
    if (showWhatsAppModal) {
        AlertDialog(
            onDismissRequest = { showWhatsAppModal = false },
            title = { Text(strings.importWhatsAppOrder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = waRawText,
                        onValueChange = { waRawText = it },
                        placeholder = { Text(strings.whatsAppParserHint) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("whatsapp_parser_input")
                    )
                    Button(
                        onClick = { parsedPreview = onParseWhatsApp(waRawText) },
                        enabled = waRawText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("parse_whatsapp_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.extractDetails)
                    }

                    parsedPreview?.let { parsed ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BasilGreenLight,
                            border = BorderStroke(1.dp, BasilGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Extracted Preview", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BasilGreenDark)
                                Text("Name: ${parsed.customerName.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                                Text("Mobile: ${parsed.customerPhone.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                                Text("Address: ${parsed.customerAddress.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                if (parsed.items.isEmpty()) {
                                    Text("No catalog items matched", style = MaterialTheme.typography.bodySmall, color = Color(0xFF92400E))
                                } else {
                                    parsed.items.forEach { li ->
                                        Text(
                                            "• ${li.itemName} (${Formatters.formatGrams(li.packGrams)} × ${li.quantity}) — ${Formatters.formatINR(li.lineSaleTotal)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                                if (parsed.unmatchedLines.isNotEmpty()) {
                                    Text(
                                        "Unmatched: ${parsed.unmatchedLines.joinToString(" | ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val parsed = parsedPreview
                Button(
                    onClick = {
                        if (parsed != null) {
                            if (parsed.customerName.isNotBlank()) {
                                customerName = parsed.customerName
                                customerNameErr = null
                            }
                            if (parsed.customerPhone.isNotBlank()) customerPhone = parsed.customerPhone
                            if (parsed.customerAddress.isNotBlank()) customerAddress = parsed.customerAddress
                            if (parsed.items.isNotEmpty()) lineItems.addAll(parsed.items)
                            showWhatsAppModal = false
                            waRawText = ""
                            parsedPreview = null
                            onShowToast("WhatsApp details applied!")
                        }
                    },
                    enabled = parsedPreview != null
                ) { Text(strings.applyToOrder) }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppModal = false }) { Text(strings.close) }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = orderDateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { orderDateMs = it }
                        showDatePicker = false
                    }
                ) { Text(strings.saveChanges) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.close) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear current order form?",
            message = "All entered customer details and added items will be cleared.",
            confirmLabel = strings.clearOrderBtn,
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                resetDraft()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun StepNumberBadge(step: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(BasilGreen),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$step",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium
        )
    }
}
