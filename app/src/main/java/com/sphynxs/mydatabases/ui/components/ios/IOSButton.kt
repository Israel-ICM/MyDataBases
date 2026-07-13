package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IOSButtonStyle = IOSButtonStyle.Primary
) {
    val backgroundColor = when (style) {
        IOSButtonStyle.Primary -> if (enabled) LocalDesignTokens.current.accentPrimary else LocalDesignTokens.current.accentPrimary.copy(alpha = 0.3f)
        IOSButtonStyle.Secondary -> Color.Transparent
        IOSButtonStyle.Destructive -> if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    }
    
    val textColor = when (style) {
        IOSButtonStyle.Primary -> Color.White
        IOSButtonStyle.Secondary -> LocalDesignTokens.current.accentPrimary
        IOSButtonStyle.Destructive -> Color.White
    }
    
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (style == IOSButtonStyle.Secondary) 
            BorderStroke(1.dp, LocalDesignTokens.current.accentPrimary) else null
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

enum class IOSButtonStyle {
    Primary,
    Secondary,
    Destructive
}
