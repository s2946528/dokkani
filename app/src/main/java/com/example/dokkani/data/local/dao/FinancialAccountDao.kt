package com.example.dokkani.data.local.dao

import androidx.room.*
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAccountDao {

    @Query("SELECT * FROM financial_accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts ORDER BY code ASC")
    suspend fun getAllAccountsSync(): List<FinancialAccountEntity>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 ORDER BY code ASC")
    fun getActiveAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 AND currentBalance != 0.0 AND accountType IN ('BANK', 'E_WALLET') ORDER BY code ASC")
    fun getActiveNonZeroBankAndWalletAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 AND currentBalance != 0.0 AND accountType IN ('BANK', 'E_WALLET') ORDER BY code ASC")
    suspend fun getActiveNonZeroBankAndWalletAccountsSync(): List<FinancialAccountEntity>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 AND currentBalance != 0.0 AND accountType != 'CASH_DRAWER' ORDER BY code ASC")
    fun getActiveNonZeroAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 AND currentBalance != 0.0 AND accountType != 'CASH_DRAWER' ORDER BY code ASC")
    suspend fun getActiveNonZeroAccountsSync(): List<FinancialAccountEntity>

    @Query("SELECT * FROM financial_accounts WHERE accountType = :type ORDER BY code ASC")
    fun getAccountsByType(type: FinancialAccountType): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): FinancialAccountEntity?

    @Query("SELECT * FROM financial_accounts WHERE code = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): FinancialAccountEntity?

    @Query("SELECT COUNT(*) FROM financial_accounts")
    suspend fun getAccountsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: FinancialAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<FinancialAccountEntity>)

    @Query("UPDATE financial_accounts SET currentBalance = currentBalance + :delta WHERE id = :id")
    suspend fun updateBalance(id: Long, delta: Double)

    @Update
    suspend fun updateAccount(account: FinancialAccountEntity)

    @Delete
    suspend fun deleteAccount(account: FinancialAccountEntity)

    @Query("DELETE FROM financial_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)
}
