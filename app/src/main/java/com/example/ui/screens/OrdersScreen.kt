package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.OrderFilter
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.export.ImageRenderer
import com.example.i18n.AppStrings
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.PaymentPill
import com.example.ui.theme.BasilGreen
import com.example.ui.theme.BasilGreenDark
import com.example.ui.theme.BasilGreenLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PaymentPaidBg
import com.example.ui.theme.PaymentPaidFg
import com.example.ui.theme.PaymentUnpaidBg
import com.example.ui.theme.PaymentUnpaidFg
import com.example.ui.theme.SaffronLight
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledFg
import com.example.ui.theme.StatusDeliveredBg
import com.example.ui.theme.StatusDeliveredFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg
import com.example.util.Formatters

private data class KpiCardSpec(
    val filter: OrderFilter,
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val bg: Color,
    val fg: Color,
    val tag: String
)

@Composable
fun OrdersScreen(
    strings: AppStrings,
    allOrders: List<OrderRecord>,
    filteredOrders: List<OrderRecord>,
    selectedFilter: OrderFilter,
    searchQuery: String,
    lastBackupAt: Long,
    stockTracking: Boolean,
    catalogItems: List<CatalogItem>,
    onSelectFilter: (OrderFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onBackupBannerClick: () -> Unit,
    onLowStockBannerClick: () -> Unit,
    onMarkOrderSent: (Long, Boolean) -> Unit,
    onUpdateStatus: (Long, OrderStatus) -> Unit,
    onUpdatePayment: (Long, PaymentStatus, Double) -> Unit,
    onOpenInvoiceJpg: (OrderRecord, Boolean) -> Unit,
    onEditOrder: (OrderRecord) -> Unit
) {
    val active = remember(allOrders) { allOrders.filter { it.status != OrderStatus.CANCELLED } }
    val inProcessList = remember(active) { active.filter { !it.isSent } }
    val sentList = remember(active) { active.filter { it.isSent } }
    val paidList = remember(active) { active.filter { it.paymentStatus == PaymentStatus.PAID } }
    val sentUnpaidList = remember(active) { active.filter { it.isSent && it.paymentStatus != PaymentStatus.PAID } }
    val cancelledList = remember(allOrders) { allOrders.filter { it.status == OrderStatus.CANCELLED } }

    val lowStockItems = remember(stockTracking, catalogItems) {
        if (!stockTracking) emptyList()
        else catalogItems.filter { it.stockGrams <= it.lowStockThresholdGrams }
    }

    var pendingCancelOrder by remember { mutableStateOf<OrderRecord?>(null) }

    val kpiCards = listOf(
        KpiCardSpec(
            filter = OrderFilter.ALL,
            title = strings.totalOrdersKpi,
            count = active.size,
            icon = Icons.Default.ReceiptLong,
            bg = MaterialTheme.colorScheme.surface,
            fg = BasilGreenDark,
            tag = "filter_card_all"
        ),
        KpiCardSpec(
            filter = OrderFilter.IN_PROCESS,
            title = strings.inProcessKpi,
            count = inProcessList.size,
            icon = Icons.Default.HourglassTop,
            bg = StatusPendingBg,
            fg = StatusPendingFg,
            tag = "filter_card_in_process"
        ),
        KpiCardSpec(
            filter = OrderFilter.DELIVERED,
            title = strings.orderSentKpi,
            count = sentList.size,
            icon = Icons.Default.LocalShipping,
            bg = StatusDeliveredBg,
            fg = StatusDeliveredFg,
            tag = "filter_card_sent"
        ),
        KpiCardSpec(
            filter = OrderFilter.PAID,
            title = strings.paymentReceivedKpi,
            count = paidList.size,
            icon = Icons.Default.CheckCircle,
            bg = PaymentPaidBg,
            fg = PaymentPaidFg,
            tag = "filter_card_paid"
        ),
        KpiCardSpec(
            filter = OrderFilter.SENT_UNPAID,
            title = strings.sentPaymentPendingKpi,
            count = sentUnpaidList.size,
            icon = Icons.Default.PendingActions,
            bg = PaymentUnpaidBg,
            fg = PaymentUnpaidFg,
            tag = "filter_card_sent_unpaid"
        ),
        KpiCardSpec(
            filter = OrderFilter.CANCELLED,
            title = strings.cancelledOrdersKpi,
            count = cancelledList.size,
            icon = Icons.Default.Cancel,
            bg = StatusCancelledBg,
            fg = StatusCancelledFg,
            tag = "filter_card_cancelled"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Subtle instruction caption
        item {
            Text(
                text = strings.ordersInstructionCaption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 6 Interactive KPI Filter Cards (3x2 on mobile, 6x1 on tablet/desktop)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        kpiCards.forEach { spec ->
                            val selected = selectedFilter == spec.filter
                            DashboardKpiCard(
                                spec = spec,
                                selected = selected,
                                modifier = Modifier.weight(1f).testTag(spec.tag),
                                onClick = {
                                    onSelectFilter(
                                        if (selected && spec.filter != OrderFilter.ALL) OrderFilter.ALL else spec.filter
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        kpiCards.chunked(3).forEach { rowSpecs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowSpecs.forEach { spec ->
                                    val selected = selectedFilter == spec.filter
                                    DashboardKpiCard(
                                        spec = spec,
                                        selected = selected,
                                        modifier = Modifier.weight(1f).testTag(spec.tag),
                                        onClick = {
                                            onSelectFilter(
                                                if (selected && spec.filter != OrderFilter.ALL) OrderFilter.ALL else spec.filter
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Last Backup Status Bar
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (lastBackupAt > 0L) BasilGreenLight else SaffronLight,
                border = BorderStroke(1.dp, if (lastBackupAt > 0L) BasilGreen.copy(alpha = 0.35f) else Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .testTag("last_backup_banner")
                    .clickable { onBackupBannerClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = if (lastBackupAt > 0L) BasilGreenDark else Color(0xFF92400E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lastBackupAt > 0L) {
                            strings.lastBackupAt(Formatters.formatDateTime(lastBackupAt))
                        } else {
                            strings.lastBackupNever
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (lastBackupAt > 0L) BasilGreenDark else Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Low-Stock Alert Banner (when stock tracking is enabled)
        if (lowStockItems.isNotEmpty()) {
            item {
                val names = lowStockItems.joinToString(", ") { it.name }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("low_stock_banner")
                        .clickable { onLowStockBannerClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.lowStockAlertBanner(names),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Search input with clear button
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChange("") },
                            modifier = Modifier.testTag("orders_search_clear")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text(strings.searchOrdersHint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("orders_search_input")
            )
        }

        if (filteredOrders.isEmpty()) {
            item {
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
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = BasilGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(strings.noOrdersYet, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.noOrdersTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    strings = strings,
                    onMarkOrderSent = { sent -> onMarkOrderSent(order.id, sent) },
                    onUpdateStatus = { st ->
                        if (st == OrderStatus.CANCELLED) {
                            pendingCancelOrder = order
                        } else {
                            onUpdateStatus(order.id, st)
                        }
                    },
                    onUpdatePayment = { ps, amt -> onUpdatePayment(order.id, ps, amt) },
                    onOpenInvoiceJpg = { isReceipt -> onOpenInvoiceJpg(order, isReceipt) },
                    onEdit = { onEditOrder(order) }
                )
            }
        }
    }

    pendingCancelOrder?.let { target ->
        ConfirmDialog(
            title = "${strings.cancelOrder}: ${target.invoiceNumber}?",
            message = "Cancelled orders are excluded from sales, profit, and vendor payables until restored.",
            confirmLabel = strings.cancelOrder,
            dismissLabel = strings.close,
            isDestructive = true,
            onConfirm = {
                onUpdateStatus(target.id, OrderStatus.CANCELLED)
                pendingCancelOrder = null
            },
            onDismiss = { pendingCancelOrder = null }
        )
    }
}

@Composable
private fun DashboardKpiCard(
    spec: KpiCardSpec,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = spec.bg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) spec.fg else spec.fg.copy(alpha = 0.22f)
        ),
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 74.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = spec.fg,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = spec.fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${spec.count}",
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = spec.fg
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(
    order: OrderRecord,
    strings: AppStrings,
    onMarkOrderSent: (Boolean) -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit,
    onUpdatePayment: (PaymentStatus, Double) -> Unit,
    onOpenInvoiceJpg: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var partialAmountText by remember(order.paidAmount) {
        mutableStateOf(if (order.paidAmount > 0) Formatters.round2(order.paidAmount).toString() else "")
    }
    var partialAmountErr by remember { mutableStateOf<String?>(null) }

    val isCancelled = order.status == OrderStatus.CANCELLED
    val isPaid = order.paymentStatus == PaymentStatus.PAID

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.invoiceNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Monospace #OrderID pill + Customer Name + Soft Status Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BasilGreenLight,
                    border = BorderStroke(1.dp, BasilGreen.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "#${order.invoiceNumber}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BasilGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = order.customerName.ifBlank { "Walk-in Customer" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isCancelled) {
                    SoftPill(
                        label = strings.cancelled.uppercase(),
                        bg = StatusCancelledBg,
                        fg = StatusCancelledFg
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SoftPill(
                            label = if (order.isSent) strings.statusSentPill else strings.statusNotSentPill,
                            bg = if (order.isSent) StatusDeliveredBg else StatusPendingBg,
                            fg = if (order.isSent) StatusDeliveredFg else StatusPendingFg
                        )
                        PaymentPill(order.paymentStatus, strings.paymentName(order.paymentStatus))
                    }
                }
            }

            // Middle metadata: <DD MMM YYYY> · <item count> item(s) (<total packs> packs) on left, bold Grand Total on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${Formatters.formatDate(order.createdAt)} · ${order.items.size} item(s) (${order.totalPacks} packs)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.customerPhone.isNotBlank()) {
                        Text(
                            text = order.customerPhone,
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = Formatters.formatINR(order.grandTotal),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else BasilGreenDark
                )
            }

            // Clean Action Bar: Details/Status | Edit | Invoice/Receipt JPG
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_details_${order.invoiceNumber}"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (expanded) strings.hideBtn else strings.detailsStatusBtn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_edit_${order.invoiceNumber}"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.edit, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { onOpenInvoiceJpg(isPaid) },
                    modifier = Modifier
                        .weight(1.15f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_invoice_jpg_${order.invoiceNumber}"),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPaid) strings.receiptJpgShareBtn else strings.invoiceJpgShareBtn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Expandable Order Details & Status Panel
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "#${order.invoiceNumber} · Order Details · ${order.customerName.ifBlank { "Walk-in Customer" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BasilGreenDark
                    )

                    // Order Metadata Grid
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailMetaRow(strings.orderDateLabel, Formatters.formatDate(order.createdAt))
                        DetailMetaRow(strings.customerPhone, order.customerPhone.ifBlank { "—" })
                        DetailMetaRow(strings.customerAddress, order.customerAddress.ifBlank { "—" })
                        if (order.notes.isNotBlank()) {
                            DetailMetaRow(strings.orderNotes, order.notes)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Structured Ledger Table: Item | Pack | Qty | Amount
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(strings.invColItem, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                        Text(strings.invColPack, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        Text(strings.invColQty, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text(strings.invColAmount, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    order.items.forEach { li ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = li.itemName,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(2.2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = Formatters.formatGrams(li.packGrams),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = "${li.quantity}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = if (li.isFreeSample) "Sample · ₹0.00" else Formatters.formatINR(li.lineSaleTotal),
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = if (li.isFreeSample) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    if (order.deliveryCharge > 0) DetailMetaRow(strings.invDelivery, Formatters.formatINR(order.deliveryCharge))
                    if (order.otherCharge > 0) DetailMetaRow(strings.invOtherCharges, Formatters.formatINR(order.otherCharge))
                    if (order.discount > 0) DetailMetaRow(strings.invDiscount, "-${Formatters.formatINR(order.discount)}")
                    if (order.taxAmount > 0) DetailMetaRow("${strings.invTax} (${order.taxPercent}%)", Formatters.formatINR(order.taxAmount))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.invGrandTotal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            text = Formatters.formatINR(order.grandTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = BasilGreenDark
                        )
                    }

                    if (!isCancelled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Quick Sent & Payment Toggles
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onMarkOrderSent(!order.isSent) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (order.isSent) Icons.Default.Undo else Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (order.isSent) strings.undoOrderSent else strings.markOrderSent)
                            }

                            Button(
                                onClick = {
                                    if (isPaid) {
                                        onUpdatePayment(PaymentStatus.UNPAID, 0.0)
                                    } else {
                                        onUpdatePayment(PaymentStatus.PAID, order.grandTotal)
                                    }
                                },
                                colors = if (isPaid) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                } else {
                                    ButtonDefaults.buttonColors(containerColor = BasilGreen)
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaid) Icons.Default.Undo else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPaid) strings.undoPaymentReceived else strings.markPaymentReceived)
                            }
                        }

                        // Partial Payment Entry
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = partialAmountText,
                                onValueChange = { partialAmountText = it; partialAmountErr = null },
                                label = { Text(strings.partialPaidAmount) },
                                isError = partialAmountErr != null,
                                supportingText = partialAmountErr?.let { err -> { Text(err) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val amt = partialAmountText.toDoubleOrNull()
                                    if (amt == null || amt < 0.0) {
                                        partialAmountErr = "Enter valid amount"
                                    } else {
                                        val rounded = Formatters.round2(amt)
                                        val status = when {
                                            rounded >= order.grandTotal && order.grandTotal > 0 -> PaymentStatus.PAID
                                            rounded > 0 -> PaymentStatus.PARTIAL
                                            else -> PaymentStatus.UNPAID
                                        }
                                        onUpdatePayment(status, rounded)
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text(strings.saveChanges) }
                        }
                    }

                    // Bottom Panel Actions
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenInvoiceJpg(isPaid) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isPaid) strings.generateReceiptJpg else strings.generateInvoiceJpg)
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.edit)
                        }

                        if (isCancelled) {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.PENDING) },
                                colors = ButtonDefaults.buttonColors(containerColor = BasilGreen),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.restoreOrder)
                            }
                        } else {
                            TextButton(
                                onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.cancelOrder)
                            }
                        }

                        TextButton(
                            onClick = { expanded = false },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(strings.close)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftPill(label: String, bg: Color, fg: Color) {
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
private fun DetailMetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}
