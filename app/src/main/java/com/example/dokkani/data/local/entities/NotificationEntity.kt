package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
/ أنواع الإشعارات لنظام دكاني
 */
enum class NotificationType(val titleArabic: String) {
    ALERT("تنبيه عام"),
    LOW_STOCK("انخفاض المخزون"),
    DEBT_DUE("تنبيه استحقاق ديون"),
    SHIFT_CLOSE("إغلاق الشفت والصندوق"),
    SYNC("المزامنة والنسخ الاحتياطي")
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.ALERT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val payloadData: String? = null
)
