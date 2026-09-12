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
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen() {
    // أجبر الواجهة على الاتجاه من اليمين لليسار (RTL) للغة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        val cartItems = remember { mutableStateListOf<CartItem>() }
        var activeOperation by remember { mutableStateOf("SALE") } // SALE, PURCHASE, RETURN, RECEIPT, EXPENSE
        var cashierTotalSales by remember { mutableStateOf(125000.0) } // إجمالي إحصائي يزداد مع كل عملية

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

        val cartTotal = cartItems.sumOf { it.price * it.quantity }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F8))
                .padding(8.dp)
        ) {
            // 1. كرت إجمالي مبيعات الكاشير العلوي (تفاعلي)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي مبيعات الشفت الحالي",
                            color = Color(0xFFC8E6C9),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${cashierTotalSales} ر.ي",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الكاشير: أحمد", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. شريط الأزرار السريعة للتنقل بين العمليات
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        selected = activeOperation == "RETURN",
                        onClick = { activeOperation = "RETURN" },
                        label = { Text("مردودات") },
                        leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) }
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

            // 3. جسم الشاشة الرئيسي (الأصناف على اليمين + السلة على اليسار)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // قسم عرض أصناف المنتجات (أخذ المساحة الأكبر 60%)
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "قائمة الأصناف والمنتجات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sampleProducts) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
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
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${product.price} ر.ي",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // قسم سلة المشتريات والمدفوعات (مساحة مناسبة وواضحة 40%)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("السلة الحالية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (cartItems.isNotEmpty()) {
                                TextButton(onClick = { cartItems.clear() }) {
                                    Text("تفرغ", color = Color.Red, fontSize = 11.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // قائمة عناصر السلة
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(cartItems) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF0F4F8))
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                        Text("${item.price} × ${item.quantity} = ${item.price * item.quantity}", fontSize = 11.sp)
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
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray)
                                        }

                                        Text("${item.quantity}", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                        IconButton(
                                            onClick = {
                                                val index = cartItems.indexOf(item)
                                                cartItems[index] = item.copy(quantity = item.quantity + 1)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF1B5E20))
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        // تفاصيل الحساب والإجمالي
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الإجمالي الكلي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$cartTotal ر.ي", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (cartItems.isNotEmpty()) {
                                    cashierTotalSales += cartTotal
                                    cartItems.clear()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            enabled = cartItems.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إتمام وتنفيذ العملية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
