package com.vuerts.permission.presentation.location.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.hadilq.liveevent.LiveEvent
import com.vuerts.permission.domain.location.repository.LocationRepository
import com.vuerts.permission.presentation.location.model.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _errorLiveEvent = LiveEvent<Throwable>()
    val error = _errorLiveEvent.asFlow()

    private val _isLoadingStateFlow = MutableStateFlow(false)
    val isLoading = _isLoadingStateFlow.asStateFlow()

    private val _locationStateFlow = MutableStateFlow<LocationState>(LocationState.Empty)
    val location = _locationStateFlow.asStateFlow()

    fun onGetLocationButtonClicked() {
        viewModelScope.launch {
            _isLoadingStateFlow.value = true
            try {
                _locationStateFlow.value = LocationState.Loaded(
                    location = locationRepository.getLocation(),
                )
            } catch (e: Throwable) {
                _errorLiveEvent.value = e
            } finally {
                _isLoadingStateFlow.value = false
            }
        }
    }
}
