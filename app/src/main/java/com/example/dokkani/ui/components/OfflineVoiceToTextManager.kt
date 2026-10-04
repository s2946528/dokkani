package com.example.dokkani.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
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
 * حالة الإدخال الصوتي المحلي والإنشائي (Voice Recognition State)
 */
data class VoiceToTextState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val error: String? = null
)

/**
 * مدير التعرف الصوتي المستقر والمحلي المعتمد على Android SpeechRecognizer الأصلية
 * مضمّن بضوابط الخيط الرئيسي (Main Thread Execution) وفلاتر عدم الإغلاق المبكر.
 */
class OfflineVoiceToTextManager(private val context: Context) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(VoiceToTextState())
    val state: StateFlow<VoiceToTextState> = _state.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var startTimeMs: Long = 0L

    init {
        mainHandler.post {
            initSpeechRecognizerOnMainThread()
        }
    }

    private fun initSpeechRecognizerOnMainThread() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@OfflineVoiceToTextManager)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "تعذر تهيئة محرك الصوت")
            }
        }
    }

    /**
     * بدء الاستماع الصوتي مع ضمان التشغيل على خيط الـ Main Thread وتفادي الإغلاق المبكر
     */
    fun startListening(
        languageCode: String = "ar-SA",
        preferOffline: Boolean = true,
        onResult: (String) -> Unit
    ) {
        // فحص الإذن أولاً
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _state.value = _state.value.copy(
                isListening = false,
                error = "يلزم منح إذن تسجيل الصوت أولاً"
            )
            Toast.makeText(context, "يلزم منح إذن تسجيل الصوت لاستخدام الميكروفون", Toast.LENGTH_SHORT).show()
            return
        }

        this.onResultCallback = onResult
        this.startTimeMs = System.currentTimeMillis()

        mainHandler.post {
            try {
                // إلغاء أي جلسة سابقة لمنع خروج ERROR_RECOGNIZER_BUSY
                speechRecognizer?.cancel()

                if (speechRecognizer == null) {
                    initSpeechRecognizerOnMainThread()
                }

                _state.value = VoiceToTextState(isListening = true, error = null)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)

                    if (preferOffline) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }

                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // صيانة وإطالة وقت الانتظار لمنع الإغلاق المبكر دون التحدث
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isListening = false,
                    error = "فشل بدء الاستماع: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _state.value = _state.value.copy(isListening = false)
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
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
        val durationMs = System.currentTimeMillis() - startTimeMs

        // إذا حدث الخطأ بسرعة فائقة (< 800ms) بسبب عدم وجود حزمة اللغة محلياً أوفلاين، نحاول إعادة الاستماع بالوضع العادي
        if (durationMs < 800 && (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_CLIENT)) {
            mainHandler.postDelayed({
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    speechRecognizer?.startListening(intent)
                } catch (_: Exception) {
                    _state.value = _state.value.copy(isListening = false, error = "يرجى التحدث بوضوح")
                }
            }, 300)
            return
        }

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
            SpeechRecognizer.ERROR_CLIENT -> "جاري تجهيز محرك الصوت..."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يلزم منح إذن تسجيل الصوت"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "يرجى التحدث بوضوح"
            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الصوت، يرجى إعادة المحاولة والتحدث بوضوح"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "المحرك مشغول، تم إعداده مجدداً"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم التقاط صوت، اضغط الميكروفون والتحدث مباشرة"
            else -> "توقف الاستماع ($error)"
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
 * زر الميكروفون التفاعلي المستقر للإدخال الصوتي المحلي مع حماية من الفصل السريع
 */
@Composable
fun VoiceInputIconButton(
    onTextCaptured: (String) -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "ar-SA",
    contentDescription: String = "إدخال صوتي محلي"
) {
    val context = LocalContext.current
    val voiceManager = rememberVoiceToTextManager()
    val voiceState by voiceManager.state.collectAsState()

    // مشغل نية نظام الأندرويد الاحتياطية
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                val text = spokenMatches[0].trim()
                onTextCaptured(text)
                Toast.makeText(context, "تم التقاط النص الصوتي: $text", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // طلب إذن الميكروفون ديناميكياً
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening(languageCode, true) { text -> onTextCaptured(text) }
        } else {
            Toast.makeText(context, "يلزم منح إذن تسجيل الصوت لاستخدام الميكروفون", Toast.LENGTH_LONG).show()
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
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@IconButton
                }

                if (voiceState.isListening) {
                    voiceManager.stopListening()
                } else {
                    try {
                        voiceManager.startListening(languageCode, true) { text -> onTextCaptured(text) }
                    } catch (_: Exception) {
                        launchSystemVoiceDialog(context, systemSpeechLauncher)
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
                        imageVector = Icons.Default.Mic,
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

private fun launchSystemVoiceDialog(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث باللغة العربية للتحويل إلى نص...")
        }
        launcher.launch(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر تشغيل الإدخال الصوتي على هذا الجهاز", Toast.LENGTH_SHORT).show()
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
