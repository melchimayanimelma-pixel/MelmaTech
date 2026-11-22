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
    codeBarres: String,
    callback: (Boolean, String, String?) -> Unit
) {
    val url = "${SupabaseConfig.url}/rest/v1/produits"

    val donnees = JSONObject().apply {
        put("entreprise_id", entrepriseId)
        put("nom", nom)
        put("prix_achat", prixAchat)
        put("prix_vente", prixVente)
        put("quantite_stock", quantite)
        put("code_barres", codeBarres)
    }

    val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.key)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=representation")
        .post(donnees.toString().toRequestBody(jsonType))
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
                            "Erreur Supabase (${it.code}) : $corps",
                            null
                        )
                    }
                    return
                }

                try {
                    val resultat = JSONArray(corps)
                    val uuid = resultat.getJSONObject(0).getString("id")

                    Handler(Looper.getMainLooper()).post {
                        callback(
                            true,
                            "Produit synchronisé avec Supabase.",
                            uuid
                        )
                    }
                } catch (_: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        callback(
                            false,
                            "Produit créé, mais son identifiant n'a pas pu être récupéré.",
                            null
                        )
                    }
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
            "&select=id,entreprise_id,nom,prix_achat,prix_vente,quantite_stock,code_barres"

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
    }

    private fun envoyerInsertion(
        table: String,
        accessToken: String,
        json: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/$table")
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(json.toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback(false, "Erreur réseau : ${e.message ?: "connexion impossible"}", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    Handler(Looper.getMainLooper()).post {
                        if (!it.isSuccessful) {
                            callback(false, "Erreur Supabase (${it.code}) : $body", null)
                        } else {
                            val id = try { JSONArray(body).optJSONObject(0)?.optString("id") } catch (_: Exception) { null }
                            callback(true, "Données synchronisées avec Supabase.", id)
                        }
                    }
                }
            }
        })
    }

    fun ajouterClient(
        accessToken: String,
        entrepriseId: String,
        nom: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val data = JSONObject()
            .put("id", java.util.UUID.randomUUID().toString())
            .put("entreprise_id", entrepriseId)
            .put("nom", nom)
        envoyerInsertion("clients", accessToken, data.toString(), callback)
    }

    fun ajouterVente(
        accessToken: String,
        entrepriseId: String,
        clientId: String,
        numeroFacture: String,
        utilisateurId: String,
        total: Double,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val data = JSONObject()
            .put("entreprise_id", entrepriseId)
            .put("client_id", clientId)
            .put("numero_facture", numeroFacture)
            .put("utilisateur_id", utilisateurId)
            .put("total", total)
            .put("statut", "terminee")
        envoyerInsertion("ventes", accessToken, data.toString(), callback)
    }

    fun ajouterDetailsVente(
        accessToken: String,
        details: JSONArray,
        callback: (Boolean, String) -> Unit
    ) {
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/details_vente")
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(details.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback(false, "Erreur réseau : ${e.message ?: "connexion impossible"}")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    Handler(Looper.getMainLooper()).post {
                        if (it.isSuccessful) callback(true, "Articles synchronisés.")
                        else callback(false, "Erreur Supabase (${it.code}) : $body")
                    }
                }
            }
        })
    }

    fun ajouterPaiement(
        accessToken: String,
        entrepriseId: String,
        venteId: String,
        utilisateurId: String,
        montant: Double,
        modePaiement: String,
        callback: (Boolean, String) -> Unit
    ) {
        val data = JSONObject()
            .put("entreprise_id", entrepriseId)
            .put("vente_id", venteId)
            .put("utilisateur_id", utilisateurId)
            .put("montant", montant)
            .put("mode_paiement", modePaiement)
        val request = Request.Builder()
            .url("${SupabaseConfig.url}/rest/v1/paiements")
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(data.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    callback(false, "Erreur réseau : ${e.message ?: "connexion impossible"}")
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    Handler(Looper.getMainLooper()).post {
                        if (it.isSuccessful) callback(true, "Paiement synchronisé.")
                        else callback(false, "Erreur Supabase (${it.code}) : $body")
                    }
                }
            }
        })
    }

}
