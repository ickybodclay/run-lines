package com.brokenshotgun.runlines.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brokenshotgun.runlines.model.Actor
import com.brokenshotgun.runlines.model.Line
import com.brokenshotgun.runlines.model.Scene
import com.brokenshotgun.runlines.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSceneScreen(
    script: Script,
    sceneIndex: Int,
    onSave: (Script) -> Unit,
    onBack: () -> Unit
) {
    val scene = script.getScene(sceneIndex)
    var showAddActorDialog by remember { mutableStateOf(false) }
    var showAddLineDialog by remember { mutableStateOf(false) }
    var showEditOptionDialog by remember { mutableStateOf<Int?>(null) }
    var showEditNameDialog by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    var newActorName by remember { mutableStateOf("") }
    var newLineText by remember { mutableStateOf("") }
    var editActorName by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        // Initialize editActorName when dialog opens
    }

    // Use a separate effect to sync editActorName when showEditNameDialog changes
    LaunchedEffect(showEditNameDialog) {
        if (showEditNameDialog != null) {
            val index = showEditNameDialog!!
            editActorName = scene.lines[index].actor.name
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Edit Scene: ${scene.name ?: "Untitled"}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddActorDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Actor")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            LazyColumn {
                itemsIndexed(scene.lines) { index, line ->
                    LineEditItem(
                        line = line,
                        index = index,
                        onEditOption = { showEditOptionDialog = index }
                    )
                }
            }
        }

        if (showAddActorDialog) {
            AlertDialog(
                onDismissRequest = { showAddActorDialog = false },
                title = { Text("Add Actor") },
                text = {
                    TextField(
                        value = newActorName,
                        onValueChange = { newActorName = it },
                        label = { Text("Actor Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newActorName.isNotBlank()) {
                            script.addActor(Actor(newActorName))
                            showAddActorDialog = false
                            newActorName = ""
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddActorDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showAddLineDialog) {
            AlertDialog(
                onDismissRequest = { showAddLineDialog = false },
                title = { Text("Add Line") },
                text = {
                    TextField(
                        value = newLineText,
                        onValueChange = { newLineText = it },
                        label = { Text("Line text") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scene.addLine(Line(Actor.ACTION, newLineText))
                        showAddLineDialog = false
                        newLineText = ""
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddLineDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showEditOptionDialog != null) {
            val index = showEditOptionDialog!!
            AlertDialog(
                onDismissRequest = { showEditOptionDialog = null },
                title = { Text("Edit Line") },
                text = { Text("What would you like to do with this line?") },
                confirmButton = {
                    // In a real app we'd have a list of options here.
                    // For simplicity, we'll provide a basic Edit/Delete choice.
                    TextButton(onClick = {
                        showEditNameDialog = index
                        showEditOptionDialog = null
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = index
                        showEditOptionDialog = null
                    }) { Text("Delete") }
                }
            )
        }

        if (showEditNameDialog != null) {
            val index = showEditNameDialog!!
            val currentLine = scene.lines[index]
            AlertDialog(
                onDismissRequest = { 
                    showEditNameDialog = null 
                    editActorName = ""
                },
                title = { Text("Edit Line Actor") },
                text = {
                    TextField(
                        value = editActorName,
                        onValueChange = { editActorName = it },
                        label = { Text("Actor Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editActorName.isNotBlank()) {
                            val updatedLine = currentLine.copy(actor = Actor(editActorName))
                            scene.lines[index] = updatedLine
                        }
                        showEditNameDialog = null
                        editActorName = ""
                    }) { Text("OK") }
                }
            )
        }

        if (showDeleteDialog != null) {
            val index = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Line") },
                text = { Text("Are you sure you want to delete this line?") },
                confirmButton = {
                    TextButton(onClick = {
                        scene.lines.removeAt(index)
                        showDeleteDialog = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun LineEditItem(line: Line, onEditOption: (Int) -> Unit, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditOption(index) }
            .padding(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text = "${line.actor.name}: ${line.line}", modifier = Modifier.weight(1f))
    }
}
