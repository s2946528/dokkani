package com.example.dokkani.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.dokkani.util.safeToDouble

/**
 * حقل إدخال رقمي مخصص للكميات والمبالغ المالية في تطبيق "دكاني".
 * - يمسح الصفر تلقائياً بمجرد الضغط أو التركيز على الحقل (Auto-clear on Focus).
 * - يضبط لوحة المفاتيح تلقائياً على KeyboardType.Decimal.
 * - ينظف الإدخالات من الأرقام العربية والفواصل ويمنع الأحرف غير الرقمية.
 * - يضمن محاذاة النص لليمين ليتناسب مع الواجهات العربية والمالية.
 */
@Composable
fun NumericOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    autoClearZeroOnFocus: Boolean = true,
    allowDecimals: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (readOnly) return@OutlinedTextField
            val cleaned = newValue
                .replace(',', '.')
                .replace('٫', '.')
                .replace('٠', '0')
                .replace('١', '1')
                .replace('٢', '2')
                .replace('٣', '3')
                .replace('٤', '4')
                .replace('٥', '5')
                .replace('٦', '6')
                .replace('٧', '7')
                .replace('٨', '8')
                .replace('٩', '9')
            
            val regex = if (allowDecimals) Regex("^-?\\d*(\\.\\d*)?$") else Regex("^-?\\d*$")
            if (cleaned.isEmpty() || cleaned.matches(regex)) {
                onValueChange(cleaned)
            }
        },
        modifier = modifier.onFocusChanged { focusState ->
            val nowFocused = focusState.isFocused
            if (nowFocused && !isFocused && autoClearZeroOnFocus && !readOnly) {
                val trimmed = value.trim()
                if (trimmed == "0" || trimmed == "0.0" || trimmed == "0.00" || trimmed == "0.000" || (trimmed.isNotEmpty() && trimmed.safeToDouble() == 0.0)) {
                    onValueChange("")
                }
            }
            isFocused = nowFocused
        },
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        shape = shape,
        colors = colors,
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly
    )
}
