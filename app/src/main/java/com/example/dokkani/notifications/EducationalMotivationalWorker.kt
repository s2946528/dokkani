package com.example.dokkani.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * عامل الخلفية (WorkManager Worker) لعرض الرسائل والنصائح التعليمية والتحفيزية بصفة دورية
 */
class EducationalMotivationalWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        // قائمة بالنصائح المحاسبية والإدارية والتحفيزية
        val EDUCATIONAL_TIPS = listOf(
            Pair(
                "نصيحة محاسبية: مطابقة النقدية",
                "احرص دائماً على مطابقة الدرج ومراجعة المبيعات الآجلة قبل إغلاق الشفت اليومي لتفادي العجز."
            ),
            Pair(
                "تلميحة إدارية: الجرد الدوري",
                "إجراء الجرد المستمر للأصناف ذات الحركة السريعة يمنع تسرب المخزون ويكشف الفروقات مبكراً."
            ),
            Pair(
                "رسالة تحفيزية: كفاءة الخدمة",
                "السرعة والدقة في إكمال فواتير العميل تبني الولاء وتزيد من متوسط المبيعات اليومية لمتجرك."
            ),
            Pair(
                "قواعد إدارة المخزون: FIFO",
                "اعتماد مبدأ (ما يدخل أولاً يخرج أولاً) يقلل من تكلفة التلف وإكسباير المنتجات الغذائية والسلع."
            ),
            Pair(
                "حماية البيانات والنسخ الاحتياطي",
                "تأكد من إنشاء نسخة احتياطية إلكترونية لقاعدة البيانات بانتظام لحماية حقوقك وحسابات العملاء."
            ),
            Pair(
                "تحليل الربحية: هامش الربح الإجمالي",
                "راجع تقارير الأرباح الدورية لمعرفة الأصناف الأكثر ربحية والتركيز على تسويقها في نقطة البيع."
            )
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // اختيار نصيحة أو رسالة تحفيزية عشوائية
            val selectedTip = EDUCATIONAL_TIPS[Random.nextInt(EDUCATIONAL_TIPS.size)]

            NotificationHelper.showEducationalTipNotification(
                context = context,
                title = selectedTip.first,
                tipContent = selectedTip.second
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
