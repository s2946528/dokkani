package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    suspend fun getMovementsForProductSync(productId: Long): List<StockMovementEntity>

    /**
     * استعلام طبقات الشراء المتوفرة المتبقية لطريقة FIFO
     * مرتبة من الأقدم إلى الأحدث (الوارد أولاً يستهلك أولاً)
     */
    @Query(
        """
        SELECT * FROM stock_movements 
        WHERE productId = :productId 
          AND remainingQuantityForFifo > 0.0001
          AND movementType IN ('PURCHASE_IN', 'PRODUCE_SORTING', 'RETURN_IN')
        ORDER BY timestamp ASC
        """
    )
    suspend fun getAvailableFifoLots(productId: Long): List<StockMovementEntity>

    /**
     * استرداد آخر حركة توريد/شراء لحساب (آخر سعر شراء - Last Purchase Price)
     */
    @Query(
        """
        SELECT * FROM stock_movements 
        WHERE productId = :productId 
          AND movementType IN ('PURCHASE_IN', 'PRODUCE_SORTING')
        ORDER BY timestamp DESC 
        LIMIT 1
        """
    )
    suspend fun getLastPurchaseMovement(productId: Long): StockMovementEntity?

    /**
     * استرداد جميع الطبقات المتاحة بالمخزن لحساب المتوسط المرجح WAC
     * بناءً على الكميات المتبقية وتكلفتها
     */
    @Query(
        """
        SELECT * FROM stock_movements 
        WHERE productId = :productId 
          AND remainingQuantityForFifo > 0.0001
          AND movementType IN ('PURCHASE_IN', 'PRODUCE_SORTING', 'RETURN_IN')
        ORDER BY timestamp ASC
        """
    )
    suspend fun getActiveStockLotsForWac(productId: Long): List<StockMovementEntity>

    /**
     * إجمالي الرصيد الحالي للصنف بالمخزن بالوحدة الأساسية
     */
    @Query(
        """
        SELECT COALESCE(SUM(quantityBaseUnit), 0.0) 
        FROM stock_movements 
        WHERE productId = :productId
        """
    )
    suspend fun getTotalStockQuantity(productId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>): List<Long>

    @Update
    suspend fun updateMovement(movement: StockMovementEntity)
}
