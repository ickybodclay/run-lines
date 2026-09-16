package com.brokenshotgun.runlines.ui.screens

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.brokenshotgun.runlines.model.Script

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadSceneScreen(
    script: Script,
    sceneIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val safeSceneIndex = sceneIndex.coerceIn(0, script.scenes.lastIndex.coerceAtLeast(0))
    var selectedSceneIndex by remember(script.scenes.size, safeSceneIndex) {
        mutableIntStateOf(safeSceneIndex)
    }
    val currentScene = script.scenes.getOrNull(selectedSceneIndex) ?: script.scenes.firstOrNull() ?: return

    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var stopAtSceneEnd by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    fun stopPlayback() {
        textToSpeech?.stop()
        isPlaying = false
        currentLineIndex = -1
        TtsPlaybackController.setPlayingState(
            context = context,
            title = script.name.ifBlank { "Untitled script" },
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
        val scene = script.scenes.getOrNull(selectedSceneIndex) ?: return 0 to 0
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
        val scene = script.scenes.getOrNull(selectedSceneIndex) ?: return ""
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
            title = script.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            playing = false,
            keepNotificationVisible = true
        )
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

    fun speakLineAt(targetSceneIndex: Int, targetLineIndex: Int) {
        val validSceneIndex = targetSceneIndex.coerceIn(0, script.scenes.lastIndex.coerceAtLeast(0))
        val targetScene = script.scenes.getOrNull(validSceneIndex) ?: return

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
        if (nextSceneIndex < script.scenes.size) {
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
                title = script.name.ifBlank { "Untitled script" },
                sceneName = currentScene.name ?: "Untitled scene",
                playing = true,
                progressMaxWords = currentScene.lines.sumOf { wordCount(it.line) },
                progressWords = currentScene.lines.take(currentLineIndex.coerceAtLeast(0) + 1).sumOf { wordCount(it.line) },
                remainingTimeLabel = estimatedRemainingTimeLabel()
            )
            speakLineAt(selectedSceneIndex, startIndex)
        }
    }

    LaunchedEffect(selectedSceneIndex, currentLineIndex, isPlaying, currentScene.name) {
        val (totalWords, spokenWords) = estimatedSceneProgress()
        TtsPlaybackController.setPlayingState(
            context = context,
            title = script.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = script.id,
            sceneIndex = selectedSceneIndex,
            playing = isPlaying,
            keepNotificationVisible = !isPlaying && currentLineIndex >= 0,
            progressMaxWords = totalWords,
            progressWords = spokenWords,
            remainingTimeLabel = estimatedRemainingTimeLabel()
        )
    }

    LaunchedEffect(selectedSceneIndex, script.id, isPlaying) {
        TtsPlaybackController.bindPlayback(
            title = script.name.ifBlank { "Untitled script" },
            sceneName = currentScene.name ?: "Untitled scene",
            scriptId = script.id,
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
                    val activeScene = script.scenes.getOrNull(selectedSceneIndex) ?: return@post
                    if (currentLineIndex + 1 < activeScene.lines.size) {
                        speakLineAt(selectedSceneIndex, currentLineIndex + 1)
                    } else if (!stopAtSceneEnd) {
                        val nextSceneIndex = selectedSceneIndex + 1
                        if (nextSceneIndex < script.scenes.size) {
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
                title = { Text(text = script.name.ifBlank { "Untitled script" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopPlayback()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
            FloatingActionButton(
                onClick = { togglePlayback() }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(script.scenes) { index, scene ->
                        val selected = index == selectedSceneIndex
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (isPlaying) {
                                    stopPlayback()
                                }
                                selectedSceneIndex = index
                                currentLineIndex = -1
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
                LineReadItem(
                    line = line,
                    isSelected = index == currentLineIndex,
                    onClick = {
                        if (isPlaying) {
                            speakLineAt(selectedSceneIndex, index)
                        } else {
                            currentLineIndex = index
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LineReadItem(line: Line, isSelected: Boolean, onClick: () -> Unit) {
    val isActionLine = line.actor == Actor.ACTION

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
