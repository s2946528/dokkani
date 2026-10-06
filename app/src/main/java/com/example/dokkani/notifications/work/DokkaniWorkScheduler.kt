package com.example.dokkani.notifications.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * جدولة وتنفيذ المهام الخلفية لـ WorkManager بنظافة وأمان دون التأثير على Main Thread
 */
object DokkaniWorkScheduler {

    private const val WORK_BACKUP_NAME = "DokkaniDailyBackupWork"
    private const val WORK_STOCK_NAME = "DokkaniStockSyncWork"
    private const val WORK_DEBT_NAME = "DokkaniDebtCheckWork"

    /**
     * جدولة كافة المهام الدورية الخلفية للتطبيق
     */
    fun schedulePeriodicTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val defaultConstraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. النسخ الاحتياطي اليومي (كل 24 ساعة)
        val backupRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_BACKUP_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )

        // 2. فحص المخزون (كل 12 ساعة)
        val stockRequest = PeriodicWorkRequestBuilder<StockSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_STOCK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            stockRequest
        )

        // 3. فحص الديون والآجل (كل 12 ساعة)
        val debtRequest = PeriodicWorkRequestBuilder<DebtCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(defaultConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_DEBT_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            debtRequest
        )
    }

    /**
     * تشغيل فورى لمهمة النسخ الاحتياطي بالخلفية
     */
    fun runBackupNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyBackupWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTime_Backup",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * تشغيل فورى لمهمة فحص الأصناف والمخزون المنخفض
     */
    fun runStockCheckNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<StockSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTime_Stock",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * تشغيل فورى لمهمة فحص الديون والآجل
     */
    fun runDebtCheckNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DebtCheckWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "OneTime_Debt",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
