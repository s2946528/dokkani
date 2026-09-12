package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id DESC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id DESC")
    suspend fun getProductsSync(): List<ProductEntity>

    @Transaction
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id DESC")
    fun getProductsWithUnits(): Flow<List<ProductWithUnits>>

    @Transaction
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY id DESC")
    suspend fun getProductsWithUnitsSync(): List<ProductWithUnits>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductWithUnitsById(productId: Long): ProductWithUnits?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM product_units WHERE productId = :productId ORDER BY isBaseUnit DESC, conversionFactor ASC")
    fun getUnitsForProduct(productId: Long): Flow<List<ProductUnitEntity>>

    @Query("SELECT * FROM product_units WHERE productId = :productId ORDER BY isBaseUnit DESC, conversionFactor ASC")
    suspend fun getUnitsForProductSync(productId: Long): List<ProductUnitEntity>

    @Query("SELECT * FROM product_units WHERE id = :unitId LIMIT 1")
    suspend fun getUnitById(unitId: Long): ProductUnitEntity?

    @Query("SELECT * FROM product_units WHERE productId = :productId AND isBaseUnit = 1 LIMIT 1")
    suspend fun getBaseUnitForProduct(productId: Long): ProductUnitEntity?

    @Query("SELECT pu.* FROM product_units pu WHERE pu.barcode = :barcode LIMIT 1")
    suspend fun findUnitByBarcode(barcode: String): ProductUnitEntity?

    @Transaction
    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    suspend fun findProductByCode(code: String): ProductWithUnits?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: ProductUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<ProductUnitEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Update
    suspend fun updateUnit(unit: ProductUnitEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Delete
    suspend fun deleteUnit(unit: ProductUnitEntity)

    @Query("DELETE FROM product_units WHERE productId = :productId")
    suspend fun deleteUnitsByProductId(productId: Long)
}
