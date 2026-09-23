package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceWithDetails
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.screens.crud.AddEditCurrencyDialog
import com.example.dokkani.ui.screens.crud.AddEditPartyDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemSettingsScreen(
    settings: SystemSettingsEntity? = null,
    currencies: List<CurrencyEntity> = emptyList(),
    parties: List<PartyEntity> = emptyList(),
    invoices: List<InvoiceEntity> = emptyList(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onUpdateValuationMethod: (CostValuationMethod) -> Unit = {},
    onUpdateEnableNegativeStock: (Boolean) -> Unit = {},
    onUpdateTaxSettings: (Boolean, Double) -> Unit = { _, _ -> },
    onUpdatePurchaseTaxSettings: (Boolean, Double) -> Unit = { _, _ -> },
    onSaveCurrency: (CurrencyEntity) -> Unit = {},
    onSetBaseCurrency: (Long) -> Unit = {},
    onDeleteCurrency: (CurrencyEntity) -> Unit = {},
    onSaveParty: (PartyEntity) -> Unit = {},
    onDeleteParty: (PartyEntity) -> Unit = {},
    onDeleteInvoice: (Long) -> Unit = {},
    onUpdateStoreProfile: (storeName: String, storeAddress: String, storePhone: String, taxNumber: String, invoiceFooterText: String, showPreviousBalance: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateShowPreviousBalance: (Boolean) -> Unit = {},
    onUpdateShowDecimals: (Boolean) -> Unit = {},
    onUpdateAutoLockSettings: (Boolean, Int) -> Unit = { _, _ -> },
    onUpdatePasswordPolicySettings: (com.example.dokkani.data.local.entities.PasswordType, Int, Boolean, Int, Int) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val isAdmin = currentUserRole == UserRole.ADMIN
    val baseCurr = remember(currencies) { currencies.find { it.isBaseCurrency } ?: currencies.firstOrNull() }

    var storeNameInput by remember(settings?.storeName) { mutableStateOf(settings?.storeName ?: "") }
    var storeAddressInput by remember(settings?.storeAddress) { mutableStateOf(settings?.storeAddress ?: "") }
    var storePhoneInput by remember(settings?.storePhone) { mutableStateOf(settings?.storePhone ?: "") }
    var taxNumberInput by remember(settings?.taxNumber) { mutableStateOf(settings?.taxNumber ?: "") }
    var invoiceFooterInput by remember(settings?.invoiceFooterText) { mutableStateOf(settings?.invoiceFooterText ?: "") }
    var showPreviousBalanceSwitch by remember(settings?.showPreviousBalanceOnInvoice) {
        mutableStateOf(settings?.showPreviousBalanceOnInvoice ?: true)
    }
    var profileSavedFeedback by remember { mutableStateOf(false) }

    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var editingCurrency by remember { mutableStateOf<CurrencyEntity?>(null) }
    var deletingCurrency by remember { mutableStateOf<CurrencyEntity?>(null) }
    var settingBaseCurrencyConfirm by remember { mutableStateOf<CurrencyEntity?>(null) }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<PartyEntity?>(null) }
    var deletingParty by remember { mutableStateOf<PartyEntity?>(null) }

    var deletingInvoiceId by remember { mutableStateOf<Long?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة بيانات المنشأة وترويسة الفواتير المطبوعة (البيع، الشراء، ومردوداتهما)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("store_profile_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "بيانات المنشأة وترويسة الفواتير والتقارير:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تظهر تلقائياً في ترويسة فواتير البيع، الشراء، مردود البيع، ومردود الشراء المطبوعة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // اسم المنشأة / البقالة
                        OutlinedTextField(
                            value = storeNameInput,
                            onValueChange = {
                                storeNameInput = it
                                profileSavedFeedback = false
                            },
                            label = { Text("اسم البقالة / المنشأة") },
                            placeholder = { Text("مثال: دكاني - تموينات ومخضار السعادة") },
                            modifier = Modifier.fillMaxWidth().testTag("store_name_input"),
                            singleLine = true,
                            enabled = isAdmin
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // عنوان المنشأة
                        OutlinedTextField(
                            value = storeAddressInput,
                            onValueChange = {
                                storeAddressInput = it
                                profileSavedFeedback = false
                            },
                            label = { Text("عنوان المنشأة / الفرع") },
                            placeholder = { Text("مثال: صنعاء - شارع الزبيري") },
                            modifier = Modifier.fillMaxWidth().testTag("store_address_input"),
                            singleLine = true,
                            enabled = isAdmin
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // هاتف المنشأة
                        OutlinedTextField(
                            value = storePhoneInput,
                            onValueChange = {
                                storePhoneInput = it
                                profileSavedFeedback = false
                            },
                            label = { Text("رقم هاتف المنشأة") },
                            placeholder = { Text("مثال: 777000111") },
                            modifier = Modifier.fillMaxWidth().testTag("store_phone_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            enabled = isAdmin
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // الرقم الضريبي للمنشأة
                        OutlinedTextField(
                            value = taxNumberInput,
                            onValueChange = {
                                taxNumberInput = it
                                profileSavedFeedback = false
                            },
                            label = { Text("الرقم الضريبي للمنشأة (إن وجد)") },
                            placeholder = { Text("مثال: 300123456700003") },
                            supportingText = {
                                Text("يُطبع تلقائياً في أعلى الفاتورة وفقاً للنظام الضريبي المعتمد")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("tax_number_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = isAdmin
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // رسالة ذيل الفاتورة
                        OutlinedTextField(
                            value = invoiceFooterInput,
                            onValueChange = {
                                invoiceFooterInput = it
                                profileSavedFeedback = false
                            },
                            label = { Text("رسالة تذييل الفاتورة المطبوعة") },
                            placeholder = { Text("مثال: شكراً لزيارتكم دكاني - تسوقكم يسعدنا!") },
                            modifier = Modifier.fillMaxWidth().testTag("invoice_footer_input"),
                            singleLine = true,
                            enabled = isAdmin
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // خيار إظهار الرصيد السابق في الفواتير الآجلة
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إظهار الرصيد السابق في الفواتير الآجلة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إظهار رصيد العميل أو المورد السابق وإجمالي الرصيد الحالي بعد العملية في طباعة ومعاينة الفواتير الآجلة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = showPreviousBalanceSwitch,
                                    onCheckedChange = {
                                        showPreviousBalanceSwitch = it
                                        profileSavedFeedback = false
                                        if (isAdmin) {
                                            onUpdateShowPreviousBalance(it)
                                        }
                                    },
                                    enabled = isAdmin,
                                    modifier = Modifier.testTag("show_previous_balance_switch")
                                )
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    onUpdateStoreProfile(
                                        storeNameInput,
                                        storeAddressInput,
                                        storePhoneInput,
                                        taxNumberInput,
                                        invoiceFooterInput,
                                        showPreviousBalanceSwitch
                                    )
                                    profileSavedFeedback = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_store_profile_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ بيانات المنشأة والفواتير")
                            }

                            if (profileSavedFeedback) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تم حفظ بيانات المنشأة والفواتير بنجاح وتحديث الترويسة المطبوعة!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة التحكم في عرض الكسور العشرية (Decimal Places Control)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("decimals_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "إعدادات عرض الكسور العشرية (Decimal Places)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (settings?.showDecimals == true)
                                            "مفعل: يتم إظهار المبالغ والأسعار بالكسور العشرية (مثال: 5000.00)"
                                        else
                                            "معطل (الافتراضي): يتم إظهار المبالغ والأسعار كأعداد صحيحة بدون كسور (مثال: 5000)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings?.showDecimals ?: false,
                                onCheckedChange = { if (isAdmin) onUpdateShowDecimals(it) },
                                enabled = isAdmin,
                                modifier = Modifier.testTag("show_decimals_switch")
                            )
                        }
                    }
                }
            }

            // بطاقة إعدادات طريقة تقييم التكلفة المحاسبية
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("system_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "طريقة التقييم المحاسبي المعتمدة لنظام دكاني:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "المخزنة في جدول (system_settings) لاحتساب تكلفة البضاعة المباعة وتقييم المخزون:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val currentMethod = settings?.costValuationMethod ?: CostValuationMethod.WAC

                        for (method in CostValuationMethod.values()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentMethod == method) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                border = if (currentMethod == method) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentMethod == method,
                                        onClick = { if (isAdmin) onUpdateValuationMethod(method) },
                                        enabled = isAdmin,
                                        modifier = Modifier.testTag("radio_method_${method.name}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${method.labelArabic} (${method.name})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = method.descriptionArabic,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // بطاقة التحكم في ضريبة القيمة المضافة (Tax & VAT Settings)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("tax_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "تفعيل / إلغاء ضريبة القيمة المضافة (Tax Control)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "التحكم في إظهار حسابات الضريبة ونسبتها المئوية في المبيعات والفواتير المطبوعة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings?.isTaxEnabled ?: false,
                                onCheckedChange = { isChecked ->
                                    if (isAdmin) {
                                        val rawRate = settings?.defaultTaxRate ?: 0.0
                                        onUpdateTaxSettings(isChecked, rawRate)
                                    }
                                },
                                enabled = isAdmin,
                                modifier = Modifier.testTag("tax_enable_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // حقل إدخال نسبة الضريبة اليدوية
                        val currentRate = settings?.defaultTaxRate ?: 0.0
                        val pctValue = if (currentRate <= 1.0) currentRate * 100.0 else currentRate
                        var taxRateText by remember(settings?.defaultTaxRate) {
                            mutableStateOf(if (pctValue % 1.0 == 0.0) pctValue.toInt().toString() else "%.1f".format(pctValue))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = taxRateText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        taxRateText = input
                                        val parsed = input.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0.0 && isAdmin) {
                                            val rateToSave = parsed / 100.0
                                            val isEnabled = settings?.isTaxEnabled ?: false
                                            onUpdateTaxSettings(isEnabled, rateToSave)
                                        }
                                    }
                                },
                                label = { Text("نسبة الضريبة المئوية (%)") },
                                suffix = { Text("%") },
                                enabled = isAdmin && (settings?.isTaxEnabled ?: false),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("tax_rate_input")
                            )

                            Button(
                                onClick = {
                                    val parsed = taxRateText.toDoubleOrNull() ?: 0.0
                                    val rateToSave = parsed / 100.0
                                    val isEnabled = settings?.isTaxEnabled ?: false
                                    onUpdateTaxSettings(isEnabled, rateToSave)
                                },
                                enabled = isAdmin && (settings?.isTaxEnabled ?: false),
                                modifier = Modifier.testTag("save_tax_rate_button")
                            ) {
                                Text("حفظ النسبة")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // صندوق الحالة التوضيحي
                        val isTaxActive = settings?.isTaxEnabled ?: false
                        val activeRate = settings?.defaultTaxRate ?: 0.0
                        val displayPct = if (activeRate <= 1.0) activeRate * 100.0 else activeRate

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTaxActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isTaxActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isTaxActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                 Text(
                                    text = if (isTaxActive)
                                        "الضريبة مفعلة بنسبة ${if (displayPct % 1.0 == 0.0) displayPct.toInt().toString() else "%.1f".format(displayPct)}%: يتم احتساب الضريبة تلقائياً وإدراجها في خلاصة السلة والإجمالي النهائي وطباعتها بالفواتير."
                                    else
                                        "الضريبة معطلة: تم إخفاء جميع صفوف وحسابات الضريبة من شاشة المبيعات والفواتير، والإجمالي النهائي يساوي المجموع الفرعي.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTaxActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة التحكم في ضريبة المشتريات المستقلة (Purchase Tax Control)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("purchase_tax_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ضريبة المشتريات المستقلة (Purchase Tax Control)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تفعيل أو إلغاء حساب الضريبة على فواتير الشراء والتوريد بشكل مستقل عن المبيعات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings?.isPurchaseTaxEnabled ?: false,
                                onCheckedChange = { isChecked ->
                                    if (isAdmin) {
                                        val rawRate = settings?.purchaseTaxRate ?: 0.0
                                        onUpdatePurchaseTaxSettings(isChecked, rawRate)
                                    }
                                },
                                enabled = isAdmin,
                                modifier = Modifier.testTag("purchase_tax_enable_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val currentPurRate = settings?.purchaseTaxRate ?: 0.0
                        val pctPurValue = if (currentPurRate <= 1.0) currentPurRate * 100.0 else currentPurRate
                        var purTaxRateText by remember(settings?.purchaseTaxRate) {
                            mutableStateOf(if (pctPurValue % 1.0 == 0.0) pctPurValue.toInt().toString() else "%.1f".format(pctPurValue))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = purTaxRateText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                        purTaxRateText = input
                                        val parsed = input.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0.0 && isAdmin) {
                                            val rateToSave = parsed / 100.0
                                            val isEnabled = settings?.isPurchaseTaxEnabled ?: false
                                            onUpdatePurchaseTaxSettings(isEnabled, rateToSave)
                                        }
                                    }
                                },
                                label = { Text("نسبة ضريبة المشتريات (%)") },
                                suffix = { Text("%") },
                                enabled = isAdmin && (settings?.isPurchaseTaxEnabled ?: false),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("purchase_tax_rate_input")
                            )

                            Button(
                                onClick = {
                                    val parsed = purTaxRateText.toDoubleOrNull() ?: 0.0
                                    val rateToSave = parsed / 100.0
                                    val isEnabled = settings?.isPurchaseTaxEnabled ?: false
                                    onUpdatePurchaseTaxSettings(isEnabled, rateToSave)
                                },
                                enabled = isAdmin && (settings?.isPurchaseTaxEnabled ?: false),
                                modifier = Modifier.testTag("save_purchase_tax_rate_button")
                            ) {
                                Text("حفظ النسبة")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val isPurTaxActive = settings?.isPurchaseTaxEnabled ?: false
                        val activePurRate = settings?.purchaseTaxRate ?: 0.0
                        val displayPurPct = if (activePurRate <= 1.0) activePurRate * 100.0 else activePurRate

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPurTaxActive) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPurTaxActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isPurTaxActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPurTaxActive)
                                        "ضريبة المشتريات مفعلة بنسبة ${if (displayPurPct % 1.0 == 0.0) displayPurPct.toInt().toString() else "%.1f".format(displayPurPct)}%: يتم احتساب قيمة الضريبة تلقائياً على فواتير التوريد بالشراء وإضافتها للإجمالي الإجمالي."
                                    else
                                        "ضريبة المشتريات معطلة: تم إخفاء ضريبة الشراء وتصفير قيمتها، والإجمالي النهائي لفاتورة الشراء يساوي المجموع الفرعي تماماً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPurTaxActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة خيارات قفل الشاشة التلقائي والأمان
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("auto_lock_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "قفل الشاشة التلقائي وتأمين الجلسة (Auto Lock)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "قفل التطبيق وتعديل حالة الجلسة تلقائياً عند ترك جهاز الكاشير دون استخدام",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isAutoLockActive = settings?.enableAutoLock ?: true
                            val currentTimeout = settings?.autoLockSeconds ?: 120

                            Switch(
                                checked = isAutoLockActive,
                                onCheckedChange = { isChecked ->
                                    if (isAdmin) {
                                        onUpdateAutoLockSettings(isChecked, currentTimeout)
                                    }
                                },
                                enabled = isAdmin,
                                modifier = Modifier.testTag("auto_lock_enable_switch")
                            )
                        }

                        if (settings?.enableAutoLock ?: true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "مهلة عدم النشاط قبل القفل التلقائي:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val options = listOf(
                                30 to "30 ثانية",
                                60 to "دقيقة واحدة",
                                120 to "دقيقتان (الافتراضي)",
                                300 to "5 دقائق",
                                600 to "10 دقائق"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                options.forEach { (sec, label) ->
                                    val isSel = (settings?.autoLockSeconds ?: 120) == sec
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            if (isAdmin) {
                                                onUpdateAutoLockSettings(true, sec)
                                            }
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        enabled = isAdmin
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // سياسة نوع وطول كلمات المرور
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "سياسة ونوع كلمات المرور والرمز السري (Password & PIN Policy)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تحديد أسلوب الدخول وشروط التعقيد للرمز السري والكاشير",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val currentType = settings?.passwordType ?: com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN
                        val currentPinLen = settings?.pinLength ?: 4
                        val autoSubmit = settings?.enableAutoSubmitPin ?: true
                        val minLen = settings?.minPasswordLength ?: 8
                        val maxLen = settings?.maxPasswordLength ?: 16

                        Text(
                            text = "نوع ونمط كلمة المرور المعتمدة بالنظام:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currentType == com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN,
                                onClick = {
                                    if (isAdmin) {
                                        onUpdatePasswordPolicySettings(
                                            com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN,
                                            currentPinLen,
                                            autoSubmit,
                                            minLen,
                                            maxLen
                                        )
                                    }
                                },
                                label = { Text("أرقام فقط (PIN)", fontSize = 12.sp) },
                                enabled = isAdmin
                            )

                            FilterChip(
                                selected = currentType == com.example.dokkani.data.local.entities.PasswordType.ALPHANUMERIC,
                                onClick = {
                                    if (isAdmin) {
                                        onUpdatePasswordPolicySettings(
                                            com.example.dokkani.data.local.entities.PasswordType.ALPHANUMERIC,
                                            currentPinLen,
                                            false,
                                            minLen,
                                            maxLen
                                        )
                                    }
                                },
                                label = { Text("أرقام وحروف ورموز (Alphanumeric)", fontSize = 12.sp) },
                                enabled = isAdmin
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentType == com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN) {
                            Text(
                                text = "طول الـ PIN المطلوب:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentPinLen == 4,
                                    onClick = {
                                        if (isAdmin) {
                                            onUpdatePasswordPolicySettings(
                                                com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN,
                                                4,
                                                autoSubmit,
                                                minLen,
                                                maxLen
                                            )
                                        }
                                    },
                                    label = { Text("4 أرقام", fontSize = 12.sp) },
                                    enabled = isAdmin
                                )

                                FilterChip(
                                    selected = currentPinLen == 6,
                                    onClick = {
                                        if (isAdmin) {
                                            onUpdatePasswordPolicySettings(
                                                com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN,
                                                6,
                                                autoSubmit,
                                                minLen,
                                                maxLen
                                            )
                                        }
                                    },
                                    label = { Text("6 أرقام", fontSize = 12.sp) },
                                    enabled = isAdmin
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الدخول التلقائي عند اكتمال الـ PIN:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تسجيل الدخول فور كتابة الرقم دون الحاجة للضغط على زر موافقة/دخول",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoSubmit,
                                    onCheckedChange = { isChecked ->
                                        if (isAdmin) {
                                            onUpdatePasswordPolicySettings(
                                                com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN,
                                                currentPinLen,
                                                isChecked,
                                                minLen,
                                                maxLen
                                            )
                                        }
                                    },
                                    enabled = isAdmin
                                )
                            }
                        } else {
                            Text(
                                text = "طول كلمة المرور المركبة (الحد الأدنى والأقصى):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(8, 10, 12).forEach { minVal ->
                                    FilterChip(
                                        selected = minLen == minVal,
                                        onClick = {
                                            if (isAdmin) {
                                                onUpdatePasswordPolicySettings(
                                                    com.example.dokkani.data.local.entities.PasswordType.ALPHANUMERIC,
                                                    currentPinLen,
                                                    false,
                                                    minVal,
                                                    maxLen
                                                )
                                            }
                                        },
                                        label = { Text("أدنى: $minVal أحرف", fontSize = 11.sp) },
                                        enabled = isAdmin
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه إرشادي: عند تغير السياسة، يُعرض إشعار تعليمات للمستخدم عند إنشاء أو تغيير الرمز يوضح الشروط وطبيعة الرموز والحد المسموح به.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // ضبط الرصيد المخزني ومنع البيع بالسالب
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "التحقق من توفر المخزون عند البيع (Stock Validation)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تحديد سياسة البيع عند نفاد الكمية أو عدم توفر رصيد بالمخزن",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings?.enableNegativeStock == true,
                                onCheckedChange = { if (isAdmin) onUpdateEnableNegativeStock(it) },
                                enabled = isAdmin
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (settings?.enableNegativeStock == true)
                                Color(0xFFFFF3E0)
                            else
                                Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (settings?.enableNegativeStock == true) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (settings?.enableNegativeStock == true) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (settings?.enableNegativeStock == true)
                                        "السماح بالبيع بالسالب مفعل: يمكن للكاشير إتمام الفواتير حتى وإن نفدت الكمية من المخزن (مفيد عند تأخر إدخال فواتير التوريد)."
                                    else
                                        "منع البيع بالسالب مفعل (موصى به): سيقوم النظام بفحص كمية المخزون ومنع إتمام الفاتورة إذا كانت الكمية المطلوبة غير متوفرة بالمخزن.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings?.enableNegativeStock == true) Color(0xFFE65100) else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!isAdmin) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تعديل هذا الخيار متاح فقط لمدير النظام (Admin)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // جدول العملات وأسعار الصرف
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = Color(0xFF0F5132),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إدارة العملات والعملة الأساسية (Currencies):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isAdmin) {
                                Button(
                                    onClick = { showAddCurrencyDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("عملة جديدة", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // بطاقة إرشادات العملة الأساسية الحالية
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "العملة الأساسية للنظام: ${baseCurr?.name ?: "غير محددة"} (${baseCurr?.symbol ?: ""})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "تقاس القوائم المالية، الأصول، ورأس المال بالعملة الأساسية. يمكنك تغيير العملة الأساسية في أي وقت من القائمة أدناه.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        for (curr in currencies) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${curr.name} (${curr.code})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (curr.isBaseCurrency) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0F5132)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "الأساسية (1.0)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else if (isAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        OutlinedButton(
                                            onClick = { settingBaseCurrencyConfirm = curr },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "جعلها الأساسية",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (curr.isBaseCurrency) "1.0 ${curr.symbol}" else "1 ${curr.symbol} = ${curr.exchangeRateToBase} ${baseCurr?.symbol ?: "ر.ي"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { editingCurrency = curr },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل العملة",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (!curr.isBaseCurrency) {
                                            IconButton(
                                                onClick = { deletingCurrency = curr },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف العملة",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // جدول العملاء والموردين
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جدول العملاء والموردين (Parties):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isAdmin) {
                                Button(
                                    onClick = { showAddPartyDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("إضافة عميل/مورد", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        for (p in parties) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = p.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${p.type.labelArabic} • ${p.phone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "الرصيد: %.2f %s".format(p.currentBalance, baseCurr?.symbol ?: "ر.ي"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.currentBalance >= 0) Color(0xFF0F5132) else Color(0xFFDC3545)
                                    )

                                    if (isAdmin) {
                                        IconButton(
                                            onClick = { editingParty = p },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { deletingParty = p },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // سجل الفواتير الأخيرة
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سجل فواتير النظام (Invoices):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (invoices.isEmpty()) {
                            Text(
                                text = "لا توجد فواتير مسجلة بعد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            for (i in invoices) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${i.invoiceNumber} (${i.type.labelArabic})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(i.date))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "%.2f %s".format(i.total, baseCurr?.symbol ?: "ر.ي"),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F5132)
                                            )
                                            Text(
                                                text = i.paymentMethod.labelArabic,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        if (isAdmin) {
                                            IconButton(
                                                onClick = { deletingInvoiceId = i.id },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الفاتورة",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
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
    }

    // Dialogs
    if (showAddCurrencyDialog || editingCurrency != null) {
        AddEditCurrencyDialog(
            initialCurrency = editingCurrency,
            onSaveCurrency = { c ->
                onSaveCurrency(c)
                showAddCurrencyDialog = false
                editingCurrency = null
            },
            onDismiss = {
                showAddCurrencyDialog = false
                editingCurrency = null
            }
        )
    }

    if (settingBaseCurrencyConfirm != null) {
        val target = settingBaseCurrencyConfirm!!
        AlertDialog(
            onDismissRequest = { settingBaseCurrencyConfirm = null },
            icon = {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF0F5132),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد تغيير العملة الأساسية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت تأكيد من تغيير العملة الأساسية للنظام إلى '${target.name} (${target.symbol})'؟\n\nسيتم جعل سعر صرف هذه العملة (1.0) وإعادة تحويل وتعديل أسعار الصرف النسبية للعملات الأخرى تلقائياً.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetBaseCurrency(target.id)
                        settingBaseCurrencyConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                ) {
                    Text("تأكيد التعيين كعملة أساسية")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { settingBaseCurrencyConfirm = null }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (deletingCurrency != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من حذف العملة '${deletingCurrency?.name}'؟",
            onConfirm = {
                onDeleteCurrency(deletingCurrency!!)
                deletingCurrency = null
            },
            onDismiss = { deletingCurrency = null }
        )
    }

    if (showAddPartyDialog || editingParty != null) {
        AddEditPartyDialog(
            initialParty = editingParty,
            onSaveParty = { p ->
                onSaveParty(p)
                showAddPartyDialog = false
                editingParty = null
            },
            onDismiss = {
                showAddPartyDialog = false
                editingParty = null
            }
        )
    }

    if (deletingParty != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من حذف الحساب '${deletingParty?.name}'؟",
            onConfirm = {
                onDeleteParty(deletingParty!!)
                deletingParty = null
            },
            onDismiss = { deletingParty = null }
        )
    }

    if (deletingInvoiceId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من حذف الفاتورة رقم #$deletingInvoiceId؟",
            onConfirm = {
                onDeleteInvoice(deletingInvoiceId!!)
                deletingInvoiceId = null
            },
            onDismiss = { deletingInvoiceId = null }
        )
    }
}
