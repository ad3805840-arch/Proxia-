package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiScannerService
import com.example.data.AppDatabase
import com.example.data.ScanResultEntity
import com.example.util.TestSceneGenerator
import com.example.util.TestSceneType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScanMode(val displayName: String, val defaultPrompt: String, val iconName: String) {
    TRADING(
        "Trading & Bourse",
        "Analyse ce graphique de trading en direct (chandeliers, RSI/MACD, tendance, niveau clé). Donne les probabilités d'évolution et les signaux d'achat/vente.",
        "📈"
    ),
    GAMING(
        "Jeu & Casino",
        "Analyse ce jeu ou cette table de jeu/casino en direct. Évalue l'état actuel, les probabilités de gain et conseille l'action optimale à effectuer.",
        "🎲"
    ),
    SPORTS(
        "Paris Sportifs",
        "Analyse cet écran de paris sportifs ou ce match live. Identifie les équipes, le temps de jeu, les cotes et donne une prédiction sur l'issue.",
        "⚽"
    ),
    GENERAL(
        "Direct Scanner",
        "Analyse cette image de jeu, de paris ou de trading en direct. Donne une lecture rapide des tendances, des configurations ou des probabilités.",
        "⚡"
    )
}

sealed interface AnalysisState {
    object Idle : AnalysisState
    data class Analyzing(val scanNumber: Int) : AnalysisState
    data class Success(val text: String, val timestamp: Long, val scanNumber: Int) : AnalysisState
    data class Error(val message: String, val timestamp: Long) : AnalysisState
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val scanDao = db.scanDao()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _intervalSeconds = MutableStateFlow(3)
    val intervalSeconds: StateFlow<Int> = _intervalSeconds.asStateFlow()

    private val _selectedMode = MutableStateFlow(ScanMode.GENERAL)
    val selectedMode: StateFlow<ScanMode> = _selectedMode.asStateFlow()

    private val _selectedScene = MutableStateFlow(TestSceneType.CAMERA_REAL)
    val selectedScene: StateFlow<TestSceneType> = _selectedScene.asStateFlow()

    private val _customPrompt = MutableStateFlow("")
    val customPrompt: StateFlow<String> = _customPrompt.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _scanCount = MutableStateFlow(0)
    val scanCount: StateFlow<Int> = _scanCount.asStateFlow()

    val historyList: StateFlow<List<ScanResultEntity>> = scanDao.getAllScans()
        .let { flow ->
            val mutable = MutableStateFlow<List<ScanResultEntity>>(emptyList())
            viewModelScope.launch {
                flow.collect { list -> mutable.value = list }
            }
            mutable.asStateFlow()
        }

    private var loopJob: Job? = null

    fun toggleScanning(onCaptureFrame: () -> Bitmap?) {
        if (_isScanning.value) {
            stopScanning()
        } else {
            startScanning(onCaptureFrame)
        }
    }

    fun startScanning(onCaptureFrame: () -> Bitmap?) {
        _isScanning.value = true
        loopJob?.cancel()

        loopJob = viewModelScope.launch {
            while (isActive && _isScanning.value) {
                performScan(onCaptureFrame)
                val delayMs = (_intervalSeconds.value.coerceAtLeast(1)) * 1000L
                delay(delayMs)
            }
        }
    }

    fun stopScanning() {
        _isScanning.value = false
        loopJob?.cancel()
        loopJob = null
    }

    fun triggerSingleScan(onCaptureFrame: () -> Bitmap?) {
        viewModelScope.launch {
            performScan(onCaptureFrame)
        }
    }

    private suspend fun performScan(onCaptureFrame: () -> Bitmap?) {
        val currentCount = _scanCount.value + 1
        _scanCount.value = currentCount
        _analysisState.value = AnalysisState.Analyzing(currentCount)

        val frameBitmap: Bitmap? = if (_selectedScene.value == TestSceneType.CAMERA_REAL) {
            onCaptureFrame()
        } else {
            TestSceneGenerator.generateBitmap(_selectedScene.value)
        }

        val bitmapToAnalyze = frameBitmap ?: TestSceneGenerator.generateBitmap(_selectedScene.value)
        val promptText = if (_customPrompt.value.isNotBlank()) {
            _customPrompt.value
        } else {
            _selectedMode.value.defaultPrompt
        }

        val result = GeminiScannerService.analyzeImage(bitmapToAnalyze, promptText)

        val now = System.currentTimeMillis()
        result.onSuccess { analysisText ->
            _analysisState.value = AnalysisState.Success(analysisText, now, currentCount)
            // Save to Room DB
            scanDao.insertScan(
                ScanResultEntity(
                    timestamp = now,
                    mode = _selectedMode.value.displayName,
                    prompt = promptText,
                    analysisText = analysisText
                )
            )
        }.onFailure { error ->
            _analysisState.value = AnalysisState.Error(
                error.message ?: "Erreur réseau ou d'analyse API",
                now
            )
        }
    }

    fun setInterval(seconds: Int) {
        _intervalSeconds.value = seconds
    }

    fun setMode(mode: ScanMode) {
        _selectedMode.value = mode
    }

    fun setScene(scene: TestSceneType) {
        _selectedScene.value = scene
    }

    fun setCustomPrompt(prompt: String) {
        _customPrompt.value = prompt
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            scanDao.deleteScan(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            scanDao.clearHistory()
        }
    }
}
