package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY date DESC")
    fun getInvoicesByType(type: InvoiceType): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY date DESC LIMIT :limit")
    fun getRecentInvoicesWithDetails(limit: Int = 20): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceWithDetailsById(invoiceId: Long): InvoiceWithDetails?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoice(invoiceId: Long): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>): List<Long>

    @Query("SELECT * FROM invoices WHERE partyId = :partyId ORDER BY date DESC")
    fun getInvoicesForParty(partyId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE partyId = :partyId ORDER BY date DESC")
    suspend fun getInvoicesForPartySync(partyId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    suspend fun getInvoicesByDateRangeSync(startTime: Long, endTime: Long): List<InvoiceEntity>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    suspend fun getAllInvoicesWithDetailsSync(): List<InvoiceWithDetails>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsSync(): List<InvoiceItemEntity>

    @Query("SELECT COUNT(*) FROM invoices WHERE type = :type")
    suspend fun countInvoicesByType(type: InvoiceType): Int

    @Query("SELECT COUNT(*) FROM invoices")
    fun getTotalInvoicesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getTotalInvoicesCountSync(): Int

    @Query("SELECT MAX(date) FROM invoices")
    suspend fun getLatestInvoiceTimestampSync(): Long?
}
