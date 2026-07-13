package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

@Composable
fun IOSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val tokens = LocalDesignTokens.current
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.surfacePrimary)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Normal
                ),
                visualTransformation = if (isPassword && !passwordVisible) 
                    PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = tokens.textTertiary,
                                fontSize = 17.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility 
                                     else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = tokens.iconNormal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                color = tokens.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
