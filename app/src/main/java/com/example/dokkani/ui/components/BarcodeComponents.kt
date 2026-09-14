package com.example.dokkani.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * دالة لتشغيل صوت تنبيه (Beep) قصير عبر ToneGenerator المدمج في نظام أندرويد
 * دون الحاجة لأي ملفات صوتية خارجية أو أذونات إضافية.
 */
fun playBeepSound(durationMs: Int = 150) {
    try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                toneGenerator.release()
            } catch (_: Exception) {}
        }, (durationMs + 50).toLong())
    } catch (e: Exception) {
        // يتم التجاوز بأمان في حال عدم توفر خدمة الصوت في بيئة معينة
    }
}

/**
 * واجهة مقبض الماسح الضوئي (Scanner Controller) لإدارة فتح وإغلاق الكاميرا
 */
class BarcodeScannerController internal constructor(
    private val onOpenRequested: () -> Unit
) {
    fun launch() {
        onOpenRequested()
    }
}

/**
 * Composable Helper لتجهيز إذن الكاميرا واستقبال نتيجة مسح الباركود باستخدام rememberLauncherForActivityResult
 */
@Composable
fun rememberBarcodeScannerLauncher(
    onBarcodeScanned: (String) -> Unit
): BarcodeScannerController {
    val context = LocalContext.current
    var showScannerDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScannerDialog = true
        } else {
            Toast.makeText(context, "يلزم منح إذن الكاميرا لمسح الباركود", Toast.LENGTH_SHORT).show()
            // فتح الماسح للإدخال اليدوي أو اختيار النماذج حتى عند رفض الإذن
            showScannerDialog = true
        }
    }

    if (showScannerDialog) {
        CameraBarcodeScannerDialog(
            onBarcodeScanned = { code ->
                playBeepSound()
                showScannerDialog = false
                onBarcodeScanned(code)
            },
            onDismiss = { showScannerDialog = false }
        )
    }

    return remember {
        BarcodeScannerController {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                showScannerDialog = true
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

/**
 * مكون عرض الكاميرا الحقيقية وتحليل إطارات الفيديو باستخدام CameraX و Google ML Kit
 */
@Composable
fun LiveCameraBarcodePreview(
    torchEnabled: Boolean,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(torchEnabled, camera) {
        try {
            camera?.cameraControl?.enableTorch(torchEnabled)
        } catch (_: Exception) {}
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (cameraError != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cameraError ?: "تعذر تشغيل الكاميرا",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        AndroidView(
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val barcodeScanner = BarcodeScanning.getClient()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            @androidx.annotation.OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !hasScanned) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (!hasScanned && barcodes.isNotEmpty()) {
                                            val rawValue = barcodes.firstOrNull()?.rawValue
                                            if (!rawValue.isNullOrBlank()) {
                                                hasScanned = true
                                                Handler(Looper.getMainLooper()).post {
                                                    onBarcodeScanned(rawValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        cameraError = "تعذر فتح عدسة الكاميرا: ${e.localizedMessage ?: "تأكد من إذن الكاميرا"}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }
}

/**
 * نافذة الماسح الضوئي التفاعلية للكاميرا مع إطار المسح والخط الليزري المتحرك
 * ونماذج الباركود للاختبار السريع
 */
@Composable
fun CameraBarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }

    // حركة الليزر المتحرك داخل إطار الكاميرا
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scanner")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    val testBarcodes = listOf(
        Pair("6281007010015", "حليب المراعي طازج 1 لتر (EAN-13)"),
        Pair("6281007010022", "كرتون حليب المراعي (12 حبة)"),
        Pair("210001017508", "طماطم بلدي - باركود ميزان (وزن 1.750 كجم)"),
        Pair("210002008503", "خيار محلي - باركود ميزان (وزن 0.850 كجم)"),
        Pair("0005", "خبز صامولي طازج (كود داخلي)"),
        Pair("0006", "تميس ساخن (كود سريع)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ماسح الباركود بالكاميرا",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "الفلاش",
                        tint = if (torchEnabled) Color(0xFFEAB308) else Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // شاشة إطار الكاميرا الحية مع خط الليزر
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    // بث الكاميرا الحقيقية ومحلل الباركود
                    LiveCameraBarcodePreview(
                        torchEnabled = torchEnabled,
                        onBarcodeScanned = onBarcodeScanned,
                        modifier = Modifier.fillMaxSize()
                    )

                    // إطار الهدف الأخضر
                    Box(
                        modifier = Modifier
                            .size(190.dp, 120.dp)
                            .border(2.5.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                    )

                    // خط الليزر المتحرك
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = (45 + (laserPosition * 140)).dp)
                            .background(Color(0xFFEF4444))
                    )

                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "وجّه الكاميرا نحو رمز الباركود لمسحه تلقائياً",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // إدخال يدوي سريع
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("أدخل أو الصق الباركود") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (manualInput.isNotBlank()) {
                            playBeepSound()
                            onBarcodeScanned(manualInput.trim())
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            playBeepSound()
                            onBarcodeScanned(manualInput.trim())
                        }
                    },
                    enabled = manualInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اعتماد الرمز المدخل", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()

                Text(
                    text = "نماذج باركود جاهزة للاختبار الفوري:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                // قائمة الباركود للاختبار السريع
                testBarcodes.forEach { (code, desc) ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playBeepSound()
                                onBarcodeScanned(code)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = desc, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                Text(text = code, fontSize = 11.sp, color = Color(0xFF0F5132), fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF0F5132),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * مكون BarcodeTextField الرئيسي:
 * عبارة عن OutlinedTextField مزود بأيقونة كاميرا في الـ trailingIcon
 * مع زر مسح الكود عند الكتابة، والقدرة على فتح ماسح الكاميرا وتشغيل صوت التنبيه Beep عند المسح.
 */
@Composable
fun BarcodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "الباركود",
    placeholder: String = "امسح أو أدخل الباركود...",
    onScanClick: (() -> Unit)? = null,
    onBarcodeScanned: ((String) -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
    },
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    // إذا كان المكون يدير الماسح بنفسه عبر onBarcodeScanned
    val internalScannerLauncher = if (onScanClick == null && onBarcodeScanned != null) {
        rememberBarcodeScannerLauncher { scannedCode ->
            onValueChange(scannedCode)
            onBarcodeScanned(scannedCode)
        }
    } else null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("barcode_text_field"),
            label = { Text(label) },
            placeholder = { Text(placeholder, fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotEmpty() && !readOnly) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح النص",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // أيقونة الكاميرا لمسح الباركود
                    IconButton(
                        onClick = {
                            if (onScanClick != null) {
                                onScanClick()
                            } else {
                                internalScannerLauncher?.launch()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_barcode_camera_scan")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "مسح بالكاميرا",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = shape
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}
