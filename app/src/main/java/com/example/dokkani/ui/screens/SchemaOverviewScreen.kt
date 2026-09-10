package com.example.dokkani.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.SystemSettingsEntity

data class TableSchemaInfo(
    val tableName: String,
    val titleArabic: String,
    val icon: ImageVector,
    val description: String,
    val keyColumns: List<String>,
    val foreignKeys: String,
    val purpose: String
)

@Composable
fun SchemaOverviewScreen(
    productsWithUnits: List<ProductWithUnits>,
    settings: SystemSettingsEntity?,
    currenciesCount: Int,
    partiesCount: Int,
    invoicesCount: Int,
    produceBatchesCount: Int,
    modifier: Modifier = Modifier
) {
    val tables = listOf(
        TableSchemaInfo(
            tableName = "products",
            titleArabic = "1. جدول الأصناف (Products)",
            icon = Icons.Default.Inventory,
            description = "السلع والمخزون الأساسي بالبقالة (اسم الصنف، الكود/SKU، القسم، بيع بالميزان، حد الطلب).",
            keyColumns = listOf("id (PK)", "code (Unique)", "name", "category", "isWeighted", "minStockAlert"),
            foreignKeys = "مرتبط مع جدول الوحدات المتعددة product_units",
            purpose = "تحديد هوية الصنف وما إذا كان يباع بالوزن كخضار أو بالقطعة كالمعلبات والألبان."
        ),
        TableSchemaInfo(
            tableName = "product_units",
            titleArabic = "2. جدول الوحدات المتعددة (Product_Units)",
            icon = Icons.Default.Category,
            description = "يدعم بيع وشراء الصنف بوحدات متعددة (حبة، كرتون، درزن، كيلو، سحارة) مع معامل التحويل.",
            keyColumns = listOf("id (PK)", "productId (FK)", "unitName", "conversionFactor", "barcode", "costPrice", "sellingPrice", "isBaseUnit"),
            foreignKeys = "FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE",
            purpose = "التحويل الرياضي بين وحدة الشراء (مثل كرتون = 24 حبة) ووحدة البيع الفردية مع ربط الباركود."
        ),
        TableSchemaInfo(
            tableName = "currencies",
            titleArabic = "3. جدول العملات وأسعار الصرف (Currencies)",
            icon = Icons.Default.CurrencyExchange,
            description = "إدارة العملة الأساسية (SAR) والعملات الأخرى مع أسعار الصرف الحالية.",
            keyColumns = listOf("id (PK)", "code (Unique)", "name", "symbol", "exchangeRateToBase", "isBaseCurrency", "isDefault"),
            foreignKeys = "تستخدم في فواتير المبيعات والمشتريات والحسابات",
            purpose = "تقييم التكلفة والأسعار بعملة المتجر الأساسية مع قبول العملات الأخرى."
        ),
        TableSchemaInfo(
            tableName = "parties",
            titleArabic = "4. جدول العملاء والموردين (Parties)",
            icon = Icons.Default.People,
            description = "إدارة حسابات دفتر البقالة (العملاء الآجلين) وموردي الألبان والخضار وحلقة الخضار.",
            keyColumns = listOf("id (PK)", "name", "type (CUSTOMER/SUPPLIER/BOTH)", "phone", "taxNumber", "currentBalance", "creditLimit"),
            foreignKeys = "مرتبط بفواتير البيع والشراء ودفعات سحاحير الخضار",
            purpose = "متابعة ديون عملاء الحي ومستحقات الموردين والرقم الضريبي."
        ),
        TableSchemaInfo(
            tableName = "invoices & invoice_items",
            titleArabic = "5. جداول الفواتير وبنودها (Invoices)",
            icon = Icons.Default.Receipt,
            description = "فواتير المبيعات ونقطة البيع (POS)، فواتير المشتريات، ومرتجعات البيع والشراء.",
            keyColumns = listOf("invoices: id, invoiceNumber, type, partyId, subtotal, taxAmount, total, paymentMethod", "invoice_items: id, invoiceId, productId, productUnitId, quantity, unitCostPrice, unitSellingPrice"),
            foreignKeys = "FOREIGN KEY (invoiceId) REFERENCES invoices(id) ON DELETE CASCADE",
            purpose = "تسجيل حركة البيع والشراء وتحديد التكلفة اللحظية لاحتساب مجمل الربح والضريبة."
        ),
        TableSchemaInfo(
            tableName = "mixed_produce_batches & yield_items",
            titleArabic = "6. جداول جرد خضار المشكل والفرز (Produce)",
            icon = Icons.Default.Eco,
            description = "النموذج المحاسبي لجرد سحاحير الخضار المشكل واحتساب الهدر وإعادة حساب تكلفة الصافي.",
            keyColumns = listOf("mixed_produce_batches: id, batchNumber, totalGrossWeightKg, totalPurchaseCost, wasteWeightKg, netSalableWeightKg, wastePercentage, effectiveCostPerKg", "yield_items: id, batchId, productId, sortedWeightKg, calculatedCostPerKg"),
            foreignKeys = "FOREIGN KEY (batchId) REFERENCES mixed_produce_batches(id) ON DELETE CASCADE",
            purpose = "معالجة التالف والهدر في الخضار وتحميل قيمته على الوزن الصافي الصالح للبيع."
        ),
        TableSchemaInfo(
            tableName = "stock_movements",
            titleArabic = "7. جدول حركات المخزون والطبقات (Stock)",
            icon = Icons.Default.Storage,
            description = "تتبع طبقات الشراء المخزنية لحساب المتوسط المرجح WAC والوارد أولاً صادر أولاً FIFO.",
            keyColumns = listOf("id (PK)", "productId (FK)", "movementType", "quantityBaseUnit", "remainingQuantityForFifo", "unitCostPriceBase", "timestamp"),
            foreignKeys = "FOREIGN KEY (productId) REFERENCES products(id) ON DELETE CASCADE",
            purpose = "العمود الفقري لمحرك التكلفة المحاسبي وتتبع رصيد كل شحنة بدقة."
        ),
        TableSchemaInfo(
            tableName = "system_settings",
            titleArabic = "8. جدول إعدادات النظام (System Settings)",
            icon = Icons.Default.Settings,
            description = "تخزين خيارات نظام دكاني، وفي مقدمتها طريقة تقييم التكلفة المعتمدة (WAC / FIFO / Last Purchase).",
            keyColumns = listOf("id (PK=1)", "storeName", "costValuationMethod", "defaultCurrencyCode", "defaultTaxRate", "enableProduceShrinkageTracking"),
            foreignKeys = "سجل وحيد للإعدادات العامة للنظام",
            purpose = "تحديد المعيار المحاسبي المعتمد لتسعير البضاعة المباعة وتقييم المخزون في المتجر."
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // كرت الترحيب والملخص العام
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F5132)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schema_header_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "دكاني (Dokkani POS & ERP)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "النواة والمخطط المحاسبي وقاعدة البيانات المحلية",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD1E7DD)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF198754),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Accounting",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // مؤشرات إحصائية سريعة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge(label = "الأصناف", count = "${productsWithUnits.size}")
                        StatBadge(label = "العملات", count = "$currenciesCount")
                        StatBadge(label = "الأطراف", count = "$partiesCount")
                        StatBadge(label = "الفواتير", count = "$invoicesCount")
                        StatBadge(label = "جرد الخضار", count = "$produceBatchesCount")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF003820),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF75B798),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "طريقة التقييم المعتمدة في النظام: ${settings?.costValuationMethod?.labelArabic ?: "المتوسط المرجح (WAC)"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD1E7DD)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "مخطط الجداول المحاسبية (SQLite Room Schema):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(tables) { table ->
            TableDetailCard(table = table)
        }
    }
}

@Composable
private fun StatBadge(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE8F5E9)
        )
    }
}

@Composable
private fun TableDetailCard(table: TableSchemaInfo) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("table_card_${table.tableName}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = table.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = table.titleArabic,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Table: ${table.tableName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = table.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // الحقول الرئيسية
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "الحقول والمفاتيح (Columns & Keys):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    for (col in table.keyColumns) {
                        Text(
                            text = "• $col",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الهدف المحاسبي
            Text(
                text = "الهدف المحاسبي: ${table.purpose}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
