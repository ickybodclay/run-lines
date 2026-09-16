package com.brokenshotgun.runlines.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brokenshotgun.runlines.data.FountainSerializer
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper
import com.brokenshotgun.runlines.model.Script
import java.io.File

@Composable
fun ScriptListScreen(
    dbHelper: ScriptReaderDbHelper,
    onScriptSelected: (Script) -> Unit
) {
    val scripts = remember { mutableStateListOf<Script>().apply { addAll(dbHelper.getScripts()) } }
    val context = LocalContext.current
    var expandedScriptId by remember { mutableStateOf<Long?>(null) }

    fun refreshScripts() {
        scripts.clear()
        scripts.addAll(dbHelper.getScripts())
    }

    fun exportScriptAsFountain(script: Script) {
        val safeName = script.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val rootDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val runLinesDir = File(rootDir, "Run Lines").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val file = File(runLinesDir, "$safeName.fountain")
        file.writeText(FountainSerializer.serialize(script))
        Toast.makeText(context, "Saved Fountain to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        expandedScriptId = null
    }

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
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No scripts found")
            }
        } else {
            LazyColumn {
                itemsIndexed(scripts) { _, script ->
                    Box {
                        ListItem(
                            headlineContent = { Text(text = script.name) },
                            modifier = Modifier.clickable { onScriptSelected(script) }
                        )
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            IconButton(
                                onClick = { expandedScriptId = if (expandedScriptId == script.id) null else script.id }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Script options")
                            }
                            DropdownMenu(
                                expanded = expandedScriptId == script.id,
                                onDismissRequest = { expandedScriptId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Save Fountain") },
                                    onClick = { exportScriptAsFountain(script) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete script") },
                                    onClick = {
                                        dbHelper.deleteScript(script)
                                        refreshScripts()
                                        expandedScriptId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
