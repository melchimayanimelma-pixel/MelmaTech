package com.melmatech.app

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object SupabaseService {

private val client = OkHttpClient()

fun testerConnexion(callback: (Boolean, String) -> Unit) {
    val url = "${SupabaseConfig.url}/rest/v1/produits?select=id&limit=1"

    val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.key)
        .addHeader("Authorization", "Bearer ${SupabaseConfig.key}")
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            callback(false, "Erreur réseau : ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (it.isSuccessful) {
                    callback(true, "Serveur Supabase accessible.")
                } else {
                    callback(
                        false,
                        "Erreur Supabase : code ${it.code}"
                    )
                }
            }
        }
    })
}

}