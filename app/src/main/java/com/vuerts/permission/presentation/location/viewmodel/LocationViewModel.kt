package com.vuerts.permission.presentation.location.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vuerts.permission.domain.location.repository.LocationRepository
import com.vuerts.permission.presentation.location.model.LocationState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _errorSharedFlow = MutableSharedFlow<Throwable>()
    val errorFlow = _errorSharedFlow.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _locationStateFlow = MutableStateFlow<LocationState>(LocationState.Empty)
    val locationStateFlow = _locationStateFlow.asStateFlow()

    fun onGetLocationButtonClicked() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _locationStateFlow.value = LocationState.Loaded(
                    location = locationRepository.getLocation(),
                )
            } catch (e: Throwable) {
                _errorSharedFlow.emit(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
