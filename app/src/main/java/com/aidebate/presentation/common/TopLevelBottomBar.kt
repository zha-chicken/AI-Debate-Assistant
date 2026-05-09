package com.aidebate.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.Radii
import com.aidebate.presentation.theme.RhetorixAccents
import com.aidebate.presentation.theme.RhetorixSurfaces
import com.aidebate.presentation.theme.Spacing

enum class TopLevelDestination {
    Home,
    History,
    Tools,
    Settings,
}

@Composable
fun TopLevelBottomBar(
    selected: TopLevelDestination,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalTranslation.current
    Row(
        modifier = modifier
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .fillMaxWidth()
            .height(58.dp)
            .background(RhetorixSurfaces.GlassRaised, Radii.largeShape)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopLevelBottomItem(
            icon = Icons.Filled.Home,
            label = t.homeTab,
            selected = selected == TopLevelDestination.Home,
            onClick = onHome
        )
        TopLevelBottomItem(
            icon = Icons.Filled.History,
            label = t.debatesTab,
            selected = selected == TopLevelDestination.History,
            onClick = onHistory
        )
        TopLevelBottomItem(
            icon = Icons.Filled.AccountTree,
            label = t.toolsTab,
            selected = selected == TopLevelDestination.Tools,
            onClick = onTools
        )
        TopLevelBottomItem(
            icon = Icons.Filled.Settings,
            label = t.profileTab,
            selected = selected == TopLevelDestination.Settings,
            onClick = onSettings
        )
    }
}

@Composable
private fun TopLevelBottomItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val active = RhetorixAccents.Amber
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) active else inactive,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) active else inactive,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
