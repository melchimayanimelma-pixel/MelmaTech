package com.melmatech.app

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object AuthService {

    private val client = OkHttpClient()

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    fun creerCompte(
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val url = "${SupabaseConfig.url}/auth/v1/signup"

        val json = JSONObject().apply {
            put("email", email)
            put("password", motDePasse)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(
                    false,
                    "Erreur réseau : ${e.message ?: "connexion impossible"}"
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {

                    if (it.isSuccessful) {
                        callback(
                            true,
                            "Compte créé avec succès."
                        )
                    } else {
                        val message = try {
                            JSONObject(it.body?.string() ?: "")
                                .optString("msg")
                        } catch (e: Exception) {
                            ""
                        }

                        callback(
                            false,
                            if (message.isNotBlank()) {
                                message
                            } else {
                                "Erreur lors de la création du compte. Code ${it.code}"
                            }
                        )
                    }
                }
            }
        })
    }

    fun connexion(
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val url =
            "${SupabaseConfig.url}/auth/v1/token?grant_type=password"

        val json = JSONObject().apply {
            put("email", email)
            put("password", motDePasse)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(
                    false,
                    "Erreur réseau : ${e.message ?: "connexion impossible"}"
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {

                    if (it.isSuccessful) {
                        callback(
                            true,
                            "Connexion réussie."
                        )
                    } else {
                        val message = try {
                            JSONObject(it.body?.string() ?: "")
                                .optString("error_description")
                        } catch (e: Exception) {
                            ""
                        }

                        callback(
                            false,
                            if (message.isNotBlank()) {
                                message
                            } else {
                                "Identifiants incorrects. Code ${it.code}"
                            }
                        )
                    }
                }
            }
        })
    }
}