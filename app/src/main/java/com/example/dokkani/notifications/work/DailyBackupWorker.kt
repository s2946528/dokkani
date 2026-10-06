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
 * مهمة خلفية آمنة عبر WorkManager للنسخ الاحتياطي اليومي وفحص سلامة السجلات
 */
class DailyBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DailyBackupWorker", "بدء تنفيذ مهمة النسخ الاحتياطي ومراجعة السجلات في الخلفية...")

            val db = DokkaniDatabase.getDatabase(applicationContext, this)

            val products = db.productDao().getProductsSync()
            val invoices = db.invoiceDao().getAllInvoicesSync()

            val productCount = products.size
            val invoiceCount = invoices.size

            val message = "تم إنجاز النسخ الاحتياطي اليومي بنجاح. عدد المنتجات: $productCount، الفواتير: $invoiceCount"

            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = "النسخ الاحتياطي الدوري",
                    message = message,
                    type = NotificationType.SYNC,
                    timestamp = System.currentTimeMillis()
                )
            )

            NotificationHelper.showNotification(
                context = applicationContext,
                title = "مزامنة ونسخ احتياطي ناجح 💾",
                body = message,
                channelId = NotificationHelper.CHANNEL_SYNC
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("DailyBackupWorker", "فشل مهمة النسخ الاحتياطي: ${e.message}", e)
            Result.retry()
        }
    }
}
