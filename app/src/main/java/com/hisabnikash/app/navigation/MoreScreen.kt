package com.hisabnikash.app.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hisabnikash.app.R

@Composable
fun MoreScreen(
    onBudgetsAndGoals: () -> Unit,
    onSettings: () -> Unit,
    onGoals: () -> Unit = onBudgetsAndGoals,
    onBackup: () -> Unit = onSettings,
    onLanguage: () -> Unit = onSettings,
) {
    val context = LocalContext.current
    var license by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.nav_more), style = MaterialTheme.typography.headlineMedium)
        ListItem(
            headlineContent = { Text(stringResource(R.string.budget_shortcut)) },
            leadingContent = { NavigationGlyph(Route.Savings) }, trailingContent = { Text("›") },
            modifier = Modifier.clickable(onClick = onBudgetsAndGoals),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.savings_goals)) },
            leadingContent = { NavigationGlyph(Route.Savings) }, trailingContent = { Text("›") },
            modifier = Modifier.clickable(onClick = onGoals),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.backup_title)) },
            trailingContent = { Text("›") }, modifier = Modifier.clickable(onClick = onBackup),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.language)) },
            trailingContent = { Text("›") }, modifier = Modifier.clickable(onClick = onLanguage),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_title)) },
            leadingContent = { NavigationGlyph(Route.Settings) }, trailingContent = { Text("›") },
            modifier = Modifier.clickable(onClick = onSettings),
        )
        Text(stringResource(R.string.privacy_display_help), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = {
            license = listOf(
                "Hind Siliguri" to "hind_siliguri_OFL.txt",
                "Noto Sans Bengali" to "noto_sans_bengali_OFL.txt",
            ).joinToString("\n\n") { (family, asset) ->
                family + "\n\n" + context.assets.open("licenses/$asset").bufferedReader().use { it.readText() }
            }
        }) { Text(stringResource(R.string.font_licenses)) }
    }
    license?.let { text ->
        AlertDialog(onDismissRequest = { license = null }, title = { Text(stringResource(R.string.font_licenses)) },
            text = { Text(text, Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { license = null }) { Text(stringResource(R.string.got_it)) } })
    }
}
