package com.brokenshotgun.runlines.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper
import com.brokenshotgun.runlines.model.Script

@Composable
fun ScriptListScreen(
    dbHelper: ScriptReaderDbHelper,
    onScriptSelected: (Script) -> Unit
) {
    val scripts = remember { mutableStateListOf<Script>().apply { addAll(dbHelper.getScripts()) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Scripts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (scripts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(text = "No scripts found")
            }
        } else {
            LazyColumn {
                itemsIndexed(scripts) { index, script ->
                    ListItem(
                        headlineContent = { Text(text = script.name) },
                        modifier = Modifier.clickable { onScriptSelected(script) }
                    )
                }
            }
        }
    }
}
