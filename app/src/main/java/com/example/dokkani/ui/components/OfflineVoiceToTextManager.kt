package com.example.dokkani.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * حالة الإدخال الصوتي المحلي (Offline Voice Recognition State)
 */
data class VoiceToTextState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val error: String? = null
)

/**
 * مدير التعرف الصوتي المحلي بدون إنترنت المعتمد على Android SpeechRecognizer الأصلية
 * (Offline SpeechRecognizer Manager)
 */
class OfflineVoiceToTextManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow(VoiceToTextState())
    val state: StateFlow<VoiceToTextState> = _state.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@OfflineVoiceToTextManager)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "تعذر تهيئة محرك الصوت المحلي")
            }
        }
    }

    /**
     * بدء استماع المحرك الصوتي مع تفعيل خيار العمل بدون إنترنت EXTRA_PREFER_OFFLINE
     */
    fun startListening(languageCode: String = "ar-SA", onResult: (String) -> Unit) {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        if (speechRecognizer == null) {
            _state.value = _state.value.copy(
                error = "خدمة التعرف الصوتي غير متوفرة على هذا الجهاز"
            )
            Toast.makeText(context, "التعرف الصوتي غير متوفر على هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }

        this.onResultCallback = onResult
        _state.value = VoiceToTextState(isListening = true, error = null)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            
            // الضابط الرئيسي: إجبار محرك الصوت على العمل أوفلاين بدون إنترنت (OFFLINE ONLY)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث باللغة العربية للتحويل إلى نص...")
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                error = "فشل بدء الاستماع الصوتي: ${e.localizedMessage}"
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _state.value = _state.value.copy(isListening = false)
    }

    /**
     * إنهاء وتفريغ الموارد لمنع تسريب الذاكرة (Memory Leak Prevention)
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.value = _state.value.copy(isListening = true, error = null)
    }

    override fun onBeginningOfSpeech() {
        _state.value = _state.value.copy(isListening = true)
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _state.value = _state.value.copy(isListening = false)
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
            SpeechRecognizer.ERROR_CLIENT -> "خطأ في الاتصال بالعميل"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يلزم منح إذن الميكروفون"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "تنبيه: العمل بالنموذج الصوتي المحلي أوفلاين"
            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت، يرجى التحدث بوضوح"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "المحرك الصوتي مشغول حالياً، حاول مجدداً"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهى وقت التحدث دون التقاط صوت"
            else -> "توقف الاستماع الصوتي ($error)"
        }
        _state.value = _state.value.copy(isListening = false, error = errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0].trim()
            _state.value = _state.value.copy(
                isListening = false,
                finalText = spokenText,
                partialText = ""
            )
            onResultCallback?.invoke(spokenText)
        } else {
            _state.value = _state.value.copy(isListening = false)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _state.value = _state.value.copy(partialText = matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

/**
 * Composable Helper لإتاحة OfflineVoiceToTextManager مع التدمير التلقائي عند خروج الشاشة
 */
@Composable
fun rememberVoiceToTextManager(): OfflineVoiceToTextManager {
    val context = LocalContext.current
    val manager = remember(context) { OfflineVoiceToTextManager(context) }
    DisposableEffect(manager) {
        onDispose {
            manager.destroy()
        }
    }
    return manager
}

/**
 * زر الميكروفون التفاعلي للإدخال الصوتي المحلي (Offline Voice Input Button)
 * مع إدارة الأذونات تلقائياً وتأثير الحركة البصرية (Pulse Animation)
 */
@Composable
fun VoiceInputIconButton(
    onTextCaptured: (String) -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "ar-SA",
    contentDescription: String = "إدخال صوتي محلي بدون إنترنت"
) {
    val context = LocalContext.current
    val voiceManager = rememberVoiceToTextManager()
    val voiceState by voiceManager.state.collectAsState()

    // طلب إذن الميكروفون ديناميكياً
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening(languageCode, onTextCaptured)
        } else {
            Toast.makeText(context, "يلزم منح إذن تسجيل الصوت لاستخدام الميكروفون المحلي", Toast.LENGTH_LONG).show()
        }
    }

    // حركة النبض المضيء عند الاستماع الصوتي
    val infiniteTransition = rememberInfiniteTransition(label = "offline_mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {
                if (voiceState.isListening) {
                    voiceManager.stopListening()
                } else {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        voiceManager.startListening(languageCode, onTextCaptured)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = modifier
                .size(36.dp)
                .scale(if (voiceState.isListening) pulseScale else 1f)
                .testTag("voice_input_mic_button")
        ) {
            Surface(
                shape = CircleShape,
                color = if (voiceState.isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (voiceState.isListening) Icons.Default.Mic else Icons.Default.Mic,
                        contentDescription = contentDescription,
                        tint = if (voiceState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // إظهار النص الجزئي أثناء التحدث المباشر
        if (voiceState.isListening && voiceState.partialText.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = voiceState.partialText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * حقل إدخال نصوص مدعوم بزر الميكروفون المحلي الصوتي (Voice Outlined Text Field)
 */
@Composable
fun VoiceOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 4,
    appendVoiceText: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        maxLines = maxLines,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                VoiceInputIconButton(
                    onTextCaptured = { spoken ->
                        if (appendVoiceText && value.isNotBlank()) {
                            onValueChange("$value $spoken")
                        } else {
                            onValueChange(spoken)
                        }
                    }
                )
                trailingIcon?.invoke()
            }
        }
    )
}
