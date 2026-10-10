package com.quickscan.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickscan.R
import com.quickscan.core.qr.QrLogo
import com.quickscan.core.qr.QrStyle
import com.quickscan.data.barcode.PayloadParser
import com.quickscan.data.repository.ScanRepository
import com.quickscan.data.repository.ScanSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CreateType(val labelRes: Int) {
    Link(R.string.create_type_link),
    Text(R.string.create_type_text),
    Wifi(R.string.create_type_wifi),
    Contact(R.string.create_type_contact),
}

data class CreateUiState(
    val type: CreateType = CreateType.Link,
    val label: String = "",
    val url: String = "",
    val note: String = "",
    val text: String = "",
    val ssid: String = "",
    val password: String = "",
    val security: String = "WPA",
    val hidden: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val style: QrStyle = QrStyle.DEFAULT,
    val styleOpen: Boolean = false,
    val saving: Boolean = false,
) {
    /** What the preview shows; blank until the required fields are filled. */
    val payload: String
        get() = when (type) {
            CreateType.Link ->
                if (url.isBlank()) "" else url.trim().let {
                    if (it.startsWith("http", ignoreCase = true)) it else "https://$it"
                }

            CreateType.Text -> text.trim()

            CreateType.Wifi -> if (ssid.isBlank()) {
                ""
            } else {
                buildString {
                    append("WIFI:T:${security.trim()};S:$ssid;")
                    if (password.isNotEmpty()) append("P:$password;")
                    if (hidden) append("H:true;")
                    append(";;")
                }
            }

            CreateType.Contact -> if (contactName.isBlank() && contactEmail.isBlank()) {
                ""
            } else {
                buildString {
                    append("BEGIN:VCARD\nVERSION:3.0\n")
                    if (contactName.isNotBlank()) append("FN:$contactName\n")
                    if (contactPhone.isNotBlank()) append("TEL:$contactPhone\n")
                    if (contactEmail.isNotBlank()) append("EMAIL:$contactEmail\n")
                    append("END:VCARD")
                }
            }
        }

    val caption: String
        get() = when {
            label.isNotBlank() -> label
            payload.isNotBlank() -> payload.take(64)
            else -> ""
        }

    val canSave: Boolean get() = payload.isNotBlank()
}

sealed interface CreateEvent {
    data class Saved(val payload: String) : CreateEvent
    data object Incomplete : CreateEvent
}

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    private val _events = Channel<CreateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setType(type: CreateType) = _state.update { it.copy(type = type) }

    fun setLabel(value: String) = _state.update { it.copy(label = value) }

    fun setUrl(value: String) = _state.update { it.copy(url = value) }

    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun setText(value: String) = _state.update { it.copy(text = value) }

    fun setSsid(value: String) = _state.update { it.copy(ssid = value) }

    fun setPassword(value: String) = _state.update { it.copy(password = value) }

    fun setSecurity(value: String) = _state.update { it.copy(security = value) }

    fun setHidden(value: Boolean) = _state.update { it.copy(hidden = value) }

    fun setContactName(value: String) = _state.update { it.copy(contactName = value) }

    fun setContactPhone(value: String) = _state.update { it.copy(contactPhone = value) }

    fun setContactEmail(value: String) = _state.update { it.copy(contactEmail = value) }

    fun setStyleOpen(open: Boolean) = _state.update { it.copy(styleOpen = open) }

    fun setForeground(colour: Int) =
        _state.update { it.copy(style = it.style.copy(foreground = colour)) }

    fun setCornerRadius(radius: Float) =
        _state.update { it.copy(style = it.style.copy(cornerRadius = radius)) }

    fun setLogo(logo: QrLogo) =
        _state.update { it.copy(style = it.style.copy(logo = logo)) }

    fun resetStyle() = _state.update { it.copy(style = QrStyle.DEFAULT) }

    /** Saves the generated code to history so it can be re-opened later. */
    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave || snapshot.saving) {
            viewModelScope.launch { _events.send(CreateEvent.Incomplete) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val parsed = PayloadParser.parse(snapshot.payload)
            // The style is stored with the row so the reopened code looks
            // like this preview rather than like a plain one.
            scanRepository.record(
                payload = parsed,
                source = ScanSource.Manual,
                style = snapshot.style,
            )
            _state.update { it.copy(saving = false) }
            _events.send(CreateEvent.Saved(snapshot.payload))
        }
    }
}