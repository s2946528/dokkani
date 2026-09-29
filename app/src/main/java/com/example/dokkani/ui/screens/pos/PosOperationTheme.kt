package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.dokkani.domain.pos.PosOperation

/**
 * طاقم ألوان ديناميكي متكامل للعمليات الست المتميزة في شاشة الفواتير والسندات
 * يدعم الوضع النهاري والليلي (Dark Mode) باحترافية عالية مع ضمان التباين وراحة العين.
 */
data class OperationColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceContainer: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val borderAccent: Color,
    val badgeContainer: Color,
    val onBadgeContainer: Color
)

object PosOperationTheme {

    @Composable
    fun getColors(
        operation: PosOperation,
        isDark: Boolean = isSystemInDarkTheme()
    ): OperationColorScheme {
        return when (operation) {
            // 1. فاتورة بيع / بيع بالقيمة: طابع لوني أخضر (يعبر عن المبيعات والإيرادات)
            PosOperation.SALE -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFF66BB6A),
                    onPrimary = Color(0xFF003A08),
                    primaryContainer = Color(0xFF1B4D20),
                    onPrimaryContainer = Color(0xFFC8E6C9),
                    surfaceContainer = Color(0xFF102814),
                    headerGradientStart = Color(0xFF1B4D20),
                    headerGradientEnd = Color(0xFF0F3013),
                    borderAccent = Color(0xFF388E3C),
                    badgeContainer = Color(0xFF1B4D20),
                    onBadgeContainer = Color(0xFFA5D6A7)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFF1B5E20),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE8F5E9),
                    onPrimaryContainer = Color(0xFF1B5E20),
                    surfaceContainer = Color(0xFFF1F8E9),
                    headerGradientStart = Color(0xFF1B5E20),
                    headerGradientEnd = Color(0xFF2E7D32),
                    borderAccent = Color(0xFFA5D6A7),
                    badgeContainer = Color(0xFFE8F5E9),
                    onBadgeContainer = Color(0xFF1B5E20)
                )
            }

            // 2. فاتورة شراء: طابع لوني أزرق (يعبر عن التوريد والمشتريات)
            PosOperation.PURCHASE -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFF42A5F5),
                    onPrimary = Color(0xFF002952),
                    primaryContainer = Color(0xFF0D3C61),
                    onPrimaryContainer = Color(0xFFBBDEFB),
                    surfaceContainer = Color(0xFF0D2138),
                    headerGradientStart = Color(0xFF0D3C61),
                    headerGradientEnd = Color(0xFF0B2942),
                    borderAccent = Color(0xFF1976D2),
                    badgeContainer = Color(0xFF0D3C61),
                    onBadgeContainer = Color(0xFF90CAF9)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFF1565C0),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE3F2FD),
                    onPrimaryContainer = Color(0xFF1565C0),
                    surfaceContainer = Color(0xFFEBF5FB),
                    headerGradientStart = Color(0xFF1565C0),
                    headerGradientEnd = Color(0xFF1E88E5),
                    borderAccent = Color(0xFF90CAF9),
                    badgeContainer = Color(0xFFE3F2FD),
                    onBadgeContainer = Color(0xFF1565C0)
                )
            }

            // 3. مردود بيع: طابع لوني أحمر/وردي داكن (يعبر عن المرتجعات الواردة)
            PosOperation.SALE_RETURN -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFFEF5350),
                    onPrimary = Color(0xFF4A0002),
                    primaryContainer = Color(0xFF5C1D1E),
                    onPrimaryContainer = Color(0xFFFFCDD2),
                    surfaceContainer = Color(0xFF2E0F10),
                    headerGradientStart = Color(0xFF5C1D1E),
                    headerGradientEnd = Color(0xFF3D1011),
                    borderAccent = Color(0xFFC62828),
                    badgeContainer = Color(0xFF5C1D1E),
                    onBadgeContainer = Color(0xFFEF9A9A)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFFC62828),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFEBEE),
                    onPrimaryContainer = Color(0xFFC62828),
                    surfaceContainer = Color(0xFFFDF0F2),
                    headerGradientStart = Color(0xFFC62828),
                    headerGradientEnd = Color(0xFFD32F2F),
                    borderAccent = Color(0xFFEF9A9A),
                    badgeContainer = Color(0xFFFFEBEE),
                    onBadgeContainer = Color(0xFFC62828)
                )
            }

            // 4. مردود شراء: طابع لوني برتقالي/بني (يعبر عن المرتجعات الصادرة)
            PosOperation.PURCHASE_RETURN -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFFFF9800),
                    onPrimary = Color(0xFF421C00),
                    primaryContainer = Color(0xFF5E2E05),
                    onPrimaryContainer = Color(0xFFFFE0B2),
                    surfaceContainer = Color(0xFF2B1607),
                    headerGradientStart = Color(0xFF5E2E05),
                    headerGradientEnd = Color(0xFF3B1A02),
                    borderAccent = Color(0xFFE65100),
                    badgeContainer = Color(0xFF5E2E05),
                    onBadgeContainer = Color(0xFFFFCC80)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFFD84315),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFBE9E7),
                    onPrimaryContainer = Color(0xFFD84315),
                    surfaceContainer = Color(0xFFFFF3E0),
                    headerGradientStart = Color(0xFFD84315),
                    headerGradientEnd = Color(0xFFE65100),
                    borderAccent = Color(0xFFFFAB91),
                    badgeContainer = Color(0xFFFBE9E7),
                    onBadgeContainer = Color(0xFFD84315)
                )
            }

            // 5. سند قبض: طابع لوني تركواز/سماوي (يعبر عن المقبوضات النقدية)
            PosOperation.RECEIPT -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFF26C6DA),
                    onPrimary = Color(0xFF00363A),
                    primaryContainer = Color(0xFF004D40),
                    onPrimaryContainer = Color(0xFFB2EBF2),
                    surfaceContainer = Color(0xFF092429),
                    headerGradientStart = Color(0xFF004D40),
                    headerGradientEnd = Color(0xFF00363A),
                    borderAccent = Color(0xFF00838F),
                    badgeContainer = Color(0xFF004D40),
                    onBadgeContainer = Color(0xFF80DEEA)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFF00838F),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0F7FA),
                    onPrimaryContainer = Color(0xFF00838F),
                    surfaceContainer = Color(0xFFE0F2F1),
                    headerGradientStart = Color(0xFF00838F),
                    headerGradientEnd = Color(0xFF00ACC1),
                    borderAccent = Color(0xFF80DEEA),
                    badgeContainer = Color(0xFFE0F7FA),
                    onBadgeContainer = Color(0xFF00838F)
                )
            }

            // 6. سند صرف: طابع لوني بنفسجي/أرجواني (يعبر عن المدفوعات والمصروفات)
            PosOperation.EXPENSE -> if (isDark) {
                OperationColorScheme(
                    primary = Color(0xFFAB47BC),
                    onPrimary = Color(0xFF320040),
                    primaryContainer = Color(0xFF4A148C),
                    onPrimaryContainer = Color(0xFFE1BEE7),
                    surfaceContainer = Color(0xFF1E082A),
                    headerGradientStart = Color(0xFF4A148C),
                    headerGradientEnd = Color(0xFF2F0A5C),
                    borderAccent = Color(0xFF7B1FA2),
                    badgeContainer = Color(0xFF4A148C),
                    onBadgeContainer = Color(0xFFCE93D8)
                )
            } else {
                OperationColorScheme(
                    primary = Color(0xFF6A1B9A),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFF3E5F5),
                    onPrimaryContainer = Color(0xFF6A1B9A),
                    surfaceContainer = Color(0xFFFAF0FA),
                    headerGradientStart = Color(0xFF6A1B9A),
                    headerGradientEnd = Color(0xFF8E24AA),
                    borderAccent = Color(0xFFCE93D8),
                    badgeContainer = Color(0xFFF3E5F5),
                    onBadgeContainer = Color(0xFF6A1B9A)
                )
            }
        }
    }
}
