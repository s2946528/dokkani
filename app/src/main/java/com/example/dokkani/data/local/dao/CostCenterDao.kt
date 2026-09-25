package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.CostCenterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CostCenterDao {

    @Query("SELECT * FROM cost_centers ORDER BY is_general DESC, center_name ASC")
    fun getAllCostCenters(): Flow<List<CostCenterEntity>>

    @Query("SELECT * FROM cost_centers WHERE is_active = 1 ORDER BY is_general DESC, center_name ASC")
    fun getAllActiveCostCenters(): Flow<List<CostCenterEntity>>

    @Query("SELECT * FROM cost_centers WHERE is_active = 1 ORDER BY is_general DESC, center_name ASC")
    suspend fun getAllActiveCostCentersSync(): List<CostCenterEntity>

    @Query("SELECT * FROM cost_centers WHERE center_id = :id LIMIT 1")
    suspend fun getCostCenterById(id: Long): CostCenterEntity?

    @Query("SELECT * FROM cost_centers WHERE is_general = 1 LIMIT 1")
    suspend fun getGeneralCostCenter(): CostCenterEntity?

    @Query("SELECT COUNT(*) FROM cost_centers")
    suspend fun getCostCenterCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCostCenter(center: CostCenterEntity): Long

    @Update
    suspend fun updateCostCenter(center: CostCenterEntity)

    @Delete
    suspend fun deleteCostCenter(center: CostCenterEntity)

    @Query("DELETE FROM cost_centers WHERE center_id = :id AND is_general = 0")
    suspend fun deleteCostCenterById(id: Long): Int

    // استعلامات إحصائية ومحاسبية لمراكز التكلفة
    @Query("SELECT COUNT(*) FROM products WHERE cost_center_id = :centerId")
    suspend fun getProductsCountByCostCenter(centerId: Long): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE cost_center_id = :centerId")
    suspend fun getExpensesCountByCostCenter(centerId: Long): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE cost_center_id = :centerId")
    suspend fun getInvoicesCountByCostCenter(centerId: Long): Int

    @Query("SELECT COUNT(*) FROM product_wastage WHERE cost_center_id = :centerId")
    suspend fun getWastageCountByCostCenter(centerId: Long): Int
}
