package com.example.dokkani.ui.screens.assets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.OwnerTransactionType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.domain.assets.AssetCategories
import com.example.dokkani.domain.assets.EquityCalculationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsAndEquityScreen(
    equityResult: EquityCalculationResult?,
    fixedAssets: List<FixedAssetEntity>,
    leaseholdRights: List<LeaseholdRightEntity> = emptyList(),
    ownerTransactions: List<OwnerTransactionEntity>,
    products: List<ProductWithUnits>,
    subTab: Int,
    showAddAssetDialog: Boolean,
    showOwnerTransDialog: Boolean,
    ownerTransType: OwnerTransactionType,
    assetCodeInput: String,
    assetNameInput: String,
    assetCategoryInput: String,
    assetCostInput: String,
    assetSupplierInput: String,
    assetNotesInput: String,
    assetPaymentMethod: PaymentMethod,
    ownerTransAmountInput: String,
    ownerTransProductId: Long?,
    ownerTransQuantityInput: String,
    ownerTransDetailsInput: String,
    ownerTransPaymentMethod: PaymentMethod,
    // Leasehold state
    showAddLeaseholdDialog: Boolean = false,
    showAmortizeLeaseholdDialog: Boolean = false,
    showSellLeaseholdDialog: Boolean = false,
    selectedLeaseholdItem: LeaseholdRightEntity? = null,
    leaseholdCodeInput: String = "",
    leaseholdNameInput: String = "",
    leaseholdCostInput: String = "",
    leaseholdYearsInput: String = "5",
    leaseholdNotesInput: String = "",
    leaseholdAmortizeAmountInput: String = "",
    leaseholdSellPriceInput: String = "",
    leaseholdSellPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    onSelectSubTab: (Int) -> Unit,
    onOpenAddAssetDialog: () -> Unit,
    onDismissAddAssetDialog: () -> Unit,
    onAssetInputsChanged: (String, String, String, String, String, String, PaymentMethod) -> Unit,
    onSubmitAddAsset: () -> Unit,
    onDeleteAsset: (Long) -> Unit,
    onOpenOwnerTransDialog: (OwnerTransactionType) -> Unit,
    onDismissOwnerTransDialog: () -> Unit,
    onOwnerTransInputsChanged: (OwnerTransactionType, String, Long?, String, String, PaymentMethod) -> Unit,
    onSubmitOwnerTrans: () -> Unit,
    onDeleteOwnerTrans: (Long) -> Unit,
    // Leasehold handlers
    onOpenAddLeaseholdDialog: () -> Unit = {},
    onDismissAddLeaseholdDialog: () -> Unit = {},
    onLeaseholdInputsChanged: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onSubmitAddLeasehold: () -> Unit = {},
    onOpenAmortizeLeaseholdDialog: (LeaseholdRightEntity) -> Unit = {},
    onDismissAmortizeLeaseholdDialog: () -> Unit = {},
    onAmortizeAmountChanged: (String) -> Unit = {},
    onSubmitAmortizeLeasehold: () -> Unit = {},
    onOpenSellLeaseholdDialog: (LeaseholdRightEntity) -> Unit = {},
    onDismissSellLeaseholdDialog: () -> Unit = {},
    onSellLeaseholdInputsChanged: (String, PaymentMethod) -> Unit = { _, _ -> },
    onSubmitSellLeasehold: () -> Unit = {},
    onDeleteLeasehold: (Long) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // العنوان العلوي
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأصول وحقوق الملكية ورأس المال",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "دليل الأصول الثابتة، الخلو ونقل القدم، الحساب الآلي لرأس المال، ومسحوبات وإيداعات المالك",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // تبويبات الشاشة
        PrimaryTabRow(selectedTabIndex = subTab) {
            Tab(
                selected = subTab == 0,
                onClick = { onSelectSubTab(0) },
                text = { Text("رأس المال والملكية", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { onSelectSubTab(1) },
                text = { Text("الأصول الثابتة (${fixedAssets.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Store, contentDescription = null) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { onSelectSubTab(2) },
                text = { Text("نقل القدم/الخلو (${leaseholdRights.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.CorporateFare, contentDescription = null) }
            )
            Tab(
                selected = subTab == 3,
                onClick = { onSelectSubTab(3) },
                text = { Text("حركات الملكية (${ownerTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (subTab) {
            0 -> CapitalAndEquityTabContent(
                equityResult = equityResult,
                onOpenOwnerTransDialog = onOpenOwnerTransDialog
            )
            1 -> FixedAssetsTabContent(
                fixedAssets = fixedAssets,
                dateFormat = dateFormat,
                onOpenAddAssetDialog = onOpenAddAssetDialog,
                onDeleteAsset = onDeleteAsset
            )
            2 -> LeaseholdRightsTabContent(
                leaseholdRights = leaseholdRights,
                dateFormat = dateFormat,
                onOpenAddLeaseholdDialog = onOpenAddLeaseholdDialog,
                onOpenAmortizeLeaseholdDialog = onOpenAmortizeLeaseholdDialog,
                onOpenSellLeaseholdDialog = onOpenSellLeaseholdDialog,
                onDeleteLeasehold = onDeleteLeasehold
            )
            3 -> OwnerTransactionsTabContent(
                ownerTransactions = ownerTransactions,
                dateFormat = dateFormat,
                onOpenOwnerTransDialog = onOpenOwnerTransDialog,
                onDeleteOwnerTrans = onDeleteOwnerTrans
            )
        }
    }

    // نصوص الحوار المفتوحة
    if (showAddAssetDialog) {
        AddAssetDialog(
            assetCodeInput = assetCodeInput,
            assetNameInput = assetNameInput,
            assetCategoryInput = assetCategoryInput,
            assetCostInput = assetCostInput,
            assetSupplierInput = assetSupplierInput,
            assetNotesInput = assetNotesInput,
            assetPaymentMethod = assetPaymentMethod,
            onInputsChanged = onAssetInputsChanged,
            onDismiss = onDismissAddAssetDialog,
            onSubmit = onSubmitAddAsset
        )
    }

    if (showOwnerTransDialog) {
        AddOwnerTransactionDialog(
            type = ownerTransType,
            products = products,
            amountInput = ownerTransAmountInput,
            productIdInput = ownerTransProductId,
            quantityInput = ownerTransQuantityInput,
            detailsInput = ownerTransDetailsInput,
            paymentMethodInput = ownerTransPaymentMethod,
            onInputsChanged = onOwnerTransInputsChanged,
            onDismiss = onDismissOwnerTransDialog,
            onSubmit = onSubmitOwnerTrans
        )
    }

    if (showAddLeaseholdDialog) {
        AddLeaseholdDialog(
            code = leaseholdCodeInput,
            name = leaseholdNameInput,
            cost = leaseholdCostInput,
            years = leaseholdYearsInput,
            notes = leaseholdNotesInput,
            onInputsChanged = onLeaseholdInputsChanged,
            onDismiss = onDismissAddLeaseholdDialog,
            onSubmit = onSubmitAddLeasehold
        )
    }

    if (showAmortizeLeaseholdDialog && selectedLeaseholdItem != null) {
        AmortizeLeaseholdDialog(
            item = selectedLeaseholdItem,
            amortizeAmountInput = leaseholdAmortizeAmountInput,
            onAmortizeAmountChanged = onAmortizeAmountChanged,
            onDismiss = onDismissAmortizeLeaseholdDialog,
            onSubmit = onSubmitAmortizeLeasehold
        )
    }

    if (showSellLeaseholdDialog && selectedLeaseholdItem != null) {
        SellLeaseholdDialog(
            item = selectedLeaseholdItem,
            sellPriceInput = leaseholdSellPriceInput,
            paymentMethod = leaseholdSellPaymentMethod,
            onInputsChanged = onSellLeaseholdInputsChanged,
            onDismiss = onDismissSellLeaseholdDialog,
            onSubmit = onSubmitSellLeasehold
        )
    }
}

@Composable
private fun CapitalAndEquityTabContent(
    equityResult: EquityCalculationResult?,
    onOpenOwnerTransDialog: (OwnerTransactionType) -> Unit
) {
    val eq = equityResult ?: EquityCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // بطاقات KPI رئيسية
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "رأس المال الافتتاحي الآلي",
                    value = "${"%.2f".format(eq.calculatedInitialCapital)} ر.س",
                    subtitle = "(نقدية + بضاعة + ديون) - التزامات",
                    color = Color(0xFF1E3A8A),
                    icon = Icons.Default.Calculate
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "صافي حقوق الملكية الإجمالي",
                    value = "${"%.2f".format(eq.netTotalEquity)} ر.س",
                    subtitle = "رأس المال + أصول + أرباح - مسحوبات",
                    color = Color(0xFF15803D),
                    icon = Icons.Default.AccountBalance
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "إجمالي الأصول الثابتة",
                    value = "${"%.2f".format(eq.totalFixedAssetsValue)} ر.س",
                    subtitle = "ثلاجات، أرفف، وموازين",
                    color = Color(0xFF0369A1),
                    icon = Icons.Default.Store
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "نقل القدم / خلو المحل",
                    value = "${"%.2f".format(eq.totalLeaseholdGoodwillValue)} ر.س",
                    subtitle = "أصل غير ملموس تأسيسي",
                    color = Color(0xFF7C3AED),
                    icon = Icons.Default.CorporateFare
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "مسحوبات المالك الشخصية",
                    value = "${"%.2f".format(eq.totalOwnerDrawings)} ر.س",
                    subtitle = "نقدية وبضاعة بسعر التكلفة",
                    color = Color(0xFFB91C1C),
                    icon = Icons.Default.MoneyOff
                )
            }
        }

        // بطاقة المعالجة الآلية لرأس المال الافتتاحي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF1E3A8A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تفصيل المعالجة الآلية لرأس المال الافتتاحي للبقالة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    EquationRow(
                        label = "(+) نقدية الصندوق والدرج في البداية",
                        amount = eq.cashInHandAndDrawer,
                        isPositive = true
                    )
                    EquationRow(
                        label = "(+) أرصدة البنوك ومقبوضات شبكة مدى",
                        amount = eq.bankAndMadaBalances,
                        isPositive = true
                    )
                    EquationRow(
                        label = "(+) تقييم بضاعة أول المدة بسعر التكلفة",
                        amount = eq.inventoryValuationAtCost,
                        isPositive = true
                    )
                    EquationRow(
                        label = "(+) نقل القدم / خلو المحل (أصل تأسيسي غير ملموس)",
                        amount = eq.totalLeaseholdGoodwillValue,
                        isPositive = true
                    )
                    EquationRow(
                        label = "(+) ديون العملاء والمستحقات (الأرصدة المدينة)",
                        amount = eq.customerReceivables,
                        isPositive = true
                    )
                    EquationRow(
                        label = "(-) ديون الموردين والالتزامات (الأرصدة الدائنة)",
                        amount = eq.supplierPayables,
                        isPositive = false
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFCBD5E1))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "(=) رأس المال الافتتاحي الآلي المحسوب:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "${"%.2f".format(eq.calculatedInitialCapital)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }
            }
        }

        // أزرار الحركات السريعة لحقوق الملكية
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "إجراءات وحركات حقوق الملكية والمالك:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenOwnerTransDialog(OwnerTransactionType.CASH_DRAWING) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسحوبات نقدية", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onOpenOwnerTransDialog(OwnerTransactionType.GOODS_DRAWING) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سحب بضاعة بالتكلفة", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onOpenOwnerTransDialog(OwnerTransactionType.CAPITAL_DEPOSIT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إيداع رأس مال", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedAssetsTabContent(
    fixedAssets: List<FixedAssetEntity>,
    dateFormat: SimpleDateFormat,
    onOpenAddAssetDialog: () -> Unit,
    onDeleteAsset: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل الأصول الثابتة للبقالة (ثلاجات، أرفف، موازين، سيارات)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )

            Button(
                onClick = onOpenAddAssetDialog,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("شراء/إضافة أصل ثابت", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (fixedAssets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أصول ثابتة مسجلة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(fixedAssets, key = { it.id }) { asset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = asset.assetCode,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = asset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التصنيف: ${asset.category} | المورد: ${asset.supplierName.ifEmpty { "غير محدد" }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "تاريخ الشراء: ${dateFormat.format(Date(asset.purchaseDate))} | طريق السداد: ${asset.paymentMethod.name}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                if (asset.notes.isNotEmpty()) {
                                    Text(
                                        text = "ملاحظات: ${asset.notes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${"%.2f".format(asset.currentValue)} ر.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "التكلفة الأصلي: ${"%.2f".format(asset.purchaseCost)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                IconButton(onClick = { onDeleteAsset(asset.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الأصل", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaseholdRightsTabContent(
    leaseholdRights: List<LeaseholdRightEntity>,
    dateFormat: SimpleDateFormat,
    onOpenAddLeaseholdDialog: () -> Unit,
    onOpenAmortizeLeaseholdDialog: (LeaseholdRightEntity) -> Unit,
    onOpenSellLeaseholdDialog: (LeaseholdRightEntity) -> Unit,
    onDeleteLeasehold: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل نقل القدم والخلو (الأصول غير الملموسة)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )

            Button(
                onClick = onOpenAddLeaseholdDialog,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة خلو / نقل قدم", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (leaseholdRights.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد حقوق نقل قدم أو خلو مسجلة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(leaseholdRights, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF3E8FF)
                                    ) {
                                        Text(
                                            text = item.code,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التكلفة الأصلية: ${"%.2f".format(item.initialCost)} ر.س | سنوات الإطفاء: ${item.amortizationYears} سنوات",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "تاريخ التسجيل: ${dateFormat.format(Date(item.createdDate))}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                if (item.notes.isNotEmpty()) {
                                    Text(
                                        text = "ملاحظات: ${item.notes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${"%.2f".format(item.currentValue)} ر.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF7C3AED)
                                )
                                Text(
                                    text = "إطفاء متراكم: ${"%.2f".format(item.accumulatedAmortization)} ر.س",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row {
                                    IconButton(onClick = { onOpenAmortizeLeaseholdDialog(item) }) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = "إطفاء أصل", tint = Color(0xFFD97706))
                                    }
                                    IconButton(onClick = { onOpenSellLeaseholdDialog(item) }) {
                                        Icon(Icons.Default.Sell, contentDescription = "بيع خلو", tint = Color(0xFF16A34A))
                                    }
                                    IconButton(onClick = { onDeleteLeasehold(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الخلو", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerTransactionsTabContent(
    ownerTransactions: List<OwnerTransactionEntity>,
    dateFormat: SimpleDateFormat,
    onOpenOwnerTransDialog: (OwnerTransactionType) -> Unit,
    onDeleteOwnerTrans: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل حركات حقوق الملكية والمسحوبات الشخصية للمالك",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onOpenOwnerTransDialog(OwnerTransactionType.CASH_DRAWING) }) {
                    Text("مسحوبات نقدية", fontSize = 11.sp)
                }
                Button(
                    onClick = { onOpenOwnerTransDialog(OwnerTransactionType.GOODS_DRAWING) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("سحب بضاعة بالتكلفة", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (ownerTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد حركات مسجلة لمالك البقالة.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ownerTransactions, key = { it.id }) { trans ->
                    val badgeColor = when (trans.type) {
                        OwnerTransactionType.CASH_DRAWING -> Color(0xFFDC2626)
                        OwnerTransactionType.GOODS_DRAWING -> Color(0xFFD97706)
                        OwnerTransactionType.CAPITAL_DEPOSIT -> Color(0xFF16A34A)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = trans.type.labelArabic,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trans.transactionNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التاريخ: ${dateFormat.format(Date(trans.createdDate))} | طريقة السداد: ${trans.paymentMethod.name}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (trans.details.isNotEmpty()) {
                                    Text(
                                        text = "التفاصيل: ${trans.details}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${"%.2f".format(trans.amount)} ر.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = badgeColor
                                )

                                IconButton(onClick = { onDeleteOwnerTrans(trans.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الحركة", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------- المكونات المساعدة (Helper Components) -------------------

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun EquationRow(
    label: String,
    amount: Double,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF475569))
        Text(
            text = "${if (isPositive) "+" else "-"} ${"%.2f".format(amount)} ر.س",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
        )
    }
}

// ------------------- نوافذ الحوار (Dialog Components) -------------------

@Composable
private fun AddAssetDialog(
    assetCodeInput: String,
    assetNameInput: String,
    assetCategoryInput: String,
    assetCostInput: String,
    assetSupplierInput: String,
    assetNotesInput: String,
    assetPaymentMethod: PaymentMethod,
    onInputsChanged: (String, String, String, String, String, String, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة أصل ثابت جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = assetCodeInput,
                    onValueChange = { onInputsChanged(it, assetNameInput, assetCategoryInput, assetCostInput, assetSupplierInput, assetNotesInput, assetPaymentMethod) },
                    label = { Text("كود الأصل (مثلاً AST-001)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetNameInput,
                    onValueChange = { onInputsChanged(assetCodeInput, it, assetCategoryInput, assetCostInput, assetSupplierInput, assetNotesInput, assetPaymentMethod) },
                    label = { Text("اسم الأصل (مثلاً: ثلاجة عرض)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetCategoryInput,
                    onValueChange = { onInputsChanged(assetCodeInput, assetNameInput, it, assetCostInput, assetSupplierInput, assetNotesInput, assetPaymentMethod) },
                    label = { Text("التصنيف") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetCostInput,
                    onValueChange = { onInputsChanged(assetCodeInput, assetNameInput, assetCategoryInput, it, assetSupplierInput, assetNotesInput, assetPaymentMethod) },
                    label = { Text("تكلفة الشراء") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetSupplierInput,
                    onValueChange = { onInputsChanged(assetCodeInput, assetNameInput, assetCategoryInput, assetCostInput, it, assetNotesInput, assetPaymentMethod) },
                    label = { Text("المورد") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assetNotesInput,
                    onValueChange = { onInputsChanged(assetCodeInput, assetNameInput, assetCategoryInput, assetCostInput, assetSupplierInput, it, assetPaymentMethod) },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("حفظ الأصل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddOwnerTransactionDialog(
    type: OwnerTransactionType,
    products: List<ProductWithUnits>,
    amountInput: String,
    productIdInput: Long?,
    quantityInput: String,
    detailsInput: String,
    paymentMethodInput: PaymentMethod,
    onInputsChanged: (OwnerTransactionType, String, Long?, String, String, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(type.labelArabic) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (type == OwnerTransactionType.GOODS_DRAWING) {
                    Text("سحب بضاعة يتم بسعر التكلفة لتحديد المسحوبات بدقة.", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { onInputsChanged(type, amountInput, productIdInput, it, detailsInput, paymentMethodInput) },
                        label = { Text("الكمية المسحوبة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { onInputsChanged(type, it, productIdInput, quantityInput, detailsInput, paymentMethodInput) },
                    label = { Text("المبلغ الإجمالي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detailsInput,
                    onValueChange = { onInputsChanged(type, amountInput, productIdInput, quantityInput, it, paymentMethodInput) },
                    label = { Text("التفاصيل / السبب") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("حفظ الحركة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AddLeaseholdDialog(
    code: String,
    name: String,
    cost: String,
    years: String,
    notes: String,
    onInputsChanged: (String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة خلو / نقل قدم جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { onInputsChanged(it, name, cost, years, notes) },
                    label = { Text("الكود (مثلاً LSH-001)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { onInputsChanged(code, it, cost, years, notes) },
                    label = { Text("الاسم / البيان") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { onInputsChanged(code, name, it, years, notes) },
                    label = { Text("التكلفة المدفوعة (الخلو)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = years,
                    onValueChange = { onInputsChanged(code, name, cost, it, notes) },
                    label = { Text("عدد سنوات الإطفاء") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { onInputsChanged(code, name, cost, years, it) },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("حفظ الخلو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun AmortizeLeaseholdDialog(
    item: LeaseholdRightEntity,
    amortizeAmountInput: String,
    onAmortizeAmountChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل إطفاء خلو المحل (${item.name})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("القيمة الحالية: ${"%.2f".format(item.currentValue)} ر.س", fontSize = 13.sp)
                OutlinedTextField(
                    value = amortizeAmountInput,
                    onValueChange = onAmortizeAmountChanged,
                    label = { Text("مبلغ الإطفاء") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("إثبات الإطفاء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun SellLeaseholdDialog(
    item: LeaseholdRightEntity,
    sellPriceInput: String,
    paymentMethod: PaymentMethod,
    onInputsChanged: (String, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بيع / التنازل عن الخلو (${item.name})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("القيمة الدفترية المسجلة: ${"%.2f".format(item.currentValue)} ر.س", fontSize = 13.sp)
                OutlinedTextField(
                    value = sellPriceInput,
                    onValueChange = { onInputsChanged(it, paymentMethod) },
                    label = { Text("سعر البيع المقبوض") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("تأكيد البيع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
