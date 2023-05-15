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
    val errorFlow = _errorLiveEvent.asFlow()

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
                _errorLiveEvent.postValue(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
