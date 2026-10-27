package com.melmatech.app

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONArray
import android.os.Handler
import android.os.Looper
import java.io.IOException

object SupabaseService {

    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun testerConnexion(callback: (Boolean, String) -> Unit) {
        val url = "${SupabaseConfig.url}/auth/v1/health"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .get()
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
                        callback(true, "Serveur Supabase accessible.")
                    } else {
                        callback(false, "Réponse Supabase : code ${it.code}")
                    }
                }
            }
        })
    }

    fun ajouterProduit(
        accessToken: String,
        entrepriseId: String,
        nom: String,
        prixAchat: Double,
        prixVente: Double,
        quantite: Int,
        callback: (Boolean, String) -> Unit
    ) {
        val url = "${SupabaseConfig.url}/rest/v1/produits"

        val donnees = JSONObject().apply {
            put("entreprise_id", entrepriseId)
            put("nom", nom)
            put("prix_achat", prixAchat)
            put("prix_vente", prixVente)
            put("quantite_stock", quantite)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(donnees.toString().toRequestBody(jsonType))
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
                        callback(true, "Produit synchronisé avec Supabase.")
                    } else {
                        callback(
                            false,
                            "Erreur Supabase (${it.code}) : ${
                                it.body?.string() ?: "enregistrement refusé"
                            }"
                        )
                    }
                }
            }
        })
    }
fun chargerProduits(
        accessToken: String,
        entrepriseId: String,
        callback: (Boolean, String, JSONArray?) -> Unit
    ) {
        val url =
            "${SupabaseConfig.url}/rest/v1/produits" +
            "?entreprise_id=eq.$entrepriseId" +
            "&select=id,entreprise_id,nom,prix_achat,prix_vente,quantite_stock"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback(
                        false,
                        "Erreur réseau : ${e.message ?: "connexion impossible"}",
                        null
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val corps = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        Handler(Looper.getMainLooper()).post {
                            callback(
                                false,
                                "Erreur Supabase (${it.code}).",
                                null
                            )
                        }
                        return
                    }

                    try {
                        val produits = JSONArray(corps)

                        Handler(Looper.getMainLooper()).post {
                            callback(
                                true,
                                "Produits chargés depuis Supabase.",
                                produits
                            )
                        }
                    } catch (_: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            callback(
                                false,
                                "Impossible de lire les produits.",
                                null
                            )
                        }
                    }
                }
            }
        })
    }}