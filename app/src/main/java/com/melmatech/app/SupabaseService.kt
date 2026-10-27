package com.melmatech.app

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
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
}