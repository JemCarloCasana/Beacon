package com.example.beacon.api

import com.example.beacon.api.models.BootstrapRequest
import com.example.beacon.api.models.DeviceDto
import com.example.beacon.api.models.DeviceRegisterRequest
import com.example.beacon.api.models.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {
    @POST("/me/bootstrap")
    suspend fun bootstrap(
        @Header("Authorization") authHeader: String,
        @Body body: BootstrapRequest
    ): UserDto

    @GET("/me")
    suspend fun me(
        @Header("Authorization") authHeader: String
    ): UserDto

    @POST("/devices/register")
    suspend fun registerDevice(
        @Header("Authorization") authHeader: String,
        @Body body: DeviceRegisterRequest
    ): DeviceDto

}
