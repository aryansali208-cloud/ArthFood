package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "pack_sizes")
data class PackSizeEntity(
    @PrimaryKey val grams: Int,
    val customName: String = "",
    val sortOrder: Int = 0
)

@Entity(tableName = "vendors")
data class VendorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val notes: String = ""
)

@Entity(tableName = "catalog_items")
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val notes: String,
    val status: String,
    val paymentStatus: String,
    val paidAmount: Double,
    val deliveryCharge: Double,
    val otherCharge: Double,
    val discount: Double,
    val taxPercent: Double,
    val subtotal: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val totalVendorCost: Double,
    val sentAt: Long = 0L,
    val paidAt: Long = 0L,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: Long,
    val catalogItemId: Long?,
    val itemName: String,
    val packGrams: Int,
    val quantity: Int,
    val unitPrice: Double,
    val sellingPricePerKg: Double,
    val vendorCostPerKg: Double,
    val vendorId: Long?,
    val vendorName: String,
    val isFreeSample: Boolean,
    val isVendorLinePaid: Boolean = false,
    val lineSaleTotal: Double,
    val lineCostTotal: Double
)

data class OrderWithItemsEntity(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)

@Entity(tableName = "vendor_payments")
data class VendorPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val vendorId: Long,
    val vendorName: String,
    val amount: Double,
    val paymentDate: Long,
    val mode: String,
    val note: String
)
