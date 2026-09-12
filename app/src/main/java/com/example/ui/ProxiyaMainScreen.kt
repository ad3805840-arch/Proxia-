package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScanResultEntity
import com.example.ui.components.CameraPreview
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberSky
import com.example.ui.theme.DarkSlate700
import com.example.ui.theme.DarkSlate800
import com.example.ui.theme.DarkSlate900
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TestSceneGenerator
import com.example.util.TestSceneType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProxiyaMainScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScanning by viewModel.isScanning.collectAsState()
    val intervalSeconds by viewModel.intervalSeconds.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedScene by viewModel.selectedScene.collectAsState()
    val customPrompt by viewModel.customPrompt.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val scanCount by viewModel.scanCount.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var isTorchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showCustomPromptInput by remember { mutableStateOf(false) }

    // Text To Speech Initialization
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsEngine: TextToSpeech? = null
        ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = Locale.FRENCH
            }
        }
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    // Capture bitmap lambda
    val captureFrameLambda: () -> Bitmap? = remember(activePreviewView, selectedScene) {
        {
            if (selectedScene == TestSceneType.CAMERA_REAL) {
                activePreviewView?.bitmap
            } else {
                TestSceneGenerator.generateBitmap(selectedScene)
            }
        }
    }

    // Auto-scroll logic for analysis box
    val resultScrollState = rememberScrollState()
    LaunchedEffect(analysisState) {
        resultScrollState.animateScrollTo(resultScrollState.maxValue)
    }

    // Scanner beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "scannerBeam")
    val beamYRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamY"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_container"),
        color = DarkSlate900
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) GlowingGreen else TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Proxiya Live Scanner",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberSky
                    )
                }

                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier.testTag("history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Historique des analyses",
                        tint = CyberSky
                    )
                }
            }

            // Mode Selection Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ScanMode.entries) { mode ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setMode(mode) },
                        label = {
                            Text("${mode.iconName} ${mode.displayName}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSlate800,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("scan_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Scene Source Switcher (Camera vs Simulator Test Scenes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source :",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TestSceneType.entries) { scene ->
                        val isSelected = selectedScene == scene
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyberSky.copy(alpha = 0.2f) else DarkSlate800,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberSky) else null,
                            modifier = Modifier.clickable { viewModel.setScene(scene) }
                        ) {
                            Text(
                                text = "${scene.icon} ${scene.label.take(15)}",
                                fontSize = 11.sp,
                                color = if (isSelected) CyberSky else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Camera / Video Container (.video-container from prompt)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isScanning) CyberSky else DarkSlate700
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (selectedScene == TestSceneType.CAMERA_REAL) {
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreview(
                                isTorchOn = isTorchOn,
                                useFrontCamera = useFrontCamera,
                                onPreviewViewCreated = { view -> activePreviewView = view }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Caméra inactive",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text("Autoriser la caméra")
                                }
                            }
                        }
                    } else {
                        // Simulated Scene Preview
                        val bitmap = remember(selectedScene) {
                            TestSceneGenerator.generateBitmap(selectedScene)
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Scène simulée ${selectedScene.label}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scanner Laser Beam Overlay when scanning is active
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    drawContent()
                                    val y = size.height * beamYRatio
                                    drawLine(
                                        brush = Brush.verticalGradient(
                                            listOf(CyberSky.copy(alpha = 0.8f), NeonCyan)
                                        ),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 4.dp.toPx()
                                    )
                                }
                        )
                    }

                    // Camera Controls Overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedScene == TestSceneType.CAMERA_REAL && cameraPermissionState.status.isGranted) {
                            IconButton(
                                onClick = { isTorchOn = !isTorchOn },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkSlate900.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flash",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkSlate900.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwitchCamera,
                                    contentDescription = "Changer caméra",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Scan count badge
                    if (isScanning && scanCount > 0) {
                        Surface(
                            color = ElectricBlue,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Scan #$scanCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Results Container (#resultat-analyse from prompt HTML)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .testTag("result_display_card"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header of result box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Résultat d'analyse en direct",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberSky
                        )

                        // Action icons for copying / reading out result
                        if (analysisState is AnalysisState.Success) {
                            val textToCopy = (analysisState as AnalysisState.Success).text
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        tts?.speak(textToCopy, TextToSpeech.QUEUE_FLUSH, null, null)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Lecture audio",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Analyse Proxiya", textToCopy)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Analyse copiée !", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copier",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Result Body Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(resultScrollState)
                    ) {
                        when (val state = analysisState) {
                            AnalysisState.Idle -> {
                                Text(
                                    text = "En attente du lancement de l'analyse en direct...",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            is AnalysisState.Analyzing -> {
                                Column {
                                    Text(
                                        text = "Système actif : Analyse #${state.scanNumber} en cours...",
                                        color = CyberSky,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Traitement par l'IA Gemini Vision en cours...",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            is AnalysisState.Success -> {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Dernière analyse (#${state.scanNumber}) :",
                                            color = GlowingGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(state.timestamp)),
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.text,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                            is AnalysisState.Error -> {
                                Column {
                                    Text(
                                        text = "Erreur d'analyse :",
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.message,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Frequency Interval Selector (3s, 5s, 10s, Snapshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intervalle :",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 5, 10).forEach { sec ->
                        val isSelected = intervalSeconds == sec
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ElectricBlue else DarkSlate800,
                            modifier = Modifier
                                .testTag("interval_${sec}s_chip")
                                .clickable { viewModel.setInterval(sec) }
                        ) {
                            Text(
                                text = "${sec}s",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Single Instant Snapshot button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSlate800,
                        modifier = Modifier.clickable {
                            viewModel.triggerSingleScan(captureFrameLambda)
                        }
                    ) {
                        Text(
                            text = "Instant ⚡",
                            fontSize = 12.sp,
                            color = CyberSky,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Main Action Button (matches button #toggleBtn in prompt)
            Button(
                onClick = {
                    viewModel.toggleScanning(captureFrameLambda)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(vertical = 4.dp)
                    .testTag("toggle_analysis_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) AlertRed else ElectricBlue
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Arrêter l'analyse" else "Démarrer l'analyse (${intervalSeconds}s)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }

        // History Bottom Sheet
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = DarkSlate800,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historique des Analyses (${historyList.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberSky
                        )

                        if (historyList.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearHistory() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Effacer l'historique",
                                    tint = AlertRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune analyse enregistrée pour l'instant.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(historyList, key = { it.id }) { scan ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = DarkSlate900),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Surface(
                                                color = ElectricBlue.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = scan.mode,
                                                    fontSize = 11.sp,
                                                    color = CyberSky,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp)),
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = scan.analysisText,
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.deleteScan(scan.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Supprimer",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
