package com.brokenshotgun.runlines.ui.screens

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brokenshotgun.runlines.model.Actor
import com.brokenshotgun.runlines.model.Line
import com.brokenshotgun.runlines.model.Scene
import com.brokenshotgun.runlines.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadSceneScreen(
    script: Script,
    sceneIndex: Int,
    onBack: () -> Unit,
    onSaveScript: (Script) -> Unit = {}
) {
    var scriptState by remember(script.id, script.name, script.scenes.size) {
        mutableStateOf(script)
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val safeSceneIndex = sceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
    var selectedSceneIndex by remember(scriptState.scenes.size, safeSceneIndex) {
        mutableIntStateOf(safeSceneIndex)
    }
    val currentScene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: scriptState.scenes.firstOrNull() ?: return

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var stopAtSceneEnd by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var unsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var editingActorName by remember { mutableStateOf("") }
    var editingLineText by remember { mutableStateOf("") }
    var originalEditingActorName by remember { mutableStateOf("") }
    var originalEditingLineText by remember { mutableStateOf("") }
    val maxHistoryActions = 500
    var sceneUndoStack by remember(selectedSceneIndex) { mutableStateOf<List<List<Line>>>(emptyList()) }
    var sceneRedoStack by remember(selectedSceneIndex) { mutableStateOf<List<List<Line>>>(emptyList()) }

    fun pushUndoState(snapshot: List<Line>) {
        sceneUndoStack = (sceneUndoStack + listOf(snapshot)).takeLast(maxHistoryActions)
        if (sceneUndoStack.size > maxHistoryActions) {
            sceneUndoStack = sceneUndoStack.takeLast(maxHistoryActions)
        }
    }

    fun snapshotSceneLines(scene: Scene): List<Line> {
        return scene.lines.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
    }

    fun applySceneLines(scene: Scene, lines: List<Line>) {
        scene.lines.clear()
        scene.lines.addAll(lines.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) })
    }

    fun resetSceneEditHistory() {
        sceneUndoStack = emptyList()
        sceneRedoStack = emptyList()
    }

    fun cloneScriptState(): Script {
        return Script(
            name = scriptState.name,
            credit = scriptState.credit,
            author = scriptState.author,
            source = scriptState.source,
            draftDate = scriptState.draftDate,
            contact = scriptState.contact,
            actors = scriptState.actors.map { it.copy() }.toMutableList(),
            scenes = scriptState.scenes.map { scene ->
                scene.copy(
                    name = scene.name,
                    number = scene.number,
                    lines = scene.lines.map { line ->
                        line.copy(
                            actor = line.actor.copy(),
                            line = line.line,
                            order = line.order,
                            characterExtensions = line.characterExtensions.toMutableList()
                        )
                    }.toMutableList()
                )
            }.toMutableList(),
            allVoices = scriptState.allVoices.toMutableList(),
            actorVoices = scriptState.actorVoices.toMutableMap(),
            id = scriptState.id
        ).apply {
            defaultVoice = scriptState.defaultVoice
        }
    }

    fun refreshScriptState() {
        scriptState = cloneScriptState()
    }

    fun isEditingDirty(): Boolean {
        return editingLineIndex != null && (editingActorName != originalEditingActorName || editingLineText != originalEditingLineText)
    }

    fun exitEditMode() {
        editingLineIndex = null
        editingActorName = ""
        editingLineText = ""
        originalEditingActorName = ""
        originalEditingLineText = ""
        unsavedChanges = false
    }

    fun saveAndLeave() {
        onSaveScript(scriptState)
        showExitDialog = false
        exitEditMode()
        onBack()
    }

    fun requestBack() {
        if (editingLineIndex != null && isEditingDirty()) {
            showExitDialog = true
        } else {
            if (editingLineIndex != null) {
                exitEditMode()
            }
            onBack()
        }
    }

    fun stopPlayback() {
        textToSpeech?.stop()
        isPlaying = false
        currentLineIndex = -1
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            playing = false,
            keepNotificationVisible = false
        )
    }

    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        var count = 0
        val parts = text.split(Regex("\\s+"))
        for (part in parts) {
            if (part.isNotBlank()) {
                count += 1
            }
        }
        return count
    }

    fun estimatedSceneProgress(): Pair<Int, Int> {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return 0 to 0
        var spokenWords = 0
        var remainingWords = 0
        val startIndex = currentLineIndex.coerceAtLeast(0)
        for (index in scene.lines.indices) {
            val lineWords = wordCount(scene.lines[index].line)
            if (index <= startIndex) {
                spokenWords += lineWords
            } else {
                remainingWords += lineWords
            }
        }
        val totalWords = (spokenWords + remainingWords).coerceAtLeast(1)
        return totalWords to spokenWords.coerceAtLeast(0)
    }

    fun estimatedRemainingTimeLabel(): String {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return ""
        if (!isPlaying) return ""
        var remainingWords = 0
        val startIndex = currentLineIndex.coerceAtLeast(0)
        for (index in scene.lines.indices) {
            if (index > startIndex) {
                remainingWords += wordCount(scene.lines[index].line)
            }
        }
        val wordsPerSecond = 2.5
        val secondsLeft = (remainingWords / wordsPerSecond).toLong().coerceAtLeast(0)
        val minutes = secondsLeft / 60
        val secs = secondsLeft % 60
        return if (minutes > 0) "${minutes}m ${secs}s left" else "${secs}s left"
    }

    fun pausePlayback() {
        textToSpeech?.stop()
        isPlaying = false
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            playing = false,
            keepNotificationVisible = true
        )
    }

    fun renameActorAcrossScript(currentActor: Actor, newName: String) {
        if (newName.isBlank()) return
        val replacement = Actor(newName)
        if (currentActor == Actor.ACTION) return

        scriptState.replaceActor(currentActor, replacement)
        val actorIndex = scriptState.actors.indexOf(currentActor)
        if (actorIndex >= 0) {
            scriptState.actors[actorIndex] = replacement
        }
        refreshScriptState()
    }

    fun beginEditingLine(lineIndex: Int) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return
        val line = scene.lines[lineIndex]
        editingLineIndex = lineIndex
        editingActorName = line.actor.name.uppercase()
        editingLineText = line.line
        originalEditingActorName = line.actor.name.uppercase()
        originalEditingLineText = line.line
        unsavedChanges = false
    }

    fun saveEditingLineChanges(lineIndex: Int, actorName: String, lineText: String) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return

        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val currentLine = updatedScene.lines.getOrNull(lineIndex) ?: return
        val beforeSnapshot = snapshotSceneLines(updatedScene)
        val normalizedActorName = actorName.trim().uppercase()

        if (normalizedActorName.isNotBlank()) {
            val existingActor = updatedScript.actors.firstOrNull { it.name.equals(normalizedActorName, ignoreCase = true) }
            val targetActor = existingActor ?: Actor(normalizedActorName).also { updatedScript.actors.add(it) }

            if (currentLine.actor == Actor.ACTION) {
                currentLine.actor = targetActor
            } else if (!currentLine.actor.name.equals(targetActor.name, ignoreCase = true)) {
                updatedScript.replaceActor(currentLine.actor, targetActor)
                currentLine.actor = targetActor
            } else {
                currentLine.actor = targetActor
            }
        }
        if (currentLine.line != lineText) {
            currentLine.line = lineText
        }

        val afterSnapshot = snapshotSceneLines(updatedScene)
        if (beforeSnapshot != afterSnapshot) {
            pushUndoState(beforeSnapshot)
            sceneRedoStack = emptyList()
        }

        scriptState = updatedScript
        onSaveScript(scriptState)
        exitEditMode()
    }

    fun undoSceneEdit() {
        val previous = sceneUndoStack.lastOrNull() ?: return
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return

        sceneRedoStack = (sceneRedoStack + listOf(snapshotSceneLines(updatedScene))).takeLast(maxHistoryActions)
        sceneUndoStack = sceneUndoStack.dropLast(1)

        val newLines = previous.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
        updatedScene.lines.clear()
        updatedScene.lines.addAll(newLines)

        scriptState = updatedScript
        onSaveScript(scriptState)
    }

    fun redoSceneEdit() {
        val next = sceneRedoStack.lastOrNull() ?: return
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return

        sceneUndoStack = (sceneUndoStack + listOf(snapshotSceneLines(updatedScene))).takeLast(maxHistoryActions)
        sceneRedoStack = sceneRedoStack.dropLast(1)

        val newLines = next.map { it.copy(actor = it.actor, line = it.line, order = it.order, characterExtensions = it.characterExtensions.toMutableList()) }
        updatedScene.lines.clear()
        updatedScene.lines.addAll(newLines)

        scriptState = updatedScript
        onSaveScript(scriptState)
    }

    fun deleteLineAndSave(lineIndex: Int) {
        val scene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return
        if (lineIndex !in scene.lines.indices) return

        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val beforeSnapshot = snapshotSceneLines(updatedScene)
        pushUndoState(beforeSnapshot)
        sceneRedoStack = emptyList()
        updatedScene.lines.removeAt(lineIndex)

        scriptState = updatedScript
        onSaveScript(scriptState)
        exitEditMode()
    }

    fun addNewLineToBottom() {
        val updatedScript = cloneScriptState()
        val updatedScene = updatedScript.scenes.getOrNull(selectedSceneIndex) ?: return
        val newLine = Line(Actor.ACTION, "")
        updatedScene.lines.add(newLine)
        val newIndex = updatedScene.lines.lastIndex
        scriptState = updatedScript
        beginEditingLine(newIndex)
        onSaveScript(scriptState)
    }

    fun cancelEditingLine() {
        if (isEditingDirty()) {
            showExitDialog = true
        } else {
            exitEditMode()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stopPlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        requestBack()
    }

    fun speakLineAt(targetSceneIndex: Int, targetLineIndex: Int) {
        val validSceneIndex = targetSceneIndex.coerceIn(0, scriptState.scenes.lastIndex.coerceAtLeast(0))
        val targetScene = scriptState.scenes.getOrNull(validSceneIndex) ?: return

        if (targetLineIndex < targetScene.lines.size) {
            selectedSceneIndex = validSceneIndex
            currentLineIndex = targetLineIndex
            val line = targetScene.lines[targetLineIndex]
            textToSpeech?.speak(line.line, TextToSpeech.QUEUE_FLUSH, null, "scene_${validSceneIndex}_line_$targetLineIndex")
            return
        }

        if (stopAtSceneEnd) {
            stopPlayback()
            return
        }

        val nextSceneIndex = validSceneIndex + 1
        if (nextSceneIndex < scriptState.scenes.size) {
            speakLineAt(nextSceneIndex, 0)
        } else {
            stopPlayback()
        }
    }

    fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            isPlaying = true
            val startIndex = if (currentLineIndex == -1) 0 else currentLineIndex
            TtsPlaybackController.setPlayingState(
                context = context,
                title = scriptState.name.ifBlank { "Untitled script" },
                sceneName = currentScene.name ?: "Untitled scene",
                playing = true,
                progressMaxWords = currentScene.lines.sumOf { wordCount(it.line) },
                progressWords = currentScene.lines.take(currentLineIndex.coerceAtLeast(0) + 1).sumOf { wordCount(it.line) },
                remainingTimeLabel = estimatedRemainingTimeLabel()
            )
            speakLineAt(selectedSceneIndex, startIndex)
        }
    }

    LaunchedEffect(script) {
        scriptState = script
    }

    LaunchedEffect(selectedSceneIndex, currentLineIndex, isPlaying, currentScene.name) {
        val (totalWords, spokenWords) = estimatedSceneProgress()
        TtsPlaybackController.setPlayingState(
            context = context,
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            keepNotificationVisible = !isPlaying && currentLineIndex >= 0,
            progressMaxWords = totalWords,
            progressWords = spokenWords,
            remainingTimeLabel = estimatedRemainingTimeLabel()
        )
    }

    LaunchedEffect(selectedSceneIndex, scriptState.id, isPlaying) {
        TtsPlaybackController.bindPlayback(
            title = scriptState.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = scriptState.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            onToggle = { togglePlayback() },
            onStop = { stopPlayback() }
        )
    }

    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("ReadSceneScreen", "TTS Initialized")
            }
        }
    }

    LaunchedEffect(textToSpeech) {
        textToSpeech?.setOnUtteranceProgressListener(ReadSceneTTSListener {
            if (isPlaying) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val activeScene = scriptState.scenes.getOrNull(selectedSceneIndex) ?: return@post
                    if (currentLineIndex + 1 < activeScene.lines.size) {
                        speakLineAt(selectedSceneIndex, currentLineIndex + 1)
                    } else if (!stopAtSceneEnd) {
                        val nextSceneIndex = selectedSceneIndex + 1
                        if (nextSceneIndex < scriptState.scenes.size) {
                            speakLineAt(nextSceneIndex, 0)
                        } else {
                            stopPlayback()
                        }
                    } else {
                        stopPlayback()
                    }
                }
            }
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = scriptState.name.ifBlank { "Untitled script" }) },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val canUndo = sceneUndoStack.isNotEmpty()
                    val canRedo = sceneRedoStack.isNotEmpty()

                    IconButton(
                        onClick = { undoSceneEdit() },
                        enabled = canUndo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo edit",
                            tint = if (canUndo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = { redoSceneEdit() },
                        enabled = canRedo
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo edit",
                            tint = if (canRedo) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Scene options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Stop at scene end") },
                            trailingIcon = {
                                Checkbox(
                                    checked = stopAtSceneEnd,
                                    onCheckedChange = null
                                )
                            },
                            onClick = {
                                stopAtSceneEnd = !stopAtSceneEnd
                                menuExpanded = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { addNewLineToBottom() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add line"
                    )
                }

                FloatingActionButton(
                    onClick = { togglePlayback() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(scriptState.scenes) { index, scene ->
                        val selected = index == selectedSceneIndex
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (isPlaying) {
                                    stopPlayback()
                                }
                                selectedSceneIndex = index
                                currentLineIndex = -1
                                editingLineIndex = null
                                resetSceneEditHistory()
                            },
                            label = {
                                Text(
                                    text = "Scene ${index + 1}${scene.name?.let { " • ${it}" } ?: ""}",
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = currentScene.name?.uppercase() ?: "UNTITLED SCENE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            itemsIndexed(currentScene.lines) { index, line ->
                LineEditRow(
                    line = line,
                    isSelected = index == currentLineIndex,
                    isEditing = editingLineIndex == index,
                    editingActorName = if (editingLineIndex == index) editingActorName else line.actor.name.uppercase(),
                    editingLineText = if (editingLineIndex == index) editingLineText else line.line,
                    characterSuggestions = scriptState.actors
                        .map { it.name }
                        .filter { it != Actor.ACTION_NAME }
                        .distinct()
                        .sorted(),
                    onClick = {
                        if (isPlaying) {
                            speakLineAt(selectedSceneIndex, index)
                        } else {
                            currentLineIndex = index
                        }
                    },
                    onLongClick = {
                        beginEditingLine(index)
                    },
                    onActorValueChange = {
                        editingActorName = it.uppercase()
                    },
                    onLineValueChange = {
                        editingLineText = it
                    },
                    onSave = {
                        saveEditingLineChanges(index, editingActorName, editingLineText)
                        currentLineIndex = index
                    },
                    onCancel = {
                        cancelEditingLine()
                    },
                    onDelete = {
                        deleteLineAndSave(index)
                    }
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("You have edits that haven't been saved. Save them before leaving or discard them.") },
            confirmButton = {
                TextButton(onClick = {
                    if (editingLineIndex != null) {
                        saveEditingLineChanges(editingLineIndex!!, editingActorName, editingLineText)
                    }
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Save & leave")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        exitEditMode()
                        onBack()
                    }) {
                        Text("Discard")
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Keep editing")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineEditRow(
    line: Line,
    isSelected: Boolean,
    isEditing: Boolean,
    editingActorName: String,
    editingLineText: String,
    characterSuggestions: List<String>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActorValueChange: (String) -> Unit,
    onLineValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val isActionLine = line.actor == Actor.ACTION
    var expanded by remember { mutableStateOf(false) }
    val matchingSuggestions = remember(editingActorName, characterSuggestions) {
        val query = editingActorName.trim()
        if (query.isBlank()) {
            characterSuggestions
        } else {
            characterSuggestions.filter { it.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditing) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded && matchingSuggestions.isNotEmpty(),
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = editingActorName,
                        onValueChange = {
                            expanded = true
                            onActorValueChange(it)
                        },
                        label = { Text("Character") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && matchingSuggestions.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        matchingSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onActorValueChange(suggestion)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editingLineText,
                    onValueChange = onLineValueChange,
                    label = { Text("Line") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text("Save")
                    }
                }
            }
        } else {
            if (!isActionLine) {
                Text(
                    text = line.actor.name.uppercase(),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (line.line.isNotBlank()) {
                Text(
                    text = line.line,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = if (isActionLine) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
