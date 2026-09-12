package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CartItem(
    val id: Long,
    val name: String,
    val price: Double,
    var quantity: Int
)

data class TransactionRecord(
    val id: String,
    val partyName: String,
    val date: String,
    val amount: Double,
    val status: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        val cartItems = remember { mutableStateListOf<CartItem>() }
        var activeOperation by remember { mutableStateOf("SALE") } 
        var cashierTotalSales by remember { mutableStateOf(125000.0) }
        var showHistoryDialog by remember { mutableStateOf(false) }

        val sampleProducts = remember {
            listOf(
                CartItem(1, "طماطم كيلو", 500.0, 1),
                CartItem(2, "بطاطس كيلو", 400.0, 1),
                CartItem(3, "بصل كيلو", 350.0, 1),
                CartItem(4, "خيار كيلو", 600.0, 1),
                CartItem(5, "تفاح كرتون", 4500.0, 1),
                CartItem(6, "موز طبق", 1200.0, 1)
            )
        }

        val sampleRecords = remember {
            listOf(
                TransactionRecord("INV-2026-001", "أبو أحمد (عميل)", "2026-09-12 14:30", 4500.0, "مكتملة", "SALE"),
                TransactionRecord("INV-2026-002", "عميل نقدي", "2026-09-12 15:10", 1200.0, "مكتملة", "SALE"),
                TransactionRecord("PUR-2026-010", "مورد خضار سوق العزيزية", "2026-09-12 06:00", 35000.0, "مستلمة", "PURCHASE"),
                TransactionRecord("PUR-2026-011", "شركة المراعي", "2026-09-11 10:00", 18500.0, "مستلمة", "PURCHASE"),
                TransactionRecord("RET-S-001", "أبو أحمد", "2026-09-12 16:00", 500.0, "مرتجع", "SALE_RETURN"),
                TransactionRecord("RET-P-001", "مورد خضار سوق العزيزية", "2026-09-11 12:00", 1200.0, "مرتجع", "PURCHASE_RETURN"),
                TransactionRecord("RCV-2026-005", "أبو أحمد (دفتر الحساب)", "2026-09-12 11:20", 5000.0, "مقبوض", "RECEIPT"),
                TransactionRecord("EXP-2026-002", "مصاريف كهرباء ونظافة", "2026-09-10 09:00", 2500.0, "مصروف", "EXPENSE")
            )
        }

        val cartTotal = cartItems.sumOf { it.price * it.quantity }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F8))
                .padding(8.dp)
        ) {
            // 1. كرت إجمالي مبيعات الكاشير العلوي
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي مبيعات الشفت الحالي", color = Color(0xFFC8E6C9), fontSize = 11.sp)
                        Text("$cashierTotalSales ر.ي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعراض السجلات", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // 2. شريط الأزرار السريعة
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeOperation == "SALE",
                        onClick = { activeOperation = "SALE" },
                        label = { Text("فاتورة بيع") },
                        leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeOperation == "PURCHASE",
                        onClick = { activeOperation = "PURCHASE" },
                        label = { Text("فاتورة شراء") },
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeOperation == "SALE_RETURN",
                        onClick = { activeOperation = "SALE_RETURN" },
                        label = { Text("مردود بيع") },
                        leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeOperation == "PURCHASE_RETURN",
                        onClick = { activeOperation = "PURCHASE_RETURN" },
                        label = { Text("مردود شراء") },
                        leadingIcon = { Icon(Icons.Default.RemoveShoppingCart, contentDescription = null) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeOperation == "RECEIPT",
                        onClick = { activeOperation = "RECEIPT" },
                        label = { Text("سند قبض") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeOperation == "EXPENSE",
                        onClick = { activeOperation = "EXPENSE" },
                        label = { Text("سند صرف") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                    )
                }
            }

            // 3. جسم الشاشة الرئيسي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // قسم الأصناف والمنتجات - تم تغييره إلى صنفين في كل سطر (weight 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = when (activeOperation) {
                            "PURCHASE" -> "أصناف المشتريات والتوريد"
                            "SALE_RETURN" -> "اختيار الأصناف المراد إرجاعها"
                            "PURCHASE_RETURN" -> "إرجاع بضاعة للمورد"
                            else -> "قائمة الأصناف والمنتجات"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), // صنفين فقط في كل سطر
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sampleProducts) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(105.dp) // زيادة الارتفاع لبطاقة المنتج
                                    .clickable {
                                        val existing = cartItems.find { it.id == product.id }
                                        if (existing != null) {
                                            val index = cartItems.indexOf(existing)
                                            cartItems[index] = existing.copy(quantity = existing.quantity + 1)
                                        } else {
                                            cartItems.add(product.copy(quantity = 1))
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${product.price} ر.ي",
                                        color = Color(0xFF1B5E20),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // قسم السلة المكسوة والموسعة (weight 1.1f لإعطائها عرضًا أوسع ومساحة أكبر)
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeOperation) {
                                    "PURCHASE" -> "فاتورة شراء جديدة"
                                    "SALE_RETURN" -> "سند مرتجع مبيعات"
                                    "PURCHASE_RETURN" -> "سند مرتجع مشتريات"
                                    "RECEIPT" -> "سند قبض نقدية"
                                    "EXPENSE" -> "سند صرف نقدية"
                                    else -> "سلة الفاتورة الحالية"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (cartItems.isNotEmpty()) {
                                TextButton(onClick = { cartItems.clear() }) {
                                    Text("تفريع السلة", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        // قائمة عناصر السلة الموسعة
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cartItems) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0F4F8))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${item.price} × ${item.quantity} = ${item.price * item.quantity} ر.ي", fontSize = 12.sp, color = Color.DarkGray)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val index = cartItems.indexOf(item)
                                                if (item.quantity > 1) {
                                                    cartItems[index] = item.copy(quantity = item.quantity - 1)
                                                } else {
                                                    cartItems.removeAt(index)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray)
                                        }

                                        Text(
                                            text = "${item.quantity}",
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(
                                            onClick = {
                                                val index = cartItems.indexOf(item)
                                                cartItems[index] = item.copy(quantity = item.quantity + 1)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF1B5E20))
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المبلغ الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("$cartTotal ر.ي", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (cartItems.isNotEmpty()) {
                                    if (activeOperation == "SALE") cashierTotalSales += cartTotal
                                    cartItems.clear()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (activeOperation) {
                                    "PURCHASE" -> Color(0xFF1976D2)
                                    "SALE_RETURN", "PURCHASE_RETURN" -> Color(0xFFD32F2F)
                                    "EXPENSE" -> Color(0xFFE65100)
                                    else -> Color(0xFF1B5E20)
                                }
                            ),
                            enabled = cartItems.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (activeOperation) {
                                    "PURCHASE" -> "حفظ فاتورة الشراء"
                                    "SALE_RETURN" -> "حفظ مرتجع البيع"
                                    "PURCHASE_RETURN" -> "حفظ مرتجع الشراء"
                                    "RECEIPT" -> "حفظ سند القبض"
                                    "EXPENSE" -> "حفظ سند الصرف"
                                    else -> "إتمام عملية البيع"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. نافذة استعراض الفواتير والسندات
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (activeOperation) {
                                "PURCHASE" -> "سجل فواتير الشراء"
                                "SALE_RETURN" -> "سجل مردودات المبيعات"
                                "PURCHASE_RETURN" -> "سجل مردودات المشتريات"
                                "RECEIPT" -> "سجل سندات القبض"
                                "EXPENSE" -> "سجل سندات الصرف"
                                else -> "سجل فواتير البيع"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showHistoryDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                },
                text = {
                    val filteredRecords = sampleRecords.filter { it.type == activeOperation }

                    if (filteredRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد سجلات حالية لهذه العملية", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredRecords) { record ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(record.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(record.partyName, fontSize = 11.sp, color = Color.Gray)
                                            Text(record.date, fontSize = 10.sp, color = Color.Gray)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${record.amount} ر.ي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                            Surface(
                                                color = Color(0xFFE8F5E9),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(record.status, color = Color(0xFF2E7D32), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("تم")
                    }
                }
            )
        }
    }
}
