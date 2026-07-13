package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

@Composable
fun IOSGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LocalDesignTokens.current.surfacePrimary,
        shadowElevation = 0.dp
    ) {
        Column {
            content()
        }
    }
}
