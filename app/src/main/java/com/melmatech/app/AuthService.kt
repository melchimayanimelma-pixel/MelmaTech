package com.melmatech.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object AuthService {

    private val client = OkHttpClient()
    private val jsonType =
        "application/json; charset=utf-8".toMediaType()

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            "melmatech_session",
            Context.MODE_PRIVATE
        )

    fun estConnecte(context: Context): Boolean {
        return preferences(context)
            .getString("access_token", null) != null
    }

    fun obtenirUtilisateurId(context: Context): String? {
        return preferences(context)
            .getString("user_id", null)
    }

    fun obtenirAccessToken(context: Context): String? {
        return preferences(context)
            .getString("access_token", null)
    }

    fun deconnecter(context: Context) {
        preferences(context).edit().clear().apply()
    }

    fun creerCompte(
        context: Context,
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("email", email.trim())
            put("password", motDePasse)
        }

        envoyerRequete(
            context = context,
            chemin = "/auth/v1/signup",
            json = json,
            sauvegarderSession = true,
            callback = callback
        )
    }

    fun connexion(
        context: Context,
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("email", email.trim())
            put("password", motDePasse)
        }

        envoyerRequete(
            context = context,
            chemin = "/auth/v1/token?grant_type=password",
            json = json,
            sauvegarderSession = true,
            callback = callback
        )
    }

    fun actualiserSession(
        context: Context,
        callback: (Boolean, String) -> Unit
    ) {
        val refreshToken = preferences(context)
            .getString("refresh_token", null)

        if (refreshToken.isNullOrBlank()) {
            mainHandler.post {
                callback(false, "Aucune session à actualiser.")
            }
            return
        }

        val json = JSONObject().apply {
            put("refresh_token", refreshToken)
        }

        envoyerRequete(
            context = context,
            chemin = "/auth/v1/token?grant_type=refresh_token",
            json = json,
            sauvegarderSession = true,
            callback = callback
        )
    }

    private fun envoyerRequete(
        context: Context,
        chemin: String,
        json: JSONObject,
        sauvegarderSession: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}$chemin")
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(
                        false,
                        "Erreur réseau : ${e.message ?: "connexion impossible"}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        val message = try {
                            val erreur = JSONObject(body)
                            erreur.optString(
                                "msg",
                                erreur.optString(
                                    "error_description",
                                    erreur.optString(
                                        "message",
                                        "Erreur de connexion."
                                    )
                                )
                            )
                        } catch (_: Exception) {
                            "Erreur du serveur (code ${it.code})."
                        }

                        mainHandler.post {
                            callback(false, message)
                        }
                        return
                    }

                    val resultat = try {
                        JSONObject(body)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    if (sauvegarderSession) {
                        val accessToken =
                            resultat.optString("access_token")
                        val refreshToken =
                            resultat.optString("refresh_token")
                        val utilisateur =
                            resultat.optJSONObject("user")

                        if (accessToken.isNotBlank()) {
                            val editeur = preferences(context).edit()
                                .putString("access_token", accessToken)

                            if (refreshToken.isNotBlank()) {
                                editeur.putString(
                                    "refresh_token",
                                    refreshToken
                                )
                            }

                            val userId = utilisateur?.optString("id")
                            if (!userId.isNullOrBlank()) {
                                editeur.putString("user_id", userId)
                            }

                            editeur.apply()

                            mainHandler.post {
                                callback(true, "Connexion réussie.")
                            }
                        } else {
                            mainHandler.post {
                                callback(
                                    true,
                                    "Compte créé. Vérifie ton e-mail si Supabase demande une confirmation avant la connexion."
                                )
                            }
                        }
                    } else {
                        mainHandler.post {
                            callback(true, "Opération réussie.")
                        }
                    }
                }
            }
        })
    }
}