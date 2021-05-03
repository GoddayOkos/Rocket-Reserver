package com.example.rocketreserver

import android.content.Context
import android.os.Looper
import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private var instance: ApolloClient? = null


fun apolloClient(context: Context): ApolloClient {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Only the main thread can get the apolloClient instance"
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthorizationInterceptor(context))
        .build()

    if (instance != null) {
        return instance!!
    }

    instance = ApolloClient.builder()
        .serverUrl("https://apollo-fullstack-tutorial.herokuapp.com")
        .subscriptionTransportFactory(
            WebSocketSubscriptionTransport
                .Factory("wss://apollo-fullstack-tutorial.herokuapp.com/graphql", okHttpClient)
        )
        .okHttpClient(okHttpClient)
        .build()

    return instance!!
}


private class AuthorizationInterceptor(val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", User.getToken(context) ?: "")
            .build()
        return chain.proceed(request)
    }
}


fun logIt(message: String) {
    Log.d("LaunchList", message)
}