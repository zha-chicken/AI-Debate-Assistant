package com.aidebate.di

import com.aidebate.data.remote.service.AnthropicApiService
import com.aidebate.data.remote.service.GeminiApiService
import com.aidebate.data.remote.service.OpenAiApiService
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshiConverterFactory(moshi: Moshi): MoshiConverterFactory =
        MoshiConverterFactory.create(moshi)

    @Provides
    @Singleton
    fun provideOpenAiApiService(
        okHttpClient: OkHttpClient,
        converterFactory: MoshiConverterFactory
    ): OpenAiApiService {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val originalAuth = chain.request().header("Authorization")
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", originalAuth ?: "Bearer placeholder")
                    .build()
                chain.proceed(newRequest)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
            .create(OpenAiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnthropicApiService(
        okHttpClient: OkHttpClient,
        converterFactory: MoshiConverterFactory
    ): AnthropicApiService =
        Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/")
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()
            .create(AnthropicApiService::class.java)

    @Provides
    @Singleton
    fun provideGeminiApiService(
        okHttpClient: OkHttpClient,
        converterFactory: MoshiConverterFactory
    ): GeminiApiService =
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()
            .create(GeminiApiService::class.java)
}
