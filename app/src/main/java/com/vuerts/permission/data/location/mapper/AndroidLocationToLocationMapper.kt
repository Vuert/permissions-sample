package com.vuerts.permission.data.location.mapper

import com.vuerts.permission.domain.location.model.Location
import android.location.Location as AndroidLocation

class AndroidLocationToLocationMapper {

    fun map(location: AndroidLocation): Location = location.run {
        Location(
            latitude = latitude,
            longitude = longitude,
        )
    }
}
