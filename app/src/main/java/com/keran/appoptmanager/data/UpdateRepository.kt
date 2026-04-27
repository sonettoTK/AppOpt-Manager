package com.keran.appoptmanager.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.keran.appoptmanager.model.UpdateInfo
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface UpdateApiService {
    @GET("nakasakisoyo/AppOpt-manager/raw/master/latest.json")
    suspend fun getUpdateInfo(): UpdateInfo

    @Streaming
    @GET
    suspend fun downloadApk(@Url url: String): ResponseBody
}

class UpdateRepository {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    private val contentType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AppOptManager/1.0")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gitee.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    private val service = retrofit.create(UpdateApiService::class.java)

    suspend fun checkUpdate(): Result<UpdateInfo> {
        return try {
            val updateInfo = service.getUpdateInfo()
            Result.success(updateInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadApk(url: String, outputFile: File, onProgress: (Float) -> Unit): Result<File> {
        return try {
            val responseBody = service.downloadApk(url)
            val inputStream: InputStream = responseBody.byteStream()
            val totalBytes = responseBody.contentLength()
            
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var bytesCopied: Long = 0
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    if (totalBytes > 0) {
                        onProgress(bytesCopied.toFloat() / totalBytes.toFloat())
                    }
                }
                outputStream.flush()
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
