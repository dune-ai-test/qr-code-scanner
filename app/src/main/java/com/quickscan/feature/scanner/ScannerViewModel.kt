package com.quickscan.feature.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.data.barcode.DecodedCode
import com.quickscan.data.barcode.PayloadParser
import com.quickscan.data.barcode.PayloadType
import com.quickscan.data.barcode.ScannedPayload
import com.quickscan.data.barcode.ZxingDecoder
import com.quickscan.core.ui.toastLabel
import com.quickscan.data.local.ScanEntity
import com.quickscan.data.repository.ScanRepository
import com.quickscan.data.repository.ScanSource
import com.quickscan.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScannerUiState(
    val torchOn: Boolean = false,
    val usingFrontCamera: Boolean = false,
    val autoDetect: Boolean = true,
    val continuousMode: Boolean = false,
    val copyAutomatically: Boolean = false,
    val scanSound: Boolean = true,
    val vibrate: Boolean = true,
    val recent: List<ScanEntity> = emptyList(),
    val pasteOpen: Boolean = false,
    val pasteText: String = "",
    val busy: Boolean = false,
) {
    val canSubmitPaste: Boolean get() = pasteText.isNotBlank()
}

sealed interface ScannerEvent {
    data class OpenResult(val scanId: Long) : ScannerEvent

    /** Continuous mode: recorded, but the scanner stays where it is. */
    data class SavedInPlace(val type: PayloadType, val label: String) : ScannerEvent

    data class Message(val text: ScannerMessage) : ScannerEvent
    data class CopyToClipboard(val text: String) : ScannerEvent
    data class SignalScanFeedback(val vibrate: Boolean, val playSound: Boolean) : ScannerEvent
}

enum class ScannerMessage {
    NoCodeInImage,
    NothingToDecode,
    NotAValidLink,
    /** The shutter button grabbed a frame with no readable code in it. */
    NothingInViewfinder,
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val settingsRepository: SettingsRepository,
    decoder: ZxingDecoder,
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _events = Channel<ScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Shared with [CameraController] so the camera and the gallery use one decoder. */
    val decoder: ZxingDecoder = decoder

    /** Only consulted in continuous mode; see [RepeatGate]. */
    private val repeatGate = RepeatGate()

    init {
        viewModelScope.launch {
            combine(
                scanRepository.observeScans(),
                settingsRepository.scannerPreferences,
            ) { scans, prefs ->
                _state.update {
                    it.copy(
                        usingFrontCamera = prefs.preferFrontCamera,
                        autoDetect = prefs.autoDetect,
                        continuousMode = prefs.continuousMode,
                        copyAutomatically = prefs.copyAutomatically,
                        scanSound = prefs.scanSound,
                        vibrate = prefs.vibrateOnScan,
                        recent = scans.take(RECENT_LIMIT),
                    )
                }
            }.collect { }
        }
    }

    /** Called from the camera analyzer for every decoded frame. */
    fun onCodeDetected(code: DecodedCode) {
        val snapshot = _state.value
        if (!snapshot.autoDetect) return
        // Continuous mode leaves the camera pointed at whatever it just read,
        // so the analyser decodes the same code again every time the frame
        // gate lets a still scene through. Deliberate actions — the shutter,
        // the gallery, a pasted link — do not come through here and are
        // never suppressed.
        if (snapshot.continuousMode && !repeatGate.shouldAccept(code.text)) return
        accept(code, ScanSource.Camera)
    }

    fun onImageSelected(bitmap: Bitmap) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val code = withContext(Dispatchers.Default) { decoder.decode(bitmap) }
            if (code == null) {
                _state.update { it.copy(busy = false) }
                _events.send(ScannerEvent.Message(ScannerMessage.NoCodeInImage))
            } else {
                _state.update { it.copy(busy = false) }
                accept(code, ScanSource.Image)
            }
        }
    }

    /**
     * The shutter button: grab a frame and decode it, whatever auto-detect is
     * set to. Shares [accept] with the automatic path so both behave the same.
     */
    fun onCaptureStarted() = _state.update { it.copy(busy = true) }

    fun onCaptured(code: DecodedCode?) {
        if (code == null) {
            _state.update { it.copy(busy = false) }
            viewModelScope.launch {
                _events.send(ScannerEvent.Message(ScannerMessage.NothingInViewfinder))
            }
            return
        }
        _state.update { it.copy(busy = false) }
        accept(code, ScanSource.Camera)
    }

    fun onPasteChanged(value: String) = _state.update { it.copy(pasteText = value) }

    fun setPasteOpen(open: Boolean) =
        _state.update { it.copy(pasteOpen = open, pasteText = "") }

    fun submitPaste() {
        val text = _state.value.pasteText.trim()
        if (text.isEmpty()) {
            viewModelScope.launch {
                _events.send(ScannerEvent.Message(ScannerMessage.NothingToDecode))
            }
            return
        }
        // Anything the parser already recognises is an instruction, not an address.
// Anything the parser already recognises is an instruction, not an
        // address. Without this, pasting `geo:51.5,-0.12` becomes
        // `https://geo:51.5,-0.12`, which is a malformed URL and fails the
        // host check below — so a pasted Wi-Fi or deep link used to die as
        // "not a valid link".
        val recognised = PayloadParser.parse(text)
        if (recognised !is ScannedPayload.Text) {
            _state.update { it.copy(pasteOpen = false, pasteText = "") }
            accept(DecodedCode(text, "QR_CODE"), ScanSource.Manual)
            return
        }

        val candidate = if (text.startsWith("http", ignoreCase = true)) text else "https://$text"
        val host = runCatching { java.net.URI(candidate).host }.getOrNull()
        if (host.isNullOrBlank()) {
            viewModelScope.launch {
                _events.send(ScannerEvent.Message(ScannerMessage.NotAValidLink))
            }
            return
        }
        _state.update { it.copy(pasteOpen = false, pasteText = "") }
        accept(DecodedCode(candidate, "QR_CODE"), ScanSource.Manual)
    }

    fun onTorchChanged(on: Boolean) = _state.update { it.copy(torchOn = on) }

    fun onCameraSwitched(front: Boolean) {
        _state.update { it.copy(usingFrontCamera = front, torchOn = false) }
        viewModelScope.launch { settingsRepository.setPreferFrontCamera(front) }
    }

    private fun accept(code: DecodedCode, source: ScanSource) {
        if (_state.value.busy) return
        val payload = PayloadParser.parse(code.text, code.formatName)
        if (payload.raw.isBlank()) {
            viewModelScope.launch {
                _events.send(ScannerEvent.Message(ScannerMessage.NothingToDecode))
            }
            return
        }

        // Decided here rather than read from the snapshot after the write, so a
        // preference change mid-write cannot swap the exit out from under an
        // accepted scan.
        val continuous = _state.value.continuousMode

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val id = scanRepository.record(payload, source)
            val snapshot = _state.value
            _state.update { it.copy(busy = false) }

            _events.send(
                ScannerEvent.SignalScanFeedback(
                    vibrate = snapshot.vibrate,
                    playSound = snapshot.scanSound,
                ),
            )
            if (snapshot.copyAutomatically) {
                _events.send(ScannerEvent.CopyToClipboard(payload.raw))
            }
            if (continuous) {
                _events.send(
                    ScannerEvent.SavedInPlace(payload.type, payload.toastLabel),
                )
            } else {
                _events.send(ScannerEvent.OpenResult(id))
            }
        }
    }

    private companion object {
        const val RECENT_LIMIT = 4
    }
}