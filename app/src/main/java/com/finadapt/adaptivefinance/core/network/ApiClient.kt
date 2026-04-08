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


object ApiClient {
    // Domains
    private const val PRIMARY_URL = "https://adaptivefinance.duckdns.org/"
    private const val BACKUP_URL = "https://adaptive-finance-backend.onrender.com/"
    private const val RATE_LIMITER_URL = "https://finadapt-ratelimiter-service-production.up.railway.app/"

    //Safely pulled from local.properties at compile time!
    const val API_TOKEN = BuildConfig.API_TOKEN



    @Volatile//ensures thread safety , OKHttp runs on background threads
    private var circuitOpenUntil: Long = 0L
    private const val CIRCUIT_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

    //auto failover Interceptor
    private val failoverInterceptor = Interceptor { chain ->
        val request = chain.request()
        val now = System.currentTimeMillis()

        // 1 Check Circuit Breaker & Safety Headers
        val isCircuitOpen = now < circuitOpenUntil
        val avoidRetry = request.header("X-No-Retry") == "true"

        var response: Response? = null
        var exception: IOException? = null

        // 2 Try Primary Server (only if the circuit is closed)
        if (!isCircuitOpen) {
            try {
                //Give primary server a fast 10-second leash, don't block the user for a minute
                response = chain
                    .withConnectTimeout(10, TimeUnit.SECONDS)
                    .withReadTimeout(15, TimeUnit.SECONDS)
                    .proceed(request)
            } catch (e: IOException) {
                exception = e //Caught a timeout or network drop
            }
        }

        // 3. Evaluate the Result
        val isServerDown = response == null || response.code in 502..504

        // If primary succeeded, or it's a 4xx error  or  forbidden to retry -> return immediately
        if (!isServerDown || avoidRetry) {
            if (response?.isSuccessful == true) {
                circuitOpenUntil = 0L // Reset the breaker on a successful AWS ec2 hit
            }
            return@Interceptor response ?: throw exception ?: IOException("Unknown network error")
        }

        // 4. Primary Failed -> Trip the Circuit Breaker
        if (!isCircuitOpen) {
            Log.w("ApiClient", "AWS server down! Tripping circuit breaker for 5 mins. Failing over to Render...")
            circuitOpenUntil = now + CIRCUIT_COOLDOWN_MS
        } else {
            Log.w("ApiClient", "Circuit open. Routing directly to Render...")
        }

        response?.close() // Prevents memory leaks from the dead primary body

        // 5. Build the Fallback Request
        val backupHttpUrl = BACKUP_URL.toHttpUrlOrNull()
            ?: throw IOException("Invalid Backup URL configuration")

        //Safely adopt Render's scheme, host, and port natively
        val fallbackUrl = request.url.newBuilder()
            .scheme(backupHttpUrl.scheme)
            .host(backupHttpUrl.host)
            .port(backupHttpUrl.port)
            .build()

        val fallbackRequest = request.newBuilder()
            .url(fallbackUrl)
            .header("X-Is-Fallback", "true")
            .removeHeader("X-No-Retry") // Clean up the request headers
            .build()

        // 6. Send to fall 0ver server
        try {
            //Give backup  the full 60 seconds to wake up from its sleep mode
            return@Interceptor chain
                .withConnectTimeout(60, TimeUnit.SECONDS)
                .withReadTimeout(60, TimeUnit.SECONDS)
                .proceed(fallbackRequest)
        } catch (e: IOException) {
            throw exception ?: e // Throw the original exception if both fail
        }
    }

    // Attach the interceptor to OkHttp
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(failoverInterceptor)
        // Default base timeouts
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // KTor Rate limiting (Separate client without the  failover logic)
    private val rateLimitOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    //build Retrofit instances
    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Retrofit Instances
    val retrofit: Retrofit by lazy {
        buildRetrofit(PRIMARY_URL, okHttpClient)
    }

    val fastApiService: FastApiInterface by lazy {
        retrofit.create(FastApiInterface::class.java)
    }

    //Rate Limiter Retrofit Instance
    val rateLimitRetrofit: Retrofit by lazy {
        buildRetrofit(RATE_LIMITER_URL, rateLimitOkHttpClient)
    }

    // Rate Limiter Service
    val rateLimitApiService: FastApiInterface by lazy {
        rateLimitRetrofit.create(FastApiInterface::class.java)
    }
}