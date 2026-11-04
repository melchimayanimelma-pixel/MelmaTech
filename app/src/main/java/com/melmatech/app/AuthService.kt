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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class ProfilUtilisateur(
    val id: String,
    val nomComplet: String,
    val role: String,
    val entrepriseId: String,
    val photoUrl: String?
)

data class UtilisateurEntreprise(
    val id: String,
    val nomComplet: String,
    val email: String,
    val role: String,
    val entrepriseId: String,
    val photoUrl: String?,
    val actif: Boolean
)

object AuthService {

    private val client = OkHttpClient()

    private val jsonType =
        "application/json; charset=utf-8".toMediaType()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            "melmatech_session",
            Context.MODE_PRIVATE
        )

    private fun retour(callback: () -> Unit) {
        mainHandler.post(callback)
    }

    fun estConnecte(context: Context): Boolean {
        return !preferences(context)
            .getString("access_token", null)
            .isNullOrBlank()
    }

    fun obtenirUtilisateurId(context: Context): String? {
        return preferences(context).getString("user_id", null)
    }

    fun obtenirAccessToken(context: Context): String? {
        return preferences(context).getString("access_token", null)
    }

    fun deconnecter(context: Context) {
        preferences(context).edit().clear().apply()
    }

    // PROFIL DE L'UTILISATEUR CONNECTÉ

    fun obtenirProfil(
        context: Context,
        callback: (Boolean, String, ProfilUtilisateur?) -> Unit
    ) {
        val id = obtenirUtilisateurId(context)
        val token = obtenirAccessToken(context)

        if (id.isNullOrBlank() || token.isNullOrBlank()) {
            retour {
                callback(false, "Reconnecte-toi à ton compte.", null)
            }
            return
        }

        val url =
            "${SupabaseConfig.url}/rest/v1/profils" +
            "?id=eq.$id" +
            "&select=id,nom_complet,role,entreprise_id,photo_url,actif"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                retour {
                    callback(false, "Erreur réseau.", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        retour {
                            callback(
                                false,
                                "Erreur Supabase (${it.code}).",
                                null
                            )
                        }
                        return
                    }

                    try {
                        val profils = JSONArray(body)

                        if (profils.length() == 0) {
                            retour {
                                callback(false, "Profil introuvable.", null)
                            }
                            return
                        }

                        val p = profils.getJSONObject(0)

                        if (!p.optBoolean("actif", false)) {
                            retour {
                                callback(false, "Compte inactif.", null)
                            }
                            return
                        }

                        val photo = p.optString("photo_url")

                        val profil = ProfilUtilisateur(
                            id = p.optString("id"),
                            nomComplet = p.optString("nom_complet"),
                            role = p.optString("role"),
                            entrepriseId = p.optString("entreprise_id"),
                            photoUrl = photo.takeIf {
                                it.isNotBlank() && it != "null"
                            }
                        )

                        retour {
                            callback(true, "Profil récupéré.", profil)
                        }

                    } catch (_: Exception) {
                        retour {
                            callback(
                                false,
                                "Impossible de lire le profil.",
                                null
                            )
                        }
                    }
                }
            }
        })
    }

    fun obtenirEntrepriseId(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { ok, message, profil ->
            callback(ok, message, profil?.entrepriseId)
        }
    }

    fun obtenirRole(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { ok, message, profil ->
            callback(ok, message, profil?.role)
        }
    }

    // PHOTOS DE PROFIL

    private fun urlPubliquePhoto(chemin: String): String {
        return "${SupabaseConfig.url}/storage/v1/object/public/" +
            "photos-profils/$chemin"
    }

    fun envoyerPhotoProfil(
        context: Context,
        utilisateurCibleId: String,
        image: ByteArray,
        typeMime: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (
            obtenirUtilisateurId(context).isNullOrBlank() ||
            token.isNullOrBlank()
        ) {
            retour {
                callback(false, "Reconnecte-toi à ton compte.", null)
            }
            return
        }

        if (image.isEmpty()) {
            retour {
                callback(false, "Image vide.", null)
            }
            return
        }

        if (image.size > 2 * 1024 * 1024) {
            retour {
                callback(false, "La photo doit faire moins de 2 Mo.", null)
            }
            return
        }

        if (
            typeMime !in listOf(
                "image/jpeg",
                "image/png",
                "image/webp"
            )
        ) {
            retour {
                callback(false, "Format d'image non accepté.", null)
            }
            return
        }

        obtenirProfil(context) { ok, message, admin ->

            if (!ok || admin == null) {
                callback(false, message, null)
                return@obtenirProfil
            }

            if (
                admin.role.lowercase() !in
                listOf("administrateur", "admin", "manager")
            ) {
                callback(
                    false,
                    "Seul un administrateur peut modifier les photos.",
                    null
                )
                return@obtenirProfil
            }

            verifierProfilCible(context, utilisateurCibleId) {
                cibleOk, _, entrepriseCible ->

                if (!cibleOk || entrepriseCible != admin.entrepriseId) {
                    callback(
                        false,
                        "Ce compte n'appartient pas à ton entreprise.",
                        null
                    )
                    return@verifierProfilCible
                }

                val extension = when (typeMime) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }

                val chemin =
                    "${admin.entrepriseId}/$utilisateurCibleId/" +
                    "${UUID.randomUUID()}.$extension"

                val url =
                    "${SupabaseConfig.url}/storage/v1/object/" +
                    "photos-profils/$chemin"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.key)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", typeMime)
                    .addHeader("x-upsert", "false")
                    .post(
                        image.toRequestBody(typeMime.toMediaType())
                    )
                    .build()

                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        retour {
                            callback(
                                false,
                                "Erreur lors de l'envoi de la photo.",
                                null
                            )
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                        response.use {
                            if (!it.isSuccessful) {
                                retour {
                                    callback(
                                        false,
                                        "Erreur Storage (${it.code}).",
                                        null
                                    )
                                }
                                return
                            }

                            val photoUrl = urlPubliquePhoto(chemin)

                            mettreAJourPhotoProfil(
                                context,
                                utilisateurCibleId,
                                photoUrl
                            ) { updateOk, resultat ->

                                callback(
                                    updateOk,
                                    resultat,
                                    if (updateOk) photoUrl else null
                                )
                            }
                        }
                    }
                })
            }
        }
    }

    private fun verifierProfilCible(
        context: Context,
        cibleId: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (token.isNullOrBlank()) {
            callback(false, "Session expirée.", null)
            return
        }

        val url =
            "${SupabaseConfig.url}/rest/v1/profils" +
            "?id=eq.$cibleId&select=entreprise_id,actif"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                retour {
                    callback(false, "Erreur réseau.", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        retour {
                            callback(false, "Erreur de vérification.", null)
                        }
                        return
                    }

                    try {
                        val profils = JSONArray(body)

                        if (profils.length() == 0) {
                            retour {
                                callback(
                                    false,
                                    "Profil cible introuvable.",
                                    null
                                )
                            }
                            return
                        }

                        val p = profils.getJSONObject(0)

                        if (!p.optBoolean("actif", false)) {
                            retour {
                                callback(
                                    false,
                                    "Compte cible inactif.",
                                    null
                                )
                            }
                            return
                        }

                        retour {
                            callback(
                                true,
                                "Profil vérifié.",
                                p.optString("entreprise_id")
                            )
                        }

                    } catch (_: Exception) {
                        retour {
                            callback(
                                false,
                                "Impossible de lire le profil cible.",
                                null
                            )
                        }
                    }
                }
            }
        })
    }

    private fun mettreAJourPhotoProfil(
        context: Context,
        cibleId: String,
        photoUrl: String,
        callback: (Boolean, String) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (token.isNullOrBlank()) {
            callback(false, "Session expirée.")
            return
        }

        val json = JSONObject().apply {
            put("photo_url", photoUrl)
        }

        val request = Request.Builder()
            .url(
                "${SupabaseConfig.url}/rest/v1/profils?id=eq.$cibleId"
            )
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(json.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                retour {
                    callback(
                        false,
                        "Photo envoyée, mais profil non mis à jour."
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    retour {
                        if (it.isSuccessful) {
                            callback(
                                true,
                                "Photo de profil enregistrée."
                            )
                        } else {
                            callback(
                                false,
                                "Erreur lors de l'enregistrement du profil (${it.code})."
                            )
                        }
                    }
                }
            }
        })
    }

    // APPEL DE LA FONCTION EDGE SUPABASE

    private fun appelerGestionVendeurs(
        context: Context,
        donnees: JSONObject,
        callback: (Boolean, String, JSONObject?) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (token.isNullOrBlank()) {
            retour {
                callback(false, "Reconnecte-toi à ton compte.", null)
            }
            return
        }

        val request = Request.Builder()
            .url(
                "${SupabaseConfig.url}/functions/v1/creer-vendeur"
            )
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(donnees.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                retour {
                    callback(
                        false,
                        "Erreur réseau. Vérifie ta connexion Internet.",
                        null
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    val resultat = try {
                        JSONObject(body)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    val succes =
                        it.isSuccessful &&
                        resultat.optBoolean("succes", false)

                    val message = if (succes) {
                        resultat.optString(
                            "message",
                            "Opération effectuée avec succès."
                        )
                    } else {
                        resultat.optString(
                            "erreur",
                            "Erreur serveur (${it.code})."
                        )
                    }

                    retour {
                        callback(succes, message, resultat)
                    }
                }
            }
        })
    }

    // CRÉER UN VENDEUR

    fun creerVendeurAvecId(
        context: Context,
        nomComplet: String,
        email: String,
        motDePasse: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "creer")
            put("nomComplet", nomComplet.trim())
            put("email", email.trim())
            put("motDePasse", motDePasse)
        }

        appelerGestionVendeurs(context, donnees) {
            ok, message, resultat ->

            val vendeurId = resultat
                ?.optString("vendeurId")
                ?.takeIf {
                    it.isNotBlank() && it != "null"
                }

            callback(ok, message, vendeurId)
        }
    }

    // Ancienne fonction conservée pour MainActivity.

    fun creerVendeur(
        context: Context,
        nomComplet: String,
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        creerVendeurAvecId(
            context,
            nomComplet,
            email,
            motDePasse
        ) { ok, message, _ ->

            callback(ok, message)
        }
    }

    // LISTER LES VENDEURS DE L'ENTREPRISE

    fun listerVendeurs(
        context: Context,
        callback: (
            Boolean,
            String,
            List<UtilisateurEntreprise>
        ) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "lister")
        }

        appelerGestionVendeurs(context, donnees) {
            ok, message, resultat ->

            if (!ok || resultat == null) {
                callback(false, message, emptyList())
                return@appelerGestionVendeurs
            }

            try {
                val array =
                    resultat.optJSONArray("utilisateurs") ?: JSONArray()

                val liste = mutableListOf<UtilisateurEntreprise>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)

                    val photo = item.optString("photoUrl")

                    liste.add(
                        UtilisateurEntreprise(
                            id = item.optString("id"),
                            nomComplet = item.optString("nomComplet"),
                            email = item.optString("email"),
                            role = item.optString("role"),
                            entrepriseId = item.optString("entrepriseId"),
                            photoUrl = photo.takeIf {
                                it.isNotBlank() && it != "null"
                            },
                            actif = item.optBoolean("actif", false)
                        )
                    )
                }

                callback(true, message, liste)

            } catch (_: Exception) {
                callback(
                    false,
                    "Impossible de lire la liste des vendeurs.",
                    emptyList()
                )
            }
        }
    }

    // MODIFIER LE NOM ET L'EMAIL

    fun modifierVendeur(
        context: Context,
        vendeurId: String,
        nomComplet: String,
        email: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "modifier")
            put("vendeurId", vendeurId)

            if (nomComplet.isNotBlank()) {
                put("nomComplet", nomComplet.trim())
            }

            if (email.isNotBlank()) {
                put("email", email.trim())
            }
        }

        appelerGestionVendeurs(context, donnees) {
            ok, message, _ ->
            callback(ok, message)
        }
    }

    // CHANGER LE MOT DE PASSE D'UN VENDEUR

    fun changerMotDePasseVendeur(
        context: Context,
        vendeurId: String,
        nouveauMotDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "mot_de_passe")
            put("vendeurId", vendeurId)
            put("motDePasse", nouveauMotDePasse)
        }

        appelerGestionVendeurs(context, donnees) {
            ok, message, _ ->
            callback(ok, message)
        }
    }

    // DÉSACTIVER UN VENDEUR

    fun desactiverVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        actionSurVendeur(
            context,
            "desactiver",
            vendeurId,
            callback
        )
    }

    // RÉACTIVER UN VENDEUR

    fun reactiverVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        actionSurVendeur(
            context,
            "reactiver",
            vendeurId,
            callback
        )
    }

    // SUPPRESSION LOGIQUE POUR CONSERVER L'HISTORIQUE DES VENTES

    fun supprimerVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        actionSurVendeur(
            context,
            "supprimer",
            vendeurId,
            callback
        )
    }

    private fun actionSurVendeur(
        context: Context,
        action: String,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", action)
            put("vendeurId", vendeurId)
        }

        appelerGestionVendeurs(context, donnees) {
            ok, message, _ ->
            callback(ok, message)
        }
    }

    // CRÉATION D'UN COMPTE

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
            context,
            "/auth/v1/signup",
            json,
            true,
            callback
        )
    }

    // CONNEXION

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
            context,
            "/auth/v1/token?grant_type=password",
            json,
            true,
            callback
        )
    }

    // ACTUALISER LA SESSION

    fun actualiserSession(
        context: Context,
        callback: (Boolean, String) -> Unit
    ) {
        val refreshToken = preferences(context)
            .getString("refresh_token", null)

        if (refreshToken.isNullOrBlank()) {
            retour {
                callback(false, "Aucune session à actualiser.")
            }
            return
        }

        val json = JSONObject().apply {
            put("refresh_token", refreshToken)
        }

        envoyerRequete(
            context,
            "/auth/v1/token?grant_type=refresh_token",
            json,
            true,
            callback
        )
    }

    // REQUÊTES AUTH SUPABASE

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
                retour {
                    callback(
                        false,
                        "Erreur réseau : " +
                            (e.message ?: "connexion impossible")
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

                        retour {
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
                            val editeur = preferences(context)
                                .edit()
                                .putString(
                                    "access_token",
                                    accessToken
                                )

                            if (refreshToken.isNotBlank()) {
                                editeur.putString(
                                    "refresh_token",
                                    refreshToken
                                )
                            }

                            val userId =
                                utilisateur?.optString("id")

                            if (!userId.isNullOrBlank()) {
                                editeur.putString("user_id", userId)
                            }

                            editeur.apply()

                            retour {
                                callback(true, "Connexion réussie.")
                            }

                        } else {
                            retour {
                                callback(
                                    true,
                                    "Compte créé. Vérifie ton e-mail si Supabase demande une confirmation."
                                )
                            }
                        }

                    } else {
                        retour {
                            callback(true, "Opération réussie.")
                        }
                    }
                }
            }
        })
    }
}