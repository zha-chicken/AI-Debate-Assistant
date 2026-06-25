@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.MBTIType
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.presentation.common.TopLevelBottomBar
import com.aidebate.presentation.common.TopLevelDestination
import com.aidebate.presentation.localization.*
import com.aidebate.presentation.theme.*

@Composable
fun SettingsScreen(
    onProviderSelected: (String) -> Unit,
    onBack: () -> Unit,
    onDonation: () -> Unit = {},
    onHome: () -> Unit = onBack,
    onHistory: () -> Unit = {},
    onTools: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    LaunchedEffect(Unit) { viewModel.loadProviders() }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.settingsTitle) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            },
            bottomBar = {
                TopLevelBottomBar(
                    selected = TopLevelDestination.Settings,
                    onHome = onHome,
                    onHistory = onHistory,
                    onTools = onTools,
                    onSettings = {},
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(
                        onClick = onDonation,
                        modifier = Modifier.fillMaxWidth(),
                        accent = WarmGlow
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = WarmGlow.copy(alpha = 0.16f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = WarmGlow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    t.supportDevelopment,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    t.supportSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }

                item {
                    SectionHeader(t.sectionGeneral)
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                t.language,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.currentLanguage == LANG_ENGLISH,
                                    onClick = { viewModel.setLanguage(LANG_ENGLISH) },
                                    label = { Text(t.english) }
                                )
                                FilterChip(
                                    selected = uiState.currentLanguage == LANG_CHINESE,
                                    onClick = { viewModel.setLanguage(LANG_CHINESE) },
                                    label = { Text(t.chinese) }
                                )
                            }
                        }
                    }
                }

                item {
                    MbtiCard(
                        selected = uiState.selectedMbti,
                        onSelected = viewModel::setMbti,
                        t = t
                    )
                }

                item {
                    SectionHeader(t.aiProviders)
                }

                item {
                    Text(
                        t.aiProvidersSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(uiState.providers) { config ->
                    ProviderCard(
                        config = config,
                        onClick = { onProviderSelected(config.provider.name) },
                        t = t
                    )
                }
            }
        }
    }
}

@Composable
private fun MbtiCard(
    selected: MBTIType?,
    onSelected: (MBTIType?) -> Unit,
    t: Translation
) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Tertiary) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                t.mbti,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                t.mbtiSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radii.mediumShape
                ) {
                    Text(selected?.name ?: t.notSet, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(t.notSet) },
                        onClick = {
                            onSelected(null)
                            expanded = false
                        }
                    )
                    MBTIType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                onSelected(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun ProviderCard(
    config: ProviderConfig,
    onClick: () -> Unit,
    t: Translation
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accent = if (config.isEnabled) Tertiary else MaterialTheme.colorScheme.error
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Adb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        config.provider.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (config.isEnabled)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (config.isEnabled) t.enabled else t.disabled,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (config.isEnabled) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (config.isEnabled) {
                    Text(
                        config.modelName.ifBlank { t.defaultModel },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        t.tapToConfigure,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
