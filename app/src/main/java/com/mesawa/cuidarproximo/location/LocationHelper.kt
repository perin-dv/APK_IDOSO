package com.mesawa.cuidarproximo.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices

class LocationHelper(
    private val context: Context
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun obterLocalizacao(
        callback: (Double, Double) -> Unit
    ) {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    callback(
                        location.latitude,
                        location.longitude
                    )
                }
            }
    }
}