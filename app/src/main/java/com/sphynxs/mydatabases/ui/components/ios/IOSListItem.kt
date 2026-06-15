package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IOSListItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    showDivider: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal
                )
                
                subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            if (trailingIcon != null) {
                trailingIcon()
                Spacer(Modifier.width(8.dp))
            }
            
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFC6C6C8),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = if (leadingIcon != null) 60.dp else 16.dp)
            )
        }
    }
}
