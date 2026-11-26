package com.example.data.model

import com.example.util.Formatters

enum class AppLanguage(val code: String, val nativeLabel: String) {
    EN("en", "English"),
    HI("hi", "हिंदी"),
    MR("mr", "मराठी");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class OrderStatus(val key: String) {
    PENDING("PENDING"),
    PACKED("PACKED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromKey(key: String?): OrderStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class PaymentStatus(val key: String) {
    UNPAID("UNPAID"),
    PARTIAL("PARTIAL"),
    PAID("PAID");

    companion object {
        fun fromKey(key: String?): PaymentStatus =
            entries.firstOrNull { it.key == key } ?: UNPAID
    }
}

enum class OrderFilter {
    ALL, IN_PROCESS, PENDING, PACKED, DELIVERED, PAID, UNPAID, SENT_UNPAID, CANCELLED
}

enum class MainTab {
    ORDERS, NEW_ORDER, MANAGE
}

enum class ManageTab {
    BUSINESS, RATES_PROFIT, VENDORS, CUSTOMERS, ORDER_QR, BACKUP_EXCEL
}

enum class DateRangePreset {
    TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME, CUSTOM
}

enum class ProfitOrderScope {
    ALL_ACTIVE, SENT_ONLY, PAID_ONLY
}

data class BusinessProfile(
    val businessName: String = "ArthFood Kitchen",
    val localBusinessName: String = "",
    val tagline: String = "Made billing easy",
    val ownerName: String = "",
    val phone: String = "",
    val whatsappSameAsPhone: Boolean = true,
    val whatsappNumber: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pinCode: String = "",
    val upiId: String = "",
    val taxPercent: Double = 0.0,
    val taxLine: String = "",
    val thankYouNote: String = "Thank you for your order! Visit again.",
    val qrContactName: String = "",
    val logoPath: String = ""
) {
    val effectiveWhatsApp: String
        get() = if (whatsappSameAsPhone) phone else whatsappNumber.ifBlank { phone }

    val fullAddress: String
        get() = listOf(address, city, state, pinCode).filter { it.isNotBlank() }.joinToString(", ")
}

data class PackSize(
    val grams: Int,
    val customName: String = "",
    val sortOrder: Int = 0
) {
    val displayLabel: String
        get() = customName.ifBlank {
            if (grams >= 1000 && grams % 1000 == 0) "${grams / 1000} kg"
            else if (grams >= 1000) String.format(java.util.Locale.getDefault(), "%.2f kg", grams / 1000.0)
            else "$grams g"
        }
}

data class LicenseState(
    val deviceId: String = "",
    val isLicensed: Boolean = false,
    val licenseKey: String = "",
    val trialStartMillis: Long = 0L,
    val daysRemaining: Int = 7,
    val isTrialExpired: Boolean = false,
    val isClockTampered: Boolean = false
) {
    val isLockedOut: Boolean
        get() = !isLicensed && (isTrialExpired || isClockTampered)
}

data class CatalogItem(
    val id: Long = 0L,
    val name: String,
    val sellingPricePerKg: Double,
    val vendorCostPerKg: Double,
    val vendorId: Long? = null,
    val vendorName: String = "",
    val stockGrams: Int = 0,
    val lowStockThresholdGrams: Int = 1000,
    val trackStock: Boolean = false,
    val packPriceOverridesJson: String = "",
    val isActive: Boolean = true
) {
    fun priceForPack(packGrams: Int): Double {
        if (packPriceOverridesJson.isNotBlank()) {
            val map = parsePackOverrides(packPriceOverridesJson)
            map[packGrams]?.let { if (it > 0.0) return Formatters.round2(it) }
        }
        return Formatters.round2((sellingPricePerKg * packGrams) / 1000.0)
    }

    companion object {
        fun parsePackOverrides(json: String): Map<Int, Double> {
            if (json.isBlank()) return emptyMap()
            return runCatching {
                val obj = org.json.JSONObject(json)
                buildMap {
                    obj.keys().forEach { k ->
                        val g = k.toIntOrNull()
                        val p = obj.optDouble(k, -1.0)
                        if (g != null && g > 0 && p > 0.0) put(g, p)
                    }
                }
            }.getOrDefault(emptyMap())
        }
    }
}

data class Vendor(
    val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val notes: String = ""
)

data class OrderLineItem(
    val id: Long = 0L,
    val orderId: Long = 0L,
    val catalogItemId: Long? = null,
    val itemName: String,
    val packGrams: Int,
    val quantity: Int,
    val unitPrice: Double,
    val sellingPricePerKg: Double,
    val vendorCostPerKg: Double,
    val vendorId: Long? = null,
    val vendorName: String = "",
    val isFreeSample: Boolean = false,
    val isVendorLinePaid: Boolean = false
) {
    val lineSaleTotal: Double
        get() = if (isFreeSample) 0.0 else Formatters.round2(unitPrice * quantity)

    val lineCostTotal: Double
        get() = Formatters.round2((vendorCostPerKg * packGrams / 1000.0) * quantity)

    val totalGrams: Int
        get() = packGrams * quantity
}

data class OrderRecord(
    val id: Long = 0L,
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val notes: String = "",
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paidAmount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val otherCharge: Double = 0.0,
    val discount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalVendorCost: Double = 0.0,
    val sentAt: Long = 0L,
    val paidAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<OrderLineItem> = emptyList()
) {
    val isSent: Boolean
        get() = status == OrderStatus.DELIVERED || sentAt > 0L

    val totalPacks: Int
        get() = items.sumOf { it.quantity }

    val freeSampleCount: Int
        get() = items.filter { it.isFreeSample }.sumOf { it.quantity }

    val freeSampleCost: Double
        get() = Formatters.round2(items.filter { it.isFreeSample }.sumOf { it.lineCostTotal })
    val balanceDue: Double
        get() = when (paymentStatus) {
            PaymentStatus.PAID -> 0.0
            PaymentStatus.UNPAID -> Formatters.round2(grandTotal)
            PaymentStatus.PARTIAL -> Formatters.round2((grandTotal - paidAmount).coerceAtLeast(0.0))
        }

    val effectivePaidAmount: Double
        get() = when (paymentStatus) {
            PaymentStatus.PAID -> Formatters.round2(grandTotal)
            PaymentStatus.UNPAID -> 0.0
            PaymentStatus.PARTIAL -> Formatters.round2(paidAmount.coerceIn(0.0, grandTotal))
        }
}

data class VendorPayment(
    val id: Long = 0L,
    val vendorId: Long,
    val vendorName: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val mode: String = "UPI",
    val note: String = ""
)

data class CustomerSummary(
    val key: String,
    val name: String,
    val phone: String,
    val orderCount: Int,
    val totalSpent: Double,
    val totalPaid: Double,
    val pendingDue: Double,
    val lastOrderAt: Long
)

data class PackSummaryEntry(
    val itemName: String,
    val packGrams: Int,
    val quantity: Int,
    val totalGrams: Int,
    val costTotal: Double,
    val vendorCostPerKg: Double = 0.0,
    val allPaid: Boolean = false
)

data class VendorPayableSummary(
    val vendor: Vendor,
    val totalGrams: Int,
    val totalPayable: Double,
    val totalPaid: Double,
    val balanceDue: Double,
    val packBreakdown: List<PackSummaryEntry>,
    val missingCostItems: List<String>,
    val hasUnpaidLines: Boolean = false
)

data class ProfitSummary(
    val orderCount: Int,
    val freeSampleCount: Int,
    val totalSales: Double,
    val totalVendorCost: Double,
    val deliveryAndOtherCharges: Double,
    val netProfit: Double,
    val marginPercent: Double,
    val missingVendorOrCostLines: Int = 0
)

data class ParsedWhatsAppOrder(
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val items: List<OrderLineItem>,
    val unmatchedLines: List<String>
)

data class BackupInspectionSummary(
    val jsonText: String,
    val totalOrdersInBackup: Int,
    val newOrdersToImport: Int,
    val itemsInBackup: Int,
    val newItemsToImport: Int,
    val vendorsInBackup: Int,
    val newVendorsToImport: Int,
    val packSizesInBackup: Int
)
