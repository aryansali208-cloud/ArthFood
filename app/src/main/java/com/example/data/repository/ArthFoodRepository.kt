package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.data.local.ArthFoodDatabase
import com.example.util.Formatters
import com.example.data.local.CatalogItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderWithItemsEntity
import com.example.data.local.PackSizeEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.VendorEntity
import com.example.data.local.VendorPaymentEntity
import com.example.data.model.AppLanguage
import com.example.data.model.BackupInspectionSummary
import com.example.data.model.BusinessProfile
import com.example.data.model.CatalogItem
import com.example.data.model.CustomerSummary
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PackSize
import com.example.data.model.PackSummaryEntry
import com.example.data.model.ParsedWhatsAppOrder
import com.example.data.model.PaymentStatus
import com.example.data.model.ProfitOrderScope
import com.example.data.model.ProfitSummary
import com.example.data.model.Vendor
import com.example.data.model.VendorPayableSummary
import com.example.data.model.VendorPayment
import com.example.export.BackupJsonCodec
import com.example.export.BackupPayload
import com.example.export.ExcelXlsxExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ArthFoodRepository(
    private val context: Context,
    private val db: ArthFoodDatabase,
    val prefs: PreferencesStore
) {
    private val packDao = db.packSizeDao()
    private val vendorDao = db.vendorDao()
    private val catalogDao = db.catalogDao()
    private val orderDao = db.orderDao()
    private val vendorPaymentDao = db.vendorPaymentDao()

    val packSizes: Flow<List<Int>> = packDao.observeAll().map { list -> list.map { it.grams } }
    val packSizeDetails: Flow<List<PackSize>> = packDao.observeAll().map { list ->
        list.map { PackSize(it.grams, it.customName, it.sortOrder) }
    }
    val vendors: Flow<List<Vendor>> = vendorDao.observeAll().map { list ->
        list.map { Vendor(it.id, it.name, it.phone, it.notes) }
    }
    val catalogItems: Flow<List<CatalogItem>> = catalogDao.observeActive().map { list ->
        list.map {
            CatalogItem(
                id = it.id,
                name = it.name,
                sellingPricePerKg = it.sellingPricePerKg,
                vendorCostPerKg = it.vendorCostPerKg,
                vendorId = it.vendorId,
                vendorName = it.vendorName,
                stockGrams = it.stockGrams,
                lowStockThresholdGrams = it.lowStockThresholdGrams,
                trackStock = it.trackStock,
                packPriceOverridesJson = it.packPriceOverridesJson,
                isActive = it.isActive
            )
        }
    }
    val orders: Flow<List<OrderRecord>> = orderDao.observeAllWithItems().map { list ->
        list.map { it.toDomain() }
    }
    val vendorPayments: Flow<List<VendorPayment>> = vendorPaymentDao.observeAll().map { list ->
        list.map { VendorPayment(it.id, it.vendorId, it.vendorName, it.amount, it.paymentDate, it.mode, it.note) }
    }

    suspend fun addPackSize(grams: Int, customName: String = "") {
        if (grams > 0) packDao.insert(PackSizeEntity(grams, customName.trim(), grams))
    }

    suspend fun deletePackSize(grams: Int) = packDao.deleteByGrams(grams)

    suspend fun saveVendor(vendor: Vendor): Long {
        val entity = VendorEntity(vendor.id, vendor.name.trim(), vendor.phone.trim(), vendor.notes.trim())
        return if (vendor.id == 0L) vendorDao.insert(entity) else {
            vendorDao.update(entity)
            vendor.id
        }
    }

    suspend fun deleteVendor(id: Long) = vendorDao.deleteById(id)

    suspend fun saveCatalogItem(item: CatalogItem): Long {
        var resolvedVendorId = item.vendorId
        val cleanVendorName = item.vendorName.trim()
        if (resolvedVendorId == null && cleanVendorName.isNotBlank()) {
            val existing = vendorDao.getAllOnce().firstOrNull { it.name.equals(cleanVendorName, ignoreCase = true) }
            resolvedVendorId = existing?.id ?: vendorDao.insert(VendorEntity(name = cleanVendorName))
        }
        val entity = CatalogItemEntity(
            id = item.id,
            name = item.name.trim(),
            sellingPricePerKg = item.sellingPricePerKg,
            vendorCostPerKg = item.vendorCostPerKg,
            vendorId = resolvedVendorId,
            vendorName = cleanVendorName,
            stockGrams = item.stockGrams,
            lowStockThresholdGrams = item.lowStockThresholdGrams,
            trackStock = item.trackStock,
            packPriceOverridesJson = item.packPriceOverridesJson,
            isActive = item.isActive
        )
        return if (item.id == 0L) catalogDao.insert(entity) else {
            catalogDao.update(entity)
            item.id
        }
    }

    suspend fun deleteCatalogItem(id: Long) = catalogDao.deleteById(id)

    suspend fun addStockToItem(itemId: Long, deltaGrams: Int) {
        if (deltaGrams != 0) {
            db.withTransaction {
                catalogDao.adjustStock(itemId, deltaGrams)
            }
        }
    }

    suspend fun assignVendorToCatalogItem(itemId: Long, vendor: Vendor?) {
        db.withTransaction {
            val existing = catalogDao.getById(itemId) ?: return@withTransaction
            catalogDao.update(
                existing.copy(
                    vendorId = vendor?.id,
                    vendorName = vendor?.name ?: ""
                )
            )
        }
    }

    suspend fun saveOrder(order: OrderRecord): Long {
        val stockEnabled = prefs.stockTracking.first()
        val now = System.currentTimeMillis()
        val isNew = order.id == 0L

        val invoiceNo = order.invoiceNumber.ifBlank {
            val seq = prefs.nextInvoiceSequence()
            val year = Calendar.getInstance().get(Calendar.YEAR)
            "AF-$year-${seq.toString().padStart(4, '0')}"
        }

        val subtotal = Formatters.round2(order.items.sumOf { it.lineSaleTotal })
        val taxBase = Formatters.round2((subtotal + order.deliveryCharge + order.otherCharge - order.discount).coerceAtLeast(0.0))
        val taxAmount = Formatters.round2((taxBase * order.taxPercent) / 100.0)
        val grandTotal = Formatters.round2((taxBase + taxAmount).coerceAtLeast(0.0))
        val totalVendorCost = Formatters.round2(order.items.sumOf { it.lineCostTotal })

        val orderId = db.withTransaction {
            if (!isNew) {
                val previous = orderDao.getByIdWithItems(order.id)
                if (previous != null && previous.order.status != OrderStatus.CANCELLED.key) {
                    previous.items.forEach { prevItem ->
                        prevItem.catalogItemId?.let { cid ->
                            val trackItem = stockEnabled || (catalogDao.getById(cid)?.trackStock == true)
                            if (trackItem) catalogDao.adjustStock(cid, prevItem.packGrams * prevItem.quantity)
                        }
                    }
                }
            }

            val resolvedSentAt = if (order.status == OrderStatus.DELIVERED) {
                order.sentAt.takeIf { it > 0L } ?: now
            } else {
                order.sentAt
            }
            val resolvedPaidAt = when (order.paymentStatus) {
                PaymentStatus.PAID, PaymentStatus.PARTIAL -> order.paidAt.takeIf { it > 0L } ?: now
                PaymentStatus.UNPAID -> 0L
            }

            val entity = OrderEntity(
                id = order.id,
                invoiceNumber = invoiceNo,
                customerName = order.customerName.trim(),
                customerPhone = order.customerPhone.trim(),
                customerAddress = order.customerAddress.trim(),
                notes = order.notes.trim(),
                status = order.status.key,
                paymentStatus = order.paymentStatus.key,
                paidAmount = when (order.paymentStatus) {
                    PaymentStatus.PAID -> grandTotal
                    PaymentStatus.UNPAID -> 0.0
                    PaymentStatus.PARTIAL -> Formatters.round2(order.paidAmount.coerceIn(0.0, grandTotal))
                },
                deliveryCharge = Formatters.round2(order.deliveryCharge),
                otherCharge = Formatters.round2(order.otherCharge),
                discount = Formatters.round2(order.discount),
                taxPercent = order.taxPercent,
                subtotal = subtotal,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                totalVendorCost = totalVendorCost,
                sentAt = resolvedSentAt,
                paidAt = resolvedPaidAt,
                createdAt = if (order.createdAt > 0L) order.createdAt else now,
                updatedAt = now
            )

            val savedId = if (isNew) {
                orderDao.insertOrder(entity)
            } else {
                orderDao.updateOrder(entity)
                orderDao.deleteItemsForOrder(order.id)
                order.id
            }

            val itemEntities = order.items.map { li ->
                OrderItemEntity(
                    id = 0L,
                    orderId = savedId,
                    catalogItemId = li.catalogItemId,
                    itemName = li.itemName,
                    packGrams = li.packGrams,
                    quantity = li.quantity,
                    unitPrice = Formatters.round2(li.unitPrice),
                    sellingPricePerKg = Formatters.round2(li.sellingPricePerKg),
                    vendorCostPerKg = Formatters.round2(li.vendorCostPerKg),
                    vendorId = li.vendorId,
                    vendorName = li.vendorName,
                    isFreeSample = li.isFreeSample,
                    isVendorLinePaid = li.isVendorLinePaid,
                    lineSaleTotal = li.lineSaleTotal,
                    lineCostTotal = li.lineCostTotal
                )
            }
            orderDao.insertItems(itemEntities)

            if (order.status != OrderStatus.CANCELLED) {
                order.items.forEach { li ->
                    li.catalogItemId?.let { cid ->
                        val trackItem = stockEnabled || (catalogDao.getById(cid)?.trackStock == true)
                        if (trackItem) catalogDao.adjustStock(cid, -(li.packGrams * li.quantity))
                    }
                }
            }
            savedId
        }

        tryAutoBackupToConfiguredFolder()
        return orderId
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        val stockEnabled = prefs.stockTracking.first()
        db.withTransaction {
            val prev = orderDao.getByIdWithItems(orderId) ?: return@withTransaction
            val oldStatus = OrderStatus.fromKey(prev.order.status)

            if (oldStatus != OrderStatus.CANCELLED && newStatus == OrderStatus.CANCELLED) {
                prev.items.forEach { item ->
                    item.catalogItemId?.let { cid ->
                        if (stockEnabled || (catalogDao.getById(cid)?.trackStock == true)) {
                            catalogDao.adjustStock(cid, item.packGrams * item.quantity)
                        }
                    }
                }
            } else if (oldStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
                prev.items.forEach { item ->
                    item.catalogItemId?.let { cid ->
                        if (stockEnabled || (catalogDao.getById(cid)?.trackStock == true)) {
                            catalogDao.adjustStock(cid, -(item.packGrams * item.quantity))
                        }
                    }
                }
            }
            val now = System.currentTimeMillis()
            val newSentAt = when (newStatus) {
                OrderStatus.DELIVERED -> prev.order.sentAt.takeIf { it > 0L } ?: now
                OrderStatus.CANCELLED -> prev.order.sentAt
                else -> 0L
            }
            orderDao.updateStatus(orderId, newStatus.key, newSentAt, now)
        }
        tryAutoBackupToConfiguredFolder()
    }

    suspend fun updateOrderPayment(orderId: Long, paymentStatus: PaymentStatus, paidAmount: Double) {
        val now = System.currentTimeMillis()
        val paidAt = when (paymentStatus) {
            PaymentStatus.PAID, PaymentStatus.PARTIAL -> now
            PaymentStatus.UNPAID -> 0L
        }
        orderDao.updatePayment(orderId, paymentStatus.key, Formatters.round2(paidAmount), paidAt, now)
        tryAutoBackupToConfiguredFolder()
    }

    suspend fun markVendorLinesPaid(vendor: Vendor, startMs: Long, endMs: Long) {
        db.withTransaction {
            orderDao.markVendorLinesPaid(vendor.id, vendor.name, startMs, endMs)
        }
        tryAutoBackupToConfiguredFolder()
    }

    suspend fun recordVendorPayment(payment: VendorPayment) {
        vendorPaymentDao.insert(
            VendorPaymentEntity(
                id = 0L,
                vendorId = payment.vendorId,
                vendorName = payment.vendorName,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                mode = payment.mode,
                note = payment.note
            )
        )
    }

    suspend fun deleteVendorPayment(id: Long) = vendorPaymentDao.deleteById(id)

    fun parseWhatsAppOrder(
        rawText: String,
        catalog: List<CatalogItem>,
        packSizes: List<Int>
    ): ParsedWhatsAppOrder {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        var detectedPhone = ""
        var detectedName = ""
        val addressLines = mutableListOf<String>()
        val phoneRegex = Regex("(?:\\+?91[\\s-]*)?(0?\\d[\\d\\s-]{8,14}\\d)")
        val weightRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(kg|kilo|g|gm|gms|gram|grams)\\b", RegexOption.IGNORE_CASE)
        val qtyPrefixRegex = Regex("^(\\d+)\\s*[xX*×]\\s*")
        val qtyTrailingRegex = Regex("(?:[xX*×]\\s*(\\d+)|\\b(\\d+)\\s*(?:pcs|pc|pack|packs|pkt|pkts|nos|no))$", RegexOption.IGNORE_CASE)
        val namePrefixRegex = Regex("^(?:name|customer|cust)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val phonePrefixRegex = Regex("^(?:phone|mobile|mob|ph|wa|whatsapp|contact)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val addrPrefixRegex = Regex("^(?:address|addr|deliver\\s*to|delivery\\s*address|location|pin|pincode)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val addressHintRegex = Regex("\\b(?:flat|house|plot|bldg|building|wing|floor|road|rd|street|st|lane|sector|nagar|colony|society|apt|apartment|near|opp|opposite|behind|chowk|bazar|bazaar|marg|vihar|enclave|pin|pincode|\\d{6})\\b", RegexOption.IGNORE_CASE)

        val matchedItems = mutableListOf<OrderLineItem>()
        val unmatched = mutableListOf<String>()
        var readingAddressBlock = false

        for (rawLine in lines) {
            // Strip bullet / dash prefixes cleanly for parsing
            val line = rawLine.trimStart('-', '*', '•', ' ')

            val namePref = namePrefixRegex.find(line)
            if (namePref != null) {
                readingAddressBlock = false
                val candidate = namePref.groupValues[1].trim()
                if (candidate.isNotBlank()) detectedName = candidate
                continue
            }

            val addrPref = addrPrefixRegex.find(line)
            if (addrPref != null) {
                readingAddressBlock = true
                val candidate = addrPref.groupValues[1].trim()
                if (candidate.isNotBlank()) addressLines.add(candidate)
                continue
            }

            val phonePref = phonePrefixRegex.find(line)
            val lineForPhone = phonePref?.groupValues?.getOrNull(1) ?: line

            val phoneMatch = phoneRegex.find(lineForPhone)
            if (phoneMatch != null && detectedPhone.isBlank()) {
                val digits = phoneMatch.value.filter { it.isDigit() }
                if (digits.length >= 10) {
                    val normalized10 = digits.takeLast(10)
                    detectedPhone = normalized10
                    val remainder = lineForPhone.replace(phoneMatch.value, "").trim(' ', ',', '-', ':')
                    if (remainder.isNotBlank() && detectedName.isBlank() && catalog.none { remainder.contains(it.name, ignoreCase = true) }) {
                        detectedName = remainder
                    }
                    if (phonePref != null || remainder.isBlank() || detectedName == remainder) {
                        readingAddressBlock = false
                        continue
                    }
                }
            }

            // Try matching catalog item
            val matchedCatalog = catalog.sortedByDescending { it.name.length }.firstOrNull { item ->
                line.contains(item.name, ignoreCase = true) ||
                    item.name.split(" ").filter { it.length >= 3 }.any { word -> line.contains(word, ignoreCase = true) }
            }

            if (matchedCatalog != null) {
                readingAddressBlock = false
                val wMatch = weightRegex.find(line)
                val packGrams = if (wMatch != null) {
                    val num = wMatch.groupValues[1].toDoubleOrNull() ?: 250.0
                    val unit = wMatch.groupValues[2].lowercase()
                    val g = if (unit.startsWith("k")) (num * 1000).roundToInt() else num.roundToInt()
                    g.coerceAtLeast(10)
                } else {
                    packSizes.firstOrNull { it == 250 } ?: packSizes.firstOrNull() ?: 250
                }

                val qPre = qtyPrefixRegex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                val qPostMatch = qtyTrailingRegex.find(line)
                val qPost = qPostMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: qPostMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
                val qty = (qPre ?: qPost ?: 1).coerceIn(1, 999)
                val unitPrice = matchedCatalog.priceForPack(packGrams)

                matchedItems.add(
                    OrderLineItem(
                        catalogItemId = matchedCatalog.id,
                        itemName = matchedCatalog.name,
                        packGrams = packGrams,
                        quantity = qty,
                        unitPrice = unitPrice,
                        sellingPricePerKg = matchedCatalog.sellingPricePerKg,
                        vendorCostPerKg = matchedCatalog.vendorCostPerKg,
                        vendorId = matchedCatalog.vendorId,
                        vendorName = matchedCatalog.vendorName,
                        isFreeSample = false
                    )
                )
            } else {
                if (detectedName.isBlank() && !line.any { it.isDigit() } && line.length <= 40 && !addressHintRegex.containsMatchIn(line)) {
                    detectedName = line
                    readingAddressBlock = false
                } else if (readingAddressBlock || addressHintRegex.containsMatchIn(line) || (detectedName.isNotBlank() && line.contains(","))) {
                    addressLines.add(line)
                } else {
                    unmatched.add(rawLine)
                }
            }
        }

        return ParsedWhatsAppOrder(
            customerName = detectedName,
            customerPhone = detectedPhone,
            customerAddress = addressLines.joinToString(", "),
            items = matchedItems,
            unmatchedLines = unmatched
        )
    }

    fun computeCustomerSummaries(orders: List<OrderRecord>): List<CustomerSummary> {
        return orders
            .filter { it.status != OrderStatus.CANCELLED }
            .groupBy { o ->
                val p = o.customerPhone.filter { it.isDigit() }.takeLast(10)
                if (p.length >= 7) "P:$p" else "N:${o.customerName.trim().lowercase()}"
            }
            .map { (key, list) ->
                val latest = list.maxByOrNull { it.createdAt }!!
                CustomerSummary(
                    key = key,
                    name = list.firstOrNull { it.customerName.isNotBlank() }?.customerName ?: "Walk-in",
                    phone = list.firstOrNull { it.customerPhone.isNotBlank() }?.customerPhone ?: "",
                    orderCount = list.size,
                    totalSpent = list.sumOf { it.grandTotal },
                    totalPaid = list.sumOf { it.effectivePaidAmount },
                    pendingDue = list.sumOf { it.balanceDue },
                    lastOrderAt = latest.createdAt
                )
            }
            .sortedByDescending { it.pendingDue }
    }

    fun computeProfitSummary(
        orders: List<OrderRecord>,
        startMs: Long,
        endMs: Long,
        scope: ProfitOrderScope = ProfitOrderScope.ALL_ACTIVE
    ): ProfitSummary {
        val active = orders.filter { o ->
            o.status != OrderStatus.CANCELLED &&
                o.createdAt in startMs..endMs &&
                when (scope) {
                    ProfitOrderScope.ALL_ACTIVE -> true
                    ProfitOrderScope.SENT_ONLY -> o.isSent
                    ProfitOrderScope.PAID_ONLY -> o.paymentStatus == PaymentStatus.PAID
                }
        }
        val totalSales = Formatters.round2(active.sumOf { it.grandTotal })
        val totalCost = Formatters.round2(active.sumOf { it.totalVendorCost })
        val charges = Formatters.round2(active.sumOf { it.deliveryCharge + it.otherCharge })
        val freeCount = active.sumOf { o -> o.items.count { it.isFreeSample } }
        val netProfit = Formatters.round2(totalSales - totalCost)
        val margin = if (totalSales > 0.0) Formatters.round2((netProfit / totalSales) * 100.0) else 0.0
        val missingVendorOrCostLines = active.sumOf { o ->
            o.items.count { (it.vendorId == null && it.vendorName.isBlank()) || it.vendorCostPerKg <= 0.0 }
        }
        return ProfitSummary(
            orderCount = active.size,
            freeSampleCount = freeCount,
            totalSales = totalSales,
            totalVendorCost = totalCost,
            deliveryAndOtherCharges = charges,
            netProfit = netProfit,
            marginPercent = margin,
            missingVendorOrCostLines = missingVendorOrCostLines
        )
    }

    fun computeVendorPayables(
        vendors: List<Vendor>,
        catalogItems: List<CatalogItem>,
        orders: List<OrderRecord>,
        payments: List<VendorPayment>,
        startMs: Long,
        endMs: Long
    ): List<VendorPayableSummary> {
        val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED && it.createdAt in startMs..endMs }
        val allLineItems = activeOrders.flatMap { it.items }

        return vendors.map { v ->
            val vItems = allLineItems.filter {
                it.vendorId == v.id || (it.vendorId == null && it.vendorName.equals(v.name, ignoreCase = true))
            }
            val breakdown = vItems
                .groupBy { it.itemName to it.packGrams }
                .map { (key, list) ->
                    PackSummaryEntry(
                        itemName = key.first,
                        packGrams = key.second,
                        quantity = list.sumOf { it.quantity },
                        totalGrams = list.sumOf { it.totalGrams },
                        costTotal = Formatters.round2(list.sumOf { it.lineCostTotal }),
                        vendorCostPerKg = list.firstOrNull()?.vendorCostPerKg ?: 0.0,
                        allPaid = list.isNotEmpty() && list.all { it.isVendorLinePaid }
                    )
                }
                .sortedWith(compareBy({ it.itemName }, { it.packGrams }))

            val totalPayable = Formatters.round2(vItems.sumOf { it.lineCostTotal })
            val paidViaLines = Formatters.round2(vItems.filter { it.isVendorLinePaid }.sumOf { it.lineCostTotal })
            val installmentPaid = Formatters.round2(
                payments
                    .filter { (it.vendorId == v.id || it.vendorName.equals(v.name, ignoreCase = true)) && it.paymentDate in startMs..endMs }
                    .sumOf { it.amount }
            )
            val effectivePaid = Formatters.round2(maxOf(paidViaLines, installmentPaid))

            val missingCost = catalogItems
                .filter { (it.vendorId == v.id || it.vendorName.equals(v.name, ignoreCase = true)) && it.vendorCostPerKg <= 0.0 }
                .map { it.name }

            VendorPayableSummary(
                vendor = v,
                totalGrams = vItems.sumOf { it.totalGrams },
                totalPayable = totalPayable,
                totalPaid = effectivePaid,
                balanceDue = Formatters.round2((totalPayable - effectivePaid).coerceAtLeast(0.0)),
                packBreakdown = breakdown,
                missingCostItems = missingCost,
                hasUnpaidLines = vItems.any { !it.isVendorLinePaid }
            )
        }
    }

    suspend fun inspectBackupJson(jsonText: String): Result<BackupInspectionSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupJsonCodec.decode(jsonText)
            val existingOrderInvs = orderDao.getAllWithItemsOnce().map { it.order.invoiceNumber }.toSet()
            val existingCatalogNames = catalogDao.getAllOnce().map { it.name.lowercase() }.toSet()
            val existingVendorNames = vendorDao.getAllOnce().map { it.name.lowercase() }.toSet()

            val newOrders = payload.orders.count { ow ->
                val inv = ow.order.invoiceNumber
                inv.isBlank() || inv !in existingOrderInvs
            }
            val newItems = payload.catalogItems.count { it.name.isNotBlank() && it.name.lowercase() !in existingCatalogNames }
            val newVendors = payload.vendors.count { it.name.isNotBlank() && it.name.lowercase() !in existingVendorNames }

            BackupInspectionSummary(
                jsonText = jsonText,
                totalOrdersInBackup = payload.orders.size,
                newOrdersToImport = newOrders,
                itemsInBackup = payload.catalogItems.size,
                newItemsToImport = newItems,
                vendorsInBackup = payload.vendors.size,
                newVendorsToImport = newVendors,
                packSizesInBackup = payload.packSizes.size
            )
        }
    }

    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            appLanguage = prefs.appLanguage.first(),
            invoiceLanguage = prefs.invoiceLanguage.first(),
            businessProfile = prefs.businessProfile.first(),
            lockedRates = prefs.lockedRates.first(),
            stockTracking = prefs.stockTracking.first(),
            packSizes = packDao.getAllOnce(),
            vendors = vendorDao.getAllOnce(),
            catalogItems = catalogDao.getAllOnce(),
            orders = orderDao.getAllWithItemsOnce(),
            vendorPayments = vendorPaymentDao.getAllOnce()
        )
        BackupJsonCodec.encode(payload)
    }

    /**
     * Merges backup records without erasing existing orders, items, or vendors.
     */
    suspend fun restoreAndMergeFromJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupJsonCodec.decode(jsonText)

            // Only update business profile if current phone/name is empty or backup has one
            val currentProfile = prefs.businessProfile.first()
            if (currentProfile.phone.isBlank() && payload.businessProfile.phone.isNotBlank()) {
                prefs.saveBusinessProfile(payload.businessProfile.copy(logoPath = currentProfile.logoPath))
            }
            prefs.setAppLanguage(payload.appLanguage)
            prefs.setInvoiceLanguage(payload.invoiceLanguage)

            // Merge inside atomic Room transaction
            val importedOrdersCount = db.withTransaction {
                packDao.insertAll(payload.packSizes)

                val vendorNameToId = vendorDao.getAllOnce().associate { it.name.lowercase() to it.id }.toMutableMap()
                payload.vendors.forEach { v ->
                    if (v.name.isNotBlank() && !vendorNameToId.containsKey(v.name.lowercase())) {
                        val id = vendorDao.insert(VendorEntity(name = v.name, phone = v.phone, notes = v.notes))
                        vendorNameToId[v.name.lowercase()] = id
                    }
                }

                val existingCatalog = catalogDao.getAllOnce().associateBy { it.name.lowercase() }
                payload.catalogItems.forEach { c ->
                    if (c.name.isNotBlank() && !existingCatalog.containsKey(c.name.lowercase())) {
                        val mappedVendorId = vendorNameToId[c.vendorName.lowercase()] ?: c.vendorId
                        catalogDao.insert(c.copy(id = 0L, vendorId = mappedVendorId))
                    }
                }

                var imported = 0
                val updatedCatalogMap = catalogDao.getAllOnce().associateBy { it.name.lowercase() }
                var maxSeq = 0
                payload.orders.forEach { ow ->
                    val inv = ow.order.invoiceNumber
                    val seqPart = inv.substringAfterLast("-").toIntOrNull() ?: 0
                    if (seqPart > maxSeq) maxSeq = seqPart

                    val existing = if (inv.isNotBlank()) orderDao.getByInvoiceNumber(inv) else null
                    if (existing == null) {
                        val newOrderId = orderDao.insertOrder(ow.order.copy(id = 0L))
                        val fixedItems = ow.items.map { li ->
                            li.copy(
                                id = 0L,
                                orderId = newOrderId,
                                catalogItemId = updatedCatalogMap[li.itemName.lowercase()]?.id ?: li.catalogItemId,
                                vendorId = vendorNameToId[li.vendorName.lowercase()] ?: li.vendorId
                            )
                        }
                        orderDao.insertItems(fixedItems)
                        imported++
                    }
                }
                if (maxSeq > 0) prefs.syncMinInvoiceSequence(maxSeq)

                val existingPayments = vendorPaymentDao.getAllOnce()
                payload.vendorPayments.forEach { vp ->
                    val dup = existingPayments.any {
                        it.paymentDate == vp.paymentDate && it.amount == vp.amount && it.vendorName.equals(vp.vendorName, ignoreCase = true)
                    }
                    if (!dup) {
                        val vId = vendorNameToId[vp.vendorName.lowercase()] ?: vp.vendorId
                        vendorPaymentDao.insert(vp.copy(id = 0L, vendorId = vId))
                    }
                }
                imported
            }

            prefs.setOnboardingCompleted(true)
            importedOrdersCount
        }
    }

    suspend fun exportBackupToCacheFile(): File = withContext(Dispatchers.IO) {
        val json = buildBackupJson()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val file = File(dir, "ArthFood_Backup_$dateStamp.json")
        FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        prefs.setLastBackupAt(System.currentTimeMillis())
        file
    }

    suspend fun exportExcelToCacheFile(
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        dateLabel: String = "All Time"
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val file = File(dir, "ArthFood_Report_$dateStamp.xlsx")
        val allOrders = orderDao.getAllWithItemsOnce().map { it.toDomain() }
        val filteredOrders = allOrders.filter { it.createdAt in startMs..endMs }
        val profitSummary = computeProfitSummary(allOrders, startMs, endMs, ProfitOrderScope.ALL_ACTIVE)
        val vendorsList = vendorDao.getAllOnce().map { Vendor(it.id, it.name, it.phone, it.notes) }
        val catalogList = catalogDao.getAllOnce().map {
            CatalogItem(
                it.id, it.name, it.sellingPricePerKg, it.vendorCostPerKg,
                it.vendorId, it.vendorName, it.stockGrams, it.lowStockThresholdGrams,
                it.trackStock, it.packPriceOverridesJson, it.isActive
            )
        }
        val paymentsList = vendorPaymentDao.getAllOnce().map {
            VendorPayment(it.id, it.vendorId, it.vendorName, it.amount, it.paymentDate, it.mode, it.note)
        }
        val vendorPayables = computeVendorPayables(vendorsList, catalogList, allOrders, paymentsList, startMs, endMs)

        FileOutputStream(file).use { out ->
            ExcelXlsxExporter.writeWorkbook(
                output = out,
                orders = filteredOrders,
                profitSummary = profitSummary,
                vendorPayables = vendorPayables,
                dateRangeLabel = dateLabel
            )
        }
        file
    }

    suspend fun tryAutoBackupToConfiguredFolder(): Boolean = withContext(Dispatchers.IO) {
        val uriStr = prefs.autoBackupFolderUri.first()
        if (uriStr.isBlank()) return@withContext false
        runCatching {
            val treeUri = Uri.parse(uriStr)
            val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
            val existing = dir.findFile("ArthFood_AutoBackup.json") ?: dir.findFile("arthfood_autobackup.json")
            val target = existing ?: dir.createFile("application/json", "ArthFood_AutoBackup.json") ?: return@withContext false
            val json = buildBackupJson()
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            prefs.setLastBackupAt(System.currentTimeMillis())
            true
        }.getOrDefault(false)
    }

    suspend fun copyLogoToInternalStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val outFile = File(context.filesDir, "business_logo.png")
            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            } ?: return@withContext null

            val maxWidth = 300
            val scaled = if (decoded.width > maxWidth) {
                val ratio = maxWidth.toFloat() / decoded.width.toFloat()
                val newH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                android.graphics.Bitmap.createScaledBitmap(decoded, maxWidth, newH, true)
            } else {
                decoded
            }

            FileOutputStream(outFile).use { output ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, output)
            }
            if (scaled !== decoded) decoded.recycle()
            outFile.absolutePath
        }.getOrNull()
    }

    private fun OrderWithItemsEntity.toDomain(): OrderRecord = OrderRecord(
        id = order.id,
        invoiceNumber = order.invoiceNumber,
        customerName = order.customerName,
        customerPhone = order.customerPhone,
        customerAddress = order.customerAddress,
        notes = order.notes,
        status = OrderStatus.fromKey(order.status),
        paymentStatus = PaymentStatus.fromKey(order.paymentStatus),
        paidAmount = order.paidAmount,
        deliveryCharge = order.deliveryCharge,
        otherCharge = order.otherCharge,
        discount = order.discount,
        taxPercent = order.taxPercent,
        subtotal = order.subtotal,
        taxAmount = order.taxAmount,
        grandTotal = order.grandTotal,
        totalVendorCost = order.totalVendorCost,
        sentAt = order.sentAt,
        paidAt = order.paidAt,
        createdAt = order.createdAt,
        updatedAt = order.updatedAt,
        items = items.map { li ->
            OrderLineItem(
                id = li.id,
                orderId = li.orderId,
                catalogItemId = li.catalogItemId,
                itemName = li.itemName,
                packGrams = li.packGrams,
                quantity = li.quantity,
                unitPrice = li.unitPrice,
                sellingPricePerKg = li.sellingPricePerKg,
                vendorCostPerKg = li.vendorCostPerKg,
                vendorId = li.vendorId,
                vendorName = li.vendorName,
                isFreeSample = li.isFreeSample,
                isVendorLinePaid = li.isVendorLinePaid
            )
        }
    )
}
