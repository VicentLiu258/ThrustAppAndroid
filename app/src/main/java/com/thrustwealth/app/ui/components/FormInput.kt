package com.thrustwealth.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thrustwealth.app.ui.theme.ThrustTheme

@Composable
fun FormLabel(
    label: String,
    hint: String? = null,
    rootModifier: Modifier = Modifier
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = rootModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            color = colors.textTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.weight(1f)
        )
        if (hint != null) {
            Text(
                text = hint,
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    rootModifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    val colors = ThrustTheme.colors
    Row(
        modifier = rootModifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.bgInput, RoundedCornerShape(10.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) {
            Text(
                text = prefix,
                color = colors.textTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Box(modifier = Modifier.width(6.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, color = colors.textMuted, fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(colors.brand),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (suffix != null) {
            Box(modifier = Modifier.width(6.dp))
            Text(
                text = suffix,
                color = colors.textTertiary,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
fun FormGroup(
    label: String,
    hint: String? = null,
    rootModifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = rootModifier.fillMaxWidth()) {
        FormLabel(label = label, hint = hint)
        Box(modifier = Modifier.height(4.dp))
        content()
    }
}
