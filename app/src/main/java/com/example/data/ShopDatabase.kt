package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val imageUrl: String = ""
)

@Entity(tableName = "nfc_cards")
data class NfcCardEntity(
    @PrimaryKey val uid: String, // e.g. "BF:28:92:76"
    val cardholderName: String,
    val balance: Double,
    val registeredAt: Long = System.currentTimeMillis()
)

@Dao
interface ShopDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM nfc_cards")
    fun getAllCards(): Flow<List<NfcCardEntity>>

    @Query("SELECT * FROM nfc_cards WHERE uid = :uid LIMIT 1")
    suspend fun getCardByUid(uid: String): NfcCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: NfcCardEntity)

    @Update
    suspend fun updateCard(card: NfcCardEntity)
}

@Database(entities = [ProductEntity::class, NfcCardEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_simulator_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.shopDao())
                }
            }
        }

        suspend fun populateDatabase(shopDao: ShopDao) {
            // Pre-populate Products
            val defaultProducts = listOf(
                ProductEntity(barcode = "8801097250041", name = "Banana Milk Shake", price = 1.99, stock = 20, category = "Beverages"),
                ProductEntity(barcode = "6941582230018", name = "Matcha Choco Biscuit", price = 2.49, stock = 15, category = "Snacks"),
                ProductEntity(barcode = "070046123451", name = "Classic Potato Chips", price = 1.89, stock = 30, category = "Snacks"),
                ProductEntity(barcode = "4009993005231", name = "Sparkling Spring Water", price = 0.99, stock = 50, category = "Beverages"),
                ProductEntity(barcode = "4901301275461", name = "Premium Matcha Tea Cup", price = 3.99, stock = 12, category = "Beverages"),
                ProductEntity(barcode = "7613034921004", name = "Double Chocolate Bar", price = 1.49, stock = 25, category = "Snacks"),
                ProductEntity(barcode = "012000000133", name = "Sparkling Galactic Cola", price = 1.29, stock = 40, category = "Beverages"),
                ProductEntity(barcode = "9780131103627", name = "Retro Coding Notebook", price = 4.99, stock = 8, category = "Stationery"),
                ProductEntity(barcode = "12345678", name = "Organic Red Strawberries", price = 3.50, stock = 10, category = "Produce"),
                ProductEntity(barcode = "8712100325947", name = "Fresh Cream Cheese Bunch", price = 2.79, stock = 14, category = "Dairy")
            )
            for (p in defaultProducts) {
                shopDao.insertProduct(p)
            }

            // Pre-populate Default NFC Card from the user's uploaded screenshot!
            // UID on screenshot is BF:28:92:76
            val defaultNfcCard = NfcCardEntity(
                uid = "BF:28:92:76",
                cardholderName = "Vince Szollosi",
                balance = 100.00
            )
            shopDao.insertCard(defaultNfcCard)
        }
    }
}

class ShopRepository(private val shopDao: ShopDao) {
    val allProducts: Flow<List<ProductEntity>> = shopDao.getAllProducts()
    val allCards: Flow<List<NfcCardEntity>> = shopDao.getAllCards()

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return shopDao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: ProductEntity) {
        shopDao.insertProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        shopDao.deleteProduct(product)
    }

    suspend fun getCardByUid(uid: String): NfcCardEntity? {
        return shopDao.getCardByUid(uid)
    }

    suspend fun insertCard(card: NfcCardEntity) {
        shopDao.insertCard(card)
    }

    suspend fun updateCard(card: NfcCardEntity) {
        shopDao.updateCard(card)
    }
}
