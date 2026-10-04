package com.example.dokkani.util

import android.content.Context
import android.net.Uri
import com.example.dokkani.data.local.DokkaniDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * بيانات ومعلومات النسخة الاحتياطية الأخيرة
 */
data class BackupMetadata(
    val lastBackupDateFormatted: String,
    val lastBackupSizeBytes: Long
) {
    val formattedSize: String
        get() {
            if (lastBackupSizeBytes <= 0) return "غ.م"
            val kb = lastBackupSizeBytes / 1024.0
            return if (kb > 1024) {
                "%.2f ميجابايت".format(Locale.US, kb / 1024.0)
            } else {
                "%.1f كيلوبايت".format(Locale.US, kb)
            }
        }
}

/**
 * دالة ومساعد إدارة النسخ الاحتياطي والاستعادة الآمنة لقاعدة بيانات تطبيق دكاني
 * (Backup & Restore SAF Helper)
 */
object BackupRestoreHelper {

    const val DB_NAME = "dokkani_pos_database"
    private const val PREFS_NAME = "dokkani_backup_prefs"
    private const val KEY_LAST_BACKUP_DATE = "last_backup_date"
    private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"

    /**
     * جلب تفاصيل آخر نسخة احتياطية صادرة من هذا الجهاز
     */
    fun getLastBackupMetadata(context: Context): BackupMetadata? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateStr = prefs.getString(KEY_LAST_BACKUP_DATE, null) ?: return null
        val sizeBytes = prefs.getLong(KEY_LAST_BACKUP_SIZE, 0L)
        return BackupMetadata(dateStr, sizeBytes)
    }

    /**
     * حفظ تاريخ وحجم النسخة الاحتياطية
     */
    fun saveBackupMetadata(context: Context, sizeBytes: Long) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
        val dateStr = dateFormat.format(Date())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_BACKUP_DATE, dateStr)
            .putLong(KEY_LAST_BACKUP_SIZE, sizeBytes)
            .apply()
    }

    /**
     * صياغة اسم ملف النسخة الاحتياطية بختم زمني قياسي
     */
    fun generateBackupFileName(): String {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        return "dokkani_backup_$dateStamp.db"
    }

    /**
     * تصدير ونسخ قاعدة البيانات الحالية إلى URI المختار عبر SAF
     */
    fun exportDatabaseToUri(context: Context, destinationUri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return false

            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val size = dbFile.length()
            saveBackupMetadata(context, size)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * استعادة قاعدة البيانات بأمان من ملف URI محدد عبر SAF مع استبدال الملف وإغلاق الجلسة
     */
    fun restoreDatabaseFromUri(context: Context, sourceUri: Uri): Boolean {
        return try {
            // 1. إغلاق اتصال قاعدة بيانات Room الحالية تماماً لتجنب تلف الملفات
            DokkaniDatabase.closeDatabase()

            val targetDbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(targetDbFile.path + "-wal")
            val shmFile = File(targetDbFile.path + "-shm")

            // حظر وإزالة ملفات الجلسة الجانبية القديمة لـ WAL
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 2. نسخ الملف الجديد واستبدال قاعدة البيانات الحالية
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(targetDbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
