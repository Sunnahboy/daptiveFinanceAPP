package com.finadapt.adaptivefinance.core.network
import com.finadapt.adaptivefinance.data.remote.FastApiInterface
import  com.finadapt.adaptivefinance.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.util.Log
import okhttp3.OkHttpClient


object ApiClient{
    //domain
    private const val PRIMARY_URL = "https://adaptivefinance.duckdns.org/"
    private  const val BACKUP_URL = "https://adaptive-finance-backend.onrender.com/"
    private  const val RATE_LIMITER_URL = "https://adaptive-finance-backend.onrender.com/"



    //Safely pulled from local.properties at compile time!
    const val API_TOKEN = BuildConfig.API_TOKEN
    //2 The auto failover Interceptor
    private val failoverInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null

        try{
            //step A: try the aws primary server
            response = chain.proceed(request)
        } catch ( e: IOException) {
            //catch the timeout
            exception = e

        }
        //step B: check if the aws failed
        if (response == null || !response.isSuccessful || response.code in 500..599){
            response?.close() // prevents memory leaks
            Log.w("ApiClient","AWS server unresponsive. Failing back to Render...")

            //Step C: Swap the Url to render
            val backupHttpUrl = BACKUP_URL.toHttpUrlOrNull()
            if (backupHttpUrl != null){
                val fallbackUrl = request.url.newBuilder()
                    .scheme(scheme = backupHttpUrl.scheme)
                    .host(backupHttpUrl.host)
                    .port( backupHttpUrl.port)
                    .build()
                 val fallbackRequest = request.newBuilder()
                    .url(fallbackUrl)
                    .build()

                 try{
                    //step D : Send to render instead
                    response = chain.proceed(fallbackRequest)
                 }catch (e: IOException){
                    throw exception ?: e
                 }

            }
        }
        //return a successful response or throw error if both server fails
        response?: throw exception ?: IOException("Both primary and backup servers failed")

        }


        //3 Attach the interceptor to the Okhttp
        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(failoverInterceptor)
            //60 second timeouts to account for render's cold start sleep mode
            .connectTimeout(60,TimeUnit.SECONDS)
            .readTimeout(60,TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

     //4 build retrofit using the custom OkHttpClient

    val retrofit: Retrofit by lazy{
        Retrofit.Builder()
            .baseUrl(PRIMARY_URL)//defaults to aws ec2
            .client(okHttpClient)//failover logic and timeout
            .addConverterFactory(GsonConverterFactory.create()) // coverts JSON to kotlin data class
            .build()
    }

    val fastApiService: FastApiInterface by lazy{
        retrofit.create(FastApiInterface::class.java)
    }


    //KTor Rate limiting
    private val rateLimitOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val rateLimitRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(RATE_LIMITER_URL)
            .client(rateLimitOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    //call endpoint safely
    val rateLimitApiService: FastApiInterface by lazy{
        rateLimitRetrofit.create(FastApiInterface::class.java)
    }
}