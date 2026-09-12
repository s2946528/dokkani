package com.example.dokkani.ui.screens.license

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.domain.security.ActivationPlan
import com.example.dokkani.domain.security.DeviceFingerprintManager
import com.example.dokkani.domain.security.LicenseStatus
import com.example.dokkani.ui.DokkaniUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة إدارة الترخيص والحماية بدون إنترنت لنظام دكاني (Dokkani Licensing & Security)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    uiState: DokkaniUiState,
    onSelectPlanForRequest: (ActivationPlan) -> Unit,
    onRefreshChallengeCode: () -> Unit,
    onActivationCodeChanged: (String) -> Unit,
    onApplyActivationCode: () -> Unit,
    onToggleDeveloperKeyGen: () -> Unit,
    onKeyGenRequestInputChanged: (String) -> Unit,
    onKeyGenPlanChanged: (ActivationPlan) -> Unit,
    onKeyGenCustomDaysChanged: (String) -> Unit,
    onGenerateKeyGenCode: () -> Unit,
    onApplyGeneratedKeyGenCodeDirectly: () -> Unit = {},
    onResetTrialForTesting: () -> Unit = {},
    onSimulateTimeTamperForTesting: () -> Unit = {},
    onClearTimeTamper: () -> Unit = {},
    onCopyToClipboard: (Context, String, String) -> Unit = { _, _, _ -> },
    onShareViaWhatsApp: (Context, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eval = uiState.licenseEvaluation
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. بطاقة حالة الترخيص العامة
        LicenseOverviewCard(
            eval = eval,
            deviceFingerprint = uiState.deviceFingerprint,
            context = context,
            onCopyToClipboard = onCopyToClipboard
        )

        // تنبيه تحذيري إذا كان مقفلاً أو قريباً من الانتهاء
        if (eval?.warningMessageArabic != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (eval.isLocked) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF3CD)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("license_warning_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (eval.isLocked) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (eval.isLocked) MaterialTheme.colorScheme.error else Color(0xFF856404),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = eval.warningMessageArabic,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (eval.isLocked) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF856404)
                    )
                }
            }
        }

        // 2. بطاقة طلب كود التفعيل (Challenge Generator)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("challenge_request_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الخطوة 1: استخراج كود الطلب (Challenge Code)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اختر الباقة المطلوبة لإرسال كود الطلب المربوط ببصمة هذا الهاتف حصرياً إلى إدارة دكاني عبر واتساب لتوليد كود التفعيل:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // اختيار الباقة
                Text(
                    text = "نوع الباقة المطلوبة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val availablePlans = listOf(
                    ActivationPlan.MONTHLY_1,
                    ActivationPlan.MONTHLY_3,
                    ActivationPlan.MONTHLY_6,
                    ActivationPlan.YEARLY_1,
                    ActivationPlan.INSTALLMENT,
                    ActivationPlan.LIFETIME
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availablePlans.take(3).forEach { plan ->
                        FilterChip(
                            selected = uiState.selectedPlanForRequest == plan,
                            onClick = { onSelectPlanForRequest(plan) },
                            label = { Text(plan.labelArabic, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availablePlans.drop(3).forEach { plan ->
                        FilterChip(
                            selected = uiState.selectedPlanForRequest == plan,
                            onClick = { onSelectPlanForRequest(plan) },
                            label = { Text(plan.labelArabic, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // عرض كود الطلب
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "كود الطلب الخاص بك (Challenge):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.generatedChallengeCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("text_generated_challenge_code")
                            )
                        }

                        Row {
                            IconButton(onClick = { onRefreshChallengeCode() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث الكود")
                            }
                            IconButton(onClick = {
                                onCopyToClipboard(context, uiState.generatedChallengeCode, "كود الطلب")
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ كود الطلب")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // زر الإرسال عبر واتساب
                Button(
                    onClick = { onShareViaWhatsApp(context, uiState.generatedChallengeCode) },
                    modifier = Modifier.fillMaxWidth().testTag("btn_share_whatsapp_challenge"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال كود الطلب عبر واتساب للمطور / الإدارة", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. بطاقة تطبيق كود التفعيل (Activation Submission)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("activation_submission_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الخطوة 2: إدخال كود التفعيل المعتمد (Activation Code)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الصق كود التفعيل المستلم من الإدارة الذي يبدأ بـ (ACT-) ثم اضغط تفعيل لتطبيق الصلاحيات وفتح النظام بدون إنترنت:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.activationCodeInput,
                    onValueChange = { onActivationCodeChanged(it) },
                    label = { Text("كود التفعيل (مثل: ACT-M01-A8F2-...)") },
                    placeholder = { Text("ACT-...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_activation_code"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Shield, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.activationCodeInput.isNotBlank()) {
                            IconButton(onClick = { onActivationCodeChanged("") }) {
                                Icon(Icons.Default.Refresh, contentDescription = "مسح")
                            }
                        }
                    }
                )

                if (uiState.activationFeedbackMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isSuccessMsg = uiState.activationFeedbackMessage.contains("نجاح")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSuccessMsg) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccessMsg) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isSuccessMsg) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.activationFeedbackMessage,
                            color = if (isSuccessMsg) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onApplyActivationCode() },
                    enabled = !uiState.isActivating && uiState.activationCodeInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("btn_apply_activation_code")
                ) {
                    if (uiState.isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري فك التشفير والتحقق...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تطبيق وتفعيل الترخيص أوفلاين", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. بطاقة الأمان ومكافحة التلاعب بالساعة (Anti-Time Tampering Guard)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("anti_tamper_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (eval?.isTimeTampered == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نظام حماية التلاعب بالوقت والتاريخ (Anti-Tampering)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "يراقب النظام تلقائياً تطابق ساعة الجهاز مع تاريخ الفواتير المسجلة في قاعدة البيانات ويمنع إرجاع التاريخ للوراء للالتفاف على الاشتراكات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حالة تدقيق الوقت:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (eval?.isTimeTampered == true) "تم اكتشاف تلاعب (مقفل أمنياً)" else "سليم ومحمي أمنياً ✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (eval?.isTimeTampered == true) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ساعة النظام الحالية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val df = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(
                        text = df.format(Date()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // أدوات اختبار بيئة التطوير والمحاكاة
                Text(
                    text = "أدوات اختبار الأمان والتراخيص (Sandbox Testing):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onResetTrialForTesting() },
                        modifier = Modifier.weight(1f).testTag("btn_test_reset_trial")
                    ) {
                        Text("إعادة ضبط تجريبي", fontSize = 11.sp)
                    }

                    if (eval?.isTimeTampered == true) {
                        Button(
                            onClick = { onClearTimeTamper() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f).testTag("btn_test_clear_tamper")
                        ) {
                            Text("فك قفل التلاعب", fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSimulateTimeTamperForTesting() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("btn_test_simulate_tamper")
                        ) {
                            Text("محاكاة تلاعب الوقت", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. قسم أدوات المطور (Dokkani Developer Key Generator)
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("developer_keygen_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مولد مفاتيح وتراخيص دكاني (KeyGen)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أداة المطور المعتمدة لتوليد أكواد التفعيل من طلبات التجار",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onToggleDeveloperKeyGen() },
                        modifier = Modifier.testTag("btn_toggle_keygen")
                    ) {
                        Text(if (uiState.isDeveloperKeyGenExpanded) "إغلاق" else "فتح الأداة", fontSize = 11.sp)
                    }
                }

                AnimatedVisibility(visible = uiState.isDeveloperKeyGenExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.keyGenRequestCodeInput,
                            onValueChange = { onKeyGenRequestInputChanged(it) },
                            label = { Text("كود طلب التاجر المستلم (REQ-...)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_keygen_request"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("اختر الباقة المراد منحها للتاجر:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        val keyGenPlans = listOf(
                            ActivationPlan.MONTHLY_1,
                            ActivationPlan.MONTHLY_3,
                            ActivationPlan.YEARLY_1,
                            ActivationPlan.LIFETIME
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            keyGenPlans.forEach { plan ->
                                FilterChip(
                                    selected = uiState.keyGenSelectedPlan == plan,
                                    onClick = { onKeyGenPlanChanged(plan) },
                                    label = { Text(plan.labelArabic, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onGenerateKeyGenCode() },
                            modifier = Modifier.fillMaxWidth().testTag("btn_keygen_generate"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد كود التفعيل المعتمد (HMAC-SHA256)", fontWeight = FontWeight.Bold)
                        }

                        if (uiState.keyGenGeneratedResult != null) {
                            val res = uiState.keyGenGeneratedResult
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "كود التفعيل الصادر:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res.activationCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("text_keygen_result_code")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الباقة: ${res.planLabelArabic} | الصلاحية: ${res.expiryDateFormatted}",
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onCopyToClipboard(context, res.activationCode, "كود التفعيل المعتمد")
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ الكود", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { onApplyGeneratedKeyGenCodeDirectly() },
                                            modifier = Modifier.weight(1.5f).testTag("btn_keygen_apply_directly")
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تطبيق وتفعيل هذا الجهاز فوراً", fontSize = 11.sp)
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

/**
 * بطاقة الملخص العام للترخيص وبصمة العتاد
 */
@Composable
private fun LicenseOverviewCard(
    eval: com.example.dokkani.domain.security.LicenseEvaluationResult?,
    deviceFingerprint: String,
    context: Context,
    onCopyToClipboard: (Context, String, String) -> Unit
) {
    val status = eval?.status ?: LicenseStatus.TRIAL

    val (statusColor, statusIcon) = when (status) {
        LicenseStatus.LIFETIME -> Color(0xFF2E7D32) to Icons.Default.CheckCircle
        LicenseStatus.SUBSCRIPTION -> Color(0xFF1565C0) to Icons.Default.VerifiedUser
        LicenseStatus.TRIAL -> Color(0xFFE65100) to Icons.Default.Info
        LicenseStatus.EXPIRED -> MaterialTheme.colorScheme.error to Icons.Default.Lock
        LicenseStatus.TAMPERED -> MaterialTheme.colorScheme.error to Icons.Default.Error
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("license_overview_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "نظام ترخيص دكاني (Dokkani POS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = status.labelArabic,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // علامة الحالة
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (eval?.isLocked == true) "مقفل" else "نشط",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // بصمة الجهاز
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("بصمة الجهاز الفريدة (UUID):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = deviceFingerprint.ifBlank { "DK-XXXX-XXXX-XXXX" },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("text_device_fingerprint")
                        )
                    }
                }

                IconButton(onClick = {
                    onCopyToClipboard(context, deviceFingerprint, "بصمة الجهاز")
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ البصمة", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // مؤشر العمليات أو الصلاحية
            if (eval != null) {
                if (eval.isLifetime) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النظام مرخص نهائياً مدى الحياة بدون حد للعمليات أو قيود زمنية.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else if (eval.status == LicenseStatus.SUBSCRIPTION && eval.expiryTimestamp != null) {
                    val df = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val expiryStr = df.format(Date(eval.expiryTimestamp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاريخ انتهاء القسط/الاشتراك:", fontSize = 12.sp)
                            Text(expiryStr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الأيام المتبقية:", fontSize = 12.sp)
                            Text(
                                text = "${eval.daysRemaining} يوماً",
                                fontWeight = FontWeight.Bold,
                                color = if (eval.isNearExpiryWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // الوضع التجريبي (TRIAL)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "فواتير النسخة التجريبية: ${eval.totalInvoicesIssued} / ${eval.maxAllowedInvoices}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "متبقي ${eval.invoicesRemaining} عملية",
                                fontSize = 12.sp,
                                color = if (eval.invoicesRemaining <= 50) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = if (eval.maxAllowedInvoices > 0) {
                            (eval.totalInvoicesIssued.toFloat() / eval.maxAllowedInvoices.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (eval.invoicesRemaining <= 50) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
