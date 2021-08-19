package com.example.fetchapi

import com.example.fetchapi.API.ApiObjectJson
import retrofit2.http.GET

interface ApiRequest
{
    @GET("/hiring.json")
    suspend fun getObjects() : ApiObjectJson
}