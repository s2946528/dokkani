package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// كلاسات مؤقتة لعرض واجهة البيع (يمكنك ربطها بـ PosViewModel لاحقاً)
data class CartItem(
    val id: Long,
    val name: String,
    val price: Double,
    var quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen() {
    // حالة سلة المشتريات (مؤقتة للتجربة والتحقق من الواجهة)
    val cartItems = remember { mutableStateListOf<CartItem>() }
    
    // قائمة منتجات تجريبية لرؤية التصميم فوراً
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

    val totalAmount = cartItems.sumOf { it.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نقطة البيع (POS) - دكاني", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xF5F5F5))
        ) {
            // 1. قسم عرض المنتجات (يمتد على 60% من الشاشة)
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Text(
                    text = "الأصناف والمنتجات",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sampleProducts) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable {
                                    val existing = cartItems.find { it.id == product.id }
                                    if (existing != null) {
                                        val index = cartItems.indexOf(existing)
                                        cartItems[index] = existing.copy(quantity = existing.quantity + 1)
                                    } else {
                                        cartItems.add(product.copy(quantity = 1))
                                    }
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${product.price} ر.ي",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 2. قسم سلة الفاتورة الحالية (يمتد على 40% من الشاشة)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "سلة الفاتورة الحالية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Divider()

                    // قائمة عناصر السلة
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0F0F0))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("${item.price} × ${item.quantity} = ${item.price * item.quantity} ر.ي", fontSize = 12.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1) {
                                                val index = cartItems.indexOf(item)
                                                cartItems[index] = item.copy(quantity = item.quantity - 1)
                                            } else {
                                                cartItems.remove(item)
                                            }
                                        }
                                    ) {
                                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            cartItems.remove(item)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    // ملخص الفاتورة والدفع
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("$totalAmount ر.ي", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            // تنفيذ عملية إتمام البيع وتنظيف السلة
                            cartItems.clear()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = cartItems.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إتمام الفاتورة (طباعة / حفظ)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
