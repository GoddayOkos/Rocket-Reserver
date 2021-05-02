package com.example.rocketreserver

import android.util.Log
import com.apollographql.apollo.ApolloClient


val apolloClient: ApolloClient = ApolloClient.builder()
    .serverUrl("https://apollo-fullstack-tutorial.herokuapp.com")
    .build()


fun logIt(message: String) {
    Log.d("LaunchList", message)
}