package com.example.resumematcher.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resumematcher.DocumentParser
import com.example.resumematcher.MatchResult
import com.example.resumematcher.MatcherEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MatcherUiState(
    val resumeUri: Uri? = null,
    val resumeName: String? = null,
    val resumeSize: String? = null,
    val resumeText: String? = null,
    val jdUri: Uri? = null,
    val jdName: String? = null,
    val jdSize: String? = null,
    val jdText: String? = null,
    val matchResult: MatchResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainScreenViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MatcherUiState())
    val uiState: StateFlow<MatcherUiState> = _uiState.asStateFlow()

    fun setResumeUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val name = DocumentParser.getFileName(context, uri)
                val sizeStr = DocumentParser.getFileSizeString(context, uri)
                val text = withContext(Dispatchers.IO) {
                    DocumentParser.extractText(context, uri)
                }
                _uiState.update {
                    it.copy(
                        resumeUri = uri,
                        resumeName = name,
                        resumeSize = sizeStr,
                        resumeText = text,
                        isLoading = false,
                        matchResult = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load resume: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun setJdUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val name = DocumentParser.getFileName(context, uri)
                val sizeStr = DocumentParser.getFileSizeString(context, uri)
                val text = withContext(Dispatchers.IO) {
                    DocumentParser.extractText(context, uri)
                }
                _uiState.update {
                    it.copy(
                        jdUri = uri,
                        jdName = name,
                        jdSize = sizeStr,
                        jdText = text,
                        isLoading = false,
                        matchResult = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load Job Description: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun runMatch() {
        val state = _uiState.value
        val resumeText = state.resumeText
        val jdText = state.jdText

        if (resumeText.isNullOrBlank() || jdText.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Please upload both documents first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = withContext(Dispatchers.Default) {
                    MatcherEngine.calculateMatch(resumeText, jdText)
                }
                _uiState.update {
                    it.copy(
                        matchResult = result,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to calculate match: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = MatcherUiState()
    }
}
