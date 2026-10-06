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
 * مهمة خلفية آمنة عبر WorkManager لمراقبة مستوى المخزون والأصناف المنخفضة
 */
class StockSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("StockSyncWorker", "بدء فحص مستويات المخزون في الخلفية...")

            val db = DokkaniDatabase.getDatabase(applicationContext, this)
            val productsWithUnits = db.productDao().getProductsWithUnitsSync()

            var lowStockCount = 0
            val lowStockNames = mutableListOf<String>()

            for (item in productsWithUnits) {
                val movements = db.stockMovementDao().getMovementsForProductSync(item.product.id)
                var stock = 0.0
                for (m in movements) {
                    stock += m.quantityBaseUnit
                }
                if (stock <= 5.0) { // حد التنبيه
                    lowStockCount++
                    if (lowStockNames.size < 3) {
                        lowStockNames.add(item.product.name)
                    }
                }
            }

            if (lowStockCount > 0) {
                val namesStr = lowStockNames.joinToString("، ")
                val message = "يوجد $lowStockCount صنف منخفض في المخزون (مثل: $namesStr). يرجى المراجعة وإعادة الطلب."

                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = "تنبيه نقص المخزون",
                        message = message,
                        type = NotificationType.LOW_STOCK,
                        timestamp = System.currentTimeMillis()
                    )
                )

                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = "تنبيه نقص المخزون ⚠️",
                    body = message,
                    channelId = NotificationHelper.CHANNEL_DEBTS
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("StockSyncWorker", "خطأ أثناء فحص المخزون بالخلفية: ${e.message}", e)
            Result.failure()
        }
    }
}
