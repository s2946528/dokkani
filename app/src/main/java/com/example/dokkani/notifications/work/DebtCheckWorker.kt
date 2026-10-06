package com.example.dokkani.notifications.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.NotificationEntity
import com.example.dokkani.data.local.entities.NotificationType
import com.example.dokkani.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * مهمة خلفية آمنة عبر WorkManager لمتابعة ديون العملاء والآجل
 */
class DebtCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DebtCheckWorker", "بدء فحص ديون الآجل والعملاء في الخلفية...")

            val db = DokkaniDatabase.getDatabase(applicationContext, this)
            val parties = db.partyDao().getAllPartiesSync()

            var indebtedCustomers = 0
            var totalDebtAmount = 0.0

            for (party in parties) {
                if (party.currentBalance > 0) { // العميل عليه مديونية
                    indebtedCustomers++
                    totalDebtAmount += party.currentBalance
                }
            }

            if (indebtedCustomers > 0) {
                val message = "يوجد $indebtedCustomers عميل عليه ديون آجلة بإجمالي مبالغ ${totalDebtAmount.toInt()} ر.ي"

                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = "تنبيه متابعة الديون والآجل",
                        message = message,
                        type = NotificationType.DEBT_DUE,
                        timestamp = System.currentTimeMillis()
                    )
                )

                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = "تنبيه استحقاق الديون 💳",
                    body = message,
                    channelId = NotificationHelper.CHANNEL_DEBTS
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DebtCheckWorker", "خطأ في فحص ديون العملاء بالخلفية: ${e.message}", e)
            Result.failure()
        }
    }
}
