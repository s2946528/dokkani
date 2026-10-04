package com.example.dokkani.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * جدولة وتنفيذ مهام خلفية الإشعارات والرسائل التنبيهية عبر WorkManager
 */
object NotificationWorkScheduler {

    private const val PERIODIC_WORK_TAG = "dokkani_educational_periodic_work"
    private const val IMMEDIATE_WORK_TAG = "dokkani_educational_immediate_work"

    /**
     * جدولة المهام التنبيهية والتحفيزية الدورية (كل 12 أو 24 ساعة)
     */
    fun schedulePeriodicMotivationalWorker(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<EducationalMotivationalWorker>(
                12, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(PERIODIC_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorkScheduler", "Failed to schedule periodic worker: ${e.message}", e)
        }
    }

    /**
     * إطلاق وتجربة إشعار تحفيزي محلي فوري للاختبار
     */
    fun triggerImmediateMotivationalWorker(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<EducationalMotivationalWorker>()
                .addTag(IMMEDIATE_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorkScheduler", "Failed to trigger immediate worker: ${e.message}", e)
        }
    }
}
