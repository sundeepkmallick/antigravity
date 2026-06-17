package com.example.resumematcher.ui.main

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
    
    @Test
    fun uiState_initiallyEmpty() = runTest {
        val viewModel = MainScreenViewModel()
        val state = viewModel.uiState.value
        assertNull(state.resumeUri)
        assertNull(state.resumeName)
        assertNull(state.resumeSize)
        assertNull(state.resumeText)
        assertNull(state.jdUri)
        assertNull(state.jdName)
        assertNull(state.jdSize)
        assertNull(state.jdText)
        assertNull(state.matchResult)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun uiState_reset_clearsState() = runTest {
        val viewModel = MainScreenViewModel()
        viewModel.reset()
        val state = viewModel.uiState.value
        assertNull(state.resumeUri)
        assertNull(state.resumeName)
        assertNull(state.resumeSize)
        assertNull(state.resumeText)
        assertNull(state.jdUri)
        assertNull(state.jdName)
        assertNull(state.jdSize)
        assertNull(state.jdText)
        assertNull(state.matchResult)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }
}
