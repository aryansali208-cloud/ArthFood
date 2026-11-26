package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PackSizeDao {
    @Query("SELECT * FROM pack_sizes ORDER BY sortOrder ASC, grams ASC")
    fun observeAll(): Flow<List<PackSizeEntity>>

    @Query("SELECT * FROM pack_sizes ORDER BY sortOrder ASC, grams ASC")
    suspend fun getAllOnce(): List<PackSizeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packSize: PackSizeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(packs: List<PackSizeEntity>)

    @Query("DELETE FROM pack_sizes WHERE grams = :grams")
    suspend fun deleteByGrams(grams: Int)
}

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors ORDER BY name ASC")
    fun observeAll(): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors ORDER BY name ASC")
    suspend fun getAllOnce(): List<VendorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vendor: VendorEntity): Long

    @Update
    suspend fun update(vendor: VendorEntity)

    @Query("DELETE FROM vendors WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items ORDER BY name ASC")
    suspend fun getAllOnce(): List<CatalogItemEntity>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getById(id: Long): CatalogItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CatalogItemEntity): Long

    @Update
    suspend fun update(item: CatalogItemEntity)

    @Query("UPDATE catalog_items SET stockGrams = stockGrams + :deltaGrams WHERE id = :id")
    suspend fun adjustStock(id: Long, deltaGrams: Int)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllWithItems(): Flow<List<OrderWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllWithItemsOnce(): List<OrderWithItemsEntity>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getByIdWithItems(orderId: Long): OrderWithItemsEntity?

    @Query("SELECT * FROM orders WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getByInvoiceNumber(invoiceNumber: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: Long)

    @Query("UPDATE orders SET status = :status, sentAt = :sentAt, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, status: String, sentAt: Long, updatedAt: Long)

    @Query("UPDATE orders SET paymentStatus = :paymentStatus, paidAmount = :paidAmount, paidAt = :paidAt, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updatePayment(orderId: Long, paymentStatus: String, paidAmount: Double, paidAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE order_items SET isVendorLinePaid = 1
        WHERE (vendorId = :vendorId OR (:vendorId <= 0 AND LOWER(vendorName) = LOWER(:vendorName)))
          AND orderId IN (
            SELECT id FROM orders WHERE status != 'CANCELLED' AND createdAt BETWEEN :startMs AND :endMs
          )
        """
    )
    suspend fun markVendorLinesPaid(vendorId: Long, vendorName: String, startMs: Long, endMs: Long)
}

@Dao
interface VendorPaymentDao {
    @Query("SELECT * FROM vendor_payments ORDER BY paymentDate DESC")
    fun observeAll(): Flow<List<VendorPaymentEntity>>

    @Query("SELECT * FROM vendor_payments ORDER BY paymentDate DESC")
    suspend fun getAllOnce(): List<VendorPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: VendorPaymentEntity): Long

    @Query("DELETE FROM vendor_payments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
