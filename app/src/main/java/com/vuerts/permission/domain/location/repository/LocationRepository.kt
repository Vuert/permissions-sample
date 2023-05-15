package com.vuerts.permission.domain.location.repository

import com.vuerts.permission.domain.location.model.Location

interface LocationRepository {

    suspend fun getLocation(): Location
}
