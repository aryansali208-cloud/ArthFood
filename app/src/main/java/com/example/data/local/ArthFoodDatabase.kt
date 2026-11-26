package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PackSizeEntity::class,
        VendorEntity::class,
        CatalogItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        VendorPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ArthFoodDatabase : RoomDatabase() {
    abstract fun packSizeDao(): PackSizeDao
    abstract fun vendorDao(): VendorDao
    abstract fun catalogDao(): CatalogDao
    abstract fun orderDao(): OrderDao
    abstract fun vendorPaymentDao(): VendorPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: ArthFoodDatabase? = null

        fun getInstance(context: Context): ArthFoodDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): ArthFoodDatabase {
            return Room.databaseBuilder(
                appContext,
                ArthFoodDatabase::class.java,
                "arthfood_billing.db"
            )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val instance = getInstance(appContext)
                            instance.packSizeDao().insertAll(
                                listOf(
                                    PackSizeEntity(250, "", 1),
                                    PackSizeEntity(500, "", 2),
                                    PackSizeEntity(750, "", 3),
                                    PackSizeEntity(1000, "1 kg", 4)
                                )
                            )
                        }
                    }
                })
                .build()
        }
    }
}
