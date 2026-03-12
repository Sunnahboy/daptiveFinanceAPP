package com.finadapt.adaptivefinance.core.network
import com.finadapt.adaptivefinance.data.remote.FastApiInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import  com.finadapt.adaptivefinance.BuildConfig


object ApiClient{
    //actual aws Nginx domain
    private const val BASE_URL = "https://adaptivefinance.duckdns.org/"

    // Safely pulled from local.properties at compile time!
    const val API_TOKEN = BuildConfig.API_TOKEN
    val retrofit: Retrofit by lazy{
        Retrofit.Builder()
            .baseUrl((BASE_URL))
            .addConverterFactory(GsonConverterFactory.create()) // coverts JSON to kotlin data class
            .build()
    }

    val fastApiService: FastApiInterface by lazy{
        retrofit.create(FastApiInterface::class.java)
    }
}