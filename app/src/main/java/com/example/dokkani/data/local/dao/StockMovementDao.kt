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

    @Query(
        """
        SELECT DISTINCT sm.* FROM stock_movements sm
        WHERE sm.productId = :productId 
          AND (
              sm.invoiceId IS NULL 
              OR EXISTS (
                  SELECT 1 FROM invoices inv 
                  WHERE inv.id = sm.invoiceId 
                    AND inv.status != 'CANCELLED'
              )
          )
          AND (
              sm.referenceNumber IS NULL 
              OR sm.referenceNumber NOT IN (
                  SELECT invoiceNumber FROM invoices WHERE status = 'CANCELLED'
              )
          )
        ORDER BY sm.timestamp DESC
        """
    )
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query(
        """
        SELECT DISTINCT sm.* FROM stock_movements sm
        WHERE sm.productId = :productId 
          AND (
              sm.invoiceId IS NULL 
              OR EXISTS (
                  SELECT 1 FROM invoices inv 
                  WHERE inv.id = sm.invoiceId 
                    AND inv.status != 'CANCELLED'
              )
          )
          AND (
              sm.referenceNumber IS NULL 
              OR sm.referenceNumber NOT IN (
                  SELECT invoiceNumber FROM invoices WHERE status = 'CANCELLED'
              )
          )
        ORDER BY sm.timestamp DESC
        """
    )
    suspend fun getMovementsForProductSync(productId: Long): List<StockMovementEntity>

    @Query(
        """
        SELECT DISTINCT sm.* FROM stock_movements sm
        WHERE sm.productId = :productId 
          AND (:costCenterId IS NULL OR :costCenterId = 0 OR sm.cost_center_id = :costCenterId)
          AND (
              sm.invoiceId IS NULL 
              OR EXISTS (
                  SELECT 1 FROM invoices inv 
                  WHERE inv.id = sm.invoiceId 
                    AND inv.status != 'CANCELLED'
              )
          )
          AND (
              sm.referenceNumber IS NULL 
              OR sm.referenceNumber NOT IN (
                  SELECT invoiceNumber FROM invoices WHERE status = 'CANCELLED'
              )
          )
        ORDER BY sm.timestamp ASC
        """
    )
    suspend fun getDetailedMovementsForProduct(productId: Long, costCenterId: Long? = null): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAllMovementsSync(): List<StockMovementEntity>

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
          AND (:costCenterId IS NULL OR cost_center_id = :costCenterId)
        ORDER BY timestamp ASC
        """
    )
    suspend fun getAvailableFifoLots(productId: Long, costCenterId: Long? = null): List<StockMovementEntity>

    /**
     * استرداد آخر حركة توريد/شراء لحساب (آخر سعر شراء - Last Purchase Price)
     */
    @Query(
        """
        SELECT * FROM stock_movements 
        WHERE productId = :productId 
          AND movementType IN ('PURCHASE_IN', 'PRODUCE_SORTING')
          AND (:costCenterId IS NULL OR cost_center_id = :costCenterId)
        ORDER BY timestamp DESC 
        LIMIT 1
        """
    )
    suspend fun getLastPurchaseMovement(productId: Long, costCenterId: Long? = null): StockMovementEntity?

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
          AND (:costCenterId IS NULL OR cost_center_id = :costCenterId)
        ORDER BY timestamp ASC
        """
    )
    suspend fun getActiveStockLotsForWac(productId: Long, costCenterId: Long? = null): List<StockMovementEntity>

    /**
     * إجمالي الرصيد الحالي للصنف بالمخزن بالوحدة الأساسية
     */
    @Query(
        """
        SELECT COALESCE(SUM(quantityBaseUnit), 0.0) 
        FROM stock_movements 
        WHERE productId = :productId
          AND (:costCenterId IS NULL OR :costCenterId = 0 OR cost_center_id = :costCenterId)
          AND (:warehouseId IS NULL OR :warehouseId = 0 OR warehouse_id = :warehouseId)
        """
    )
    suspend fun getTotalStockQuantity(productId: Long, costCenterId: Long? = null, warehouseId: Long? = null): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>): List<Long>

    @Update
    suspend fun updateMovement(movement: StockMovementEntity)

    @Query("DELETE FROM stock_movements WHERE referenceNumber = :referenceNumber")
    suspend fun deleteMovementsByReferenceNumber(referenceNumber: String): Int

    @Query("DELETE FROM stock_movements WHERE invoiceId = :invoiceId")
    suspend fun deleteMovementsByInvoiceId(invoiceId: Long): Int

    @Query("SELECT * FROM stock_movements WHERE referenceNumber = :referenceNumber")
    suspend fun getMovementsByReferenceNumber(referenceNumber: String): List<StockMovementEntity>
}
