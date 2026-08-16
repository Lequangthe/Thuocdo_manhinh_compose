package com.quangthe.thuocdo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quangthe.thuocdo.data.RulerRepository
import com.quangthe.thuocdo.model.RulerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulerViewModel @Inject constructor(
    private val repository: RulerRepository
) : ViewModel() {

    val uiState: StateFlow<RulerState> = repository.rulerStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RulerState()
        )

    fun updateScale(scale: Float) {
        viewModelScope.launch {
            repository.updateScale(scale)
        }
    }

    fun toggleRuler() {
        viewModelScope.launch {
            repository.toggleRulerVisibility()
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            repository.saveAll(RulerState())
        }
    }
    
    fun updateUnit(unit: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            repository.saveAll(currentState.copy(unit = unit))
        }
    }
    
    fun updateNumRulers(num: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            repository.saveAll(currentState.copy(numRulers = num))
        }
    }
    
    fun toggleCoupled(coupled: Boolean) {
        viewModelScope.launch {
            val currentState = uiState.value
            repository.saveAll(currentState.copy(isCoupled = coupled))
        }
    }

    fun toggleZoom(enabled: Boolean) {
        viewModelScope.launch {
            val currentState = uiState.value
            repository.saveAll(currentState.copy(isZoomEnabled = enabled))
        }
    }
    
    fun updateFixedOrientation(orientation: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            repository.saveAll(currentState.copy(fixedOrientation = orientation))
        }
    }
}
