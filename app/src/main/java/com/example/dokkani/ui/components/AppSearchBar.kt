package com.example.dokkani.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * دالة معالجة وتفسير أسماء ومسميات الأصناف باللغة العربية (Arabic Dialect & Term Normalizer)
 * تقوم بتنظيف وتوحيد الأحرف العربية وعلامات التشكيل لتسهيل مطابقة الصوت المكتوب أو المنطوق
 * مع أسماء وأكواد الأصناف المخزنة بجميع اللهجات والمصطلحات التجارية.
 */
fun normalizeArabicItemSearch(rawQuery: String): String {
    if (rawQuery.isBlank()) return ""
    var q = rawQuery.trim().lowercase(Locale.ROOT)

    // 1. إزالة التشكيل والحركات العربية
    q = q.replace(Regex("[\u064B-\u0652]"), "")

    // 2. توحيد أشكال الألف الهمزة (أ، إ، آ -> ا)
    q = q.replace(Regex("[أإآ]"), "ا")

    // 3. توحيد الياء المقصورة والتاء المربوطة (ى -> ي, ة -> ه)
    q = q.replace('ى', 'ي')
    q = q.replace('ة', 'ه')

    // 4. توحيد المسافات الزائدة
    q = q.replace(Regex("\\s+"), " ")

    return q
}

/**
 * دالة فحص المطابقة الذكية للـ Query مع بيانات الصنف (الاسم، الباركود، الكود، التصنيف)
 * تدعم المطابقة المباشرة، المطابقة المرنة للكلمات المنطوقة، وبدون "ال" التعريف.
 */
fun isItemMatchQuery(
    itemName: String,
    itemCode: String = "",
    barcode: String = "",
    category: String = "",
    searchQuery: String
): Boolean {
    val cleanQuery = normalizeArabicItemSearch(searchQuery)
    if (cleanQuery.isBlank()) return true

    val cleanName = normalizeArabicItemSearch(itemName)
    val cleanCode = normalizeArabicItemSearch(itemCode)
    val cleanBarcode = normalizeArabicItemSearch(barcode)
    val cleanCat = normalizeArabicItemSearch(category)

    // مطابقة مباشرة مع الأكواد والباركود أو الاسم
    if (cleanName.contains(cleanQuery) || cleanCode.contains(cleanQuery) ||
        cleanBarcode.contains(cleanQuery) || cleanCat.contains(cleanQuery)) {
        return true
    }

    // مطابقة بدون ال التعريف
    val queryNoAL = if (cleanQuery.startsWith("ال") && cleanQuery.length > 2) cleanQuery.substring(2) else cleanQuery
    val nameNoAL = if (cleanName.startsWith("ال") && cleanName.length > 2) cleanName.substring(2) else cleanName

    if (nameNoAL.contains(queryNoAL) || cleanCode.contains(queryNoAL) || cleanBarcode.contains(queryNoAL)) {
        return true
    }

    // مطابقة الكلمات المتعددة من البحث الصوتي (مثال: "حليب المراعي طازج")
    val queryWords = cleanQuery.split(" ").filter { it.length > 1 }
    if (queryWords.size > 1) {
        val allWordsMatch = queryWords.all { word ->
            val wNoAl = if (word.startsWith("ال") && word.length > 2) word.substring(2) else word
            cleanName.contains(word) || cleanName.contains(wNoAl) ||
                    cleanCode.contains(word) || cleanBarcode.contains(word) || cleanCat.contains(word)
        }
        if (allWordsMatch) return true
    }

    return false
}

/**
 * مكون البحث الذكي والموحد للأصناف (Smart Items Search Component - AppSearchBar)
 * مدعوم بـ Material 3 ومخصص حصرياً للبحث عن الأصناف والمنتجات عبر الكتابة اليدوية أو التعرف الصوتي.
 */
@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "ابحث عن صنف بالاسم، الكود، أو بالميكروفون...",
    label: String? = "بحث الأصناف",
    enableVoiceSearch: Boolean = true,
    enableBarcodeScanner: Boolean = true,
    onBarcodeScanned: ((String) -> Unit)? = null,
    onSearchSubmitted: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var isListeningVoice by remember { mutableStateOf(false) }

    // مشغل التعرف على الكلام (Speech Recognition Launcher)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                val rawSpokenText = spokenMatches[0]
                val normalizedText = rawSpokenText.trim()
                onValueChange(normalizedText)
                onSearchSubmitted?.invoke(normalizedText)
                Toast.makeText(context, "تم الالتقاط الصوتي: $normalizedText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // مشغل طلب إذن الميكروفون
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchVoiceRecognition(context, speechRecognizerLauncher) { isListeningVoice = true }
        } else {
            Toast.makeText(context, "يلزم منح إذن الميكروفون لاستخدام البحث الصوتي للأصناف", Toast.LENGTH_LONG).show()
        }
    }

    // مشغل الماسح الضوئي للباركود
    val barcodeScannerLauncher = rememberBarcodeScannerLauncher { scannedBarcode ->
        onValueChange(scannedBarcode)
        onBarcodeScanned?.invoke(scannedBarcode)
        onSearchSubmitted?.invoke(scannedBarcode)
    }

    // حركة النبض البصري عند استماع الميكروفون
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_bar_input"),
            label = label?.let { { Text(it, fontSize = 12.sp) } },
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "أيقونة البحث",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    // زر مسح النص
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onValueChange("")
                                onSearchSubmitted?.invoke("")
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // زر البحث الصوتي المحلي أوفلاين
                    if (enableVoiceSearch) {
                        VoiceInputIconButton(
                            onTextCaptured = { spokenText ->
                                onValueChange(spokenText)
                                onSearchSubmitted?.invoke(spokenText)
                                Toast.makeText(context, "تم الالتقاط الصوتي المحلي: $spokenText", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // زر مسح الباركود بالكاميرا (إن وُجد)
                    if (enableBarcodeScanner) {
                        IconButton(
                            onClick = { barcodeScannerLauncher.launch() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("search_barcode_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "مسح باركود الصنف",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmitted?.invoke(value)
                }
            )
        )

        // شريط تنبيه بصري عند تفعيل الاستماع الصوتي
        AnimatedVisibility(visible = isListeningVoice) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "جاري الاستماع الآن... انطق اسم الصنف أو كوده باللغة العربية",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * دالة استدعاء نية التعرف الصوتي برقم لغة العربية الشاملة (Arabic Voice Recognition Intent)
 */
private fun launchVoiceRecognition(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStarted: () -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "انطق اسم الصنف، باركوده، أو المسمى التجاري...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        onStarted()
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر تشغيل خدمة التعرف الصوتي على هذا الجهاز", Toast.LENGTH_SHORT).show()
    }
}
