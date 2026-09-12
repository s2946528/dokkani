package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.InvoiceWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY date DESC")
    suspend fun getAllInvoicesSync(): List<InvoiceEntity>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getInvoicesWithDetails(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceWithDetailsById(invoiceId: Long): InvoiceWithDetails?

    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getInvoiceById(invoiceId: Long): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE partyId = :partyId ORDER BY date DESC")
    fun getInvoicesForParty(partyId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE partyId = :partyId ORDER BY date DESC")
    suspend fun getInvoicesForPartySync(partyId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE type = :type ORDER BY date DESC")
    fun getInvoicesByType(type: InvoiceType): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsSync(): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>): List<Long>

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun countInvoices(): Int

    @Query("SELECT COUNT(*) FROM invoices WHERE type = 'SALE'")
    suspend fun countSaleInvoices(): Int
}
