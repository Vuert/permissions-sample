package com.vuerts.permission.presentation.location.model

import com.vuerts.permission.domain.location.model.Location

sealed class LocationState {

    object Empty : LocationState()
    data class Loaded(val location: Location) : LocationState()
}
