package com.example.dokkani.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dokkani.data.local.entities.ProductWithUnits

/**
 * خيارات فرز وترتيب الأصناف في شاشات المبيعات والمشتريات والمردودات
 */
enum class ProductSortOption(
    val title: String,
    val icon: ImageVector
) {
    POPULAR("الأكثر طلباً / مبيعاً", Icons.Default.Star),
    ALPHABETICAL("أبجدي (أ - ي)", Icons.Default.SortByAlpha),
    NEWEST("الأحدث إضافة", Icons.Default.NewReleases),
    QUANTITY("حسب الكمية المتوفرة", Icons.Default.Inventory2)
}

/**
 * دالة مساعدة لفرز قائمة الأصناف بناءً على الخيار المحدد وخريطة المخزون
 */
fun List<ProductWithUnits>.sortProducts(
    sortOption: ProductSortOption,
    stockMap: Map<Long, Double> = emptyMap()
): List<ProductWithUnits> {
    return when (sortOption) {
        ProductSortOption.POPULAR -> this.sortedByDescending { it.product.id }
        ProductSortOption.ALPHABETICAL -> this.sortedBy { it.product.name.trim() }
        ProductSortOption.NEWEST -> this.sortedByDescending { it.product.createdAt }
        ProductSortOption.QUANTITY -> this.sortedByDescending { stockMap[it.product.id] ?: 0.0 }
    }
}
