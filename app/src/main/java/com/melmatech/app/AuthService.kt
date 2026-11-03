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
        return preferences(context).getString("user_id", null)
    }

    fun obtenirAccessToken(context: Context): String? {
        return preferences(context).getString("access_token", null)
    }

    fun deconnecter(context: Context) {
        preferences(context).edit().clear().apply()
    }

    // Récupérer le profil complet de l'utilisateur connecté.
    fun obtenirProfil(
        context: Context,
        callback: (Boolean, String, ProfilUtilisateur?) -> Unit
    ) {
        val id = obtenirUtilisateurId(context)
        val token = obtenirAccessToken(context)

        if (id.isNullOrBlank() || token.isNullOrBlank()) {
            mainHandler.post {
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
                mainHandler.post {
                    callback(false, "Erreur réseau.", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        mainHandler.post {
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
                            mainHandler.post {
                                callback(false, "Profil introuvable.", null)
                            }
                            return
                        }

                        val p = profils.getJSONObject(0)

                        if (!p.optBoolean("actif", false)) {
                            mainHandler.post {
                                callback(false, "Compte inactif.", null)
                            }
                            return
                        }

                        val profil = ProfilUtilisateur(
                            id = p.optString("id"),
                            nomComplet = p.optString("nom_complet"),
                            role = p.optString("role"),
                            entrepriseId = p.optString("entreprise_id"),
                            photoUrl = if (p.isNull("photo_url")) {
                                null
                            } else {
                                p.optString("photo_url").takeIf {
                                    it.isNotBlank()
                                }
                            }
                        )

                        mainHandler.post {
                            callback(true, "Profil récupéré.", profil)
                        }

                    } catch (_: Exception) {
                        mainHandler.post {
                            callback(false, "Impossible de lire le profil.", null)
                        }
                    }
                }
            }
        })
    }

    // Récupérer l'entreprise de l'utilisateur connecté.
    fun obtenirEntrepriseId(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { succes, message, profil ->
            callback(
                succes,
                message,
                profil?.entrepriseId
            )
        }
    }

    // Récupérer le rôle de l'utilisateur connecté.
    fun obtenirRole(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { succes, message, profil ->
            callback(
                succes,
                message,
                profil?.role
            )
        }
    }

    // Récupérer l'URL publique d'une photo.
    private fun urlPubliquePhoto(chemin: String): String {
        return "${SupabaseConfig.url}/storage/v1/object/public/" +
            "photos-profils/$chemin"
    }

    // Envoyer une photo dans Supabase Storage.
    // Le fichier est stocké dans le dossier de l'entreprise
    // puis dans celui de l'utilisateur.
    fun envoyerPhotoProfil(
        context: Context,
        utilisateurCibleId: String,
        image: ByteArray,
        typeMime: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val utilisateurConnecteId = obtenirUtilisateurId(context)
        val token = obtenirAccessToken(context)

        if (
            utilisateurConnecteId.isNullOrBlank() ||
            token.isNullOrBlank()
        ) {
            mainHandler.post {
                callback(false, "Reconnecte-toi à ton compte.", null)
            }
            return
        }

        if (image.isEmpty()) {
            mainHandler.post {
                callback(false, "Image vide.", null)
            }
            return
        }

        if (image.size > 2 * 1024 * 1024) {
            mainHandler.post {
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
            mainHandler.post {
                callback(false, "Format d'image non accepté.", null)
            }
            return
        }

        // Vérifier que l'utilisateur connecté est administrateur
        // et que la cible appartient à sa propre entreprise.
        obtenirProfil(context) { succes, message, profilAdmin ->

            if (!succes || profilAdmin == null) {
                callback(false, message, null)
                return@obtenirProfil
            }

            val role = profilAdmin.role.lowercase()

            if (
                role !in listOf(
                    "administrateur",
                    "admin",
                    "manager"
                )
            ) {
                callback(
                    false,
                    "Seul un administrateur peut modifier les photos.",
                    null
                )
                return@obtenirProfil
            }

            // Vérifier le profil de la personne dont on change la photo.
            verifierProfilCible(
                context,
                utilisateurCibleId
            ) { cibleOK, cibleMessage, entrepriseCible ->

                if (!cibleOK || entrepriseCible != profilAdmin.entrepriseId) {
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

                val nomFichier =
                    "${UUID.randomUUID()}.$extension"

                val chemin =
                    "${profilAdmin.entrepriseId}/" +
                    "$utilisateurCibleId/$nomFichier"

                val url =
                    "${SupabaseConfig.url}/storage/v1/object/" +
                    "photos-profils/$chemin"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.key)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", typeMime)
                    .addHeader("x-upsert", "false")
                    .post(image.toRequestBody(typeMime.toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        mainHandler.post {
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
                                mainHandler.post {
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
                            ) { miseAJourOK, resultat ->

                                callback(
                                    miseAJourOK,
                                    resultat,
                                    if (miseAJourOK) photoUrl else null
                                )
                            }
                        }
                    }
                })
            }
        }
    }

    // Vérifier le profil de la cible avant l'envoi.
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
                mainHandler.post {
                    callback(false, "Erreur réseau.", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    if (!it.isSuccessful) {
                        mainHandler.post {
                            callback(false, "Erreur de vérification.", null)
                        }
                        return
                    }

                    try {
                        val profils = JSONArray(body)

                        if (profils.length() == 0) {
                            mainHandler.post {
                                callback(false, "Profil cible introuvable.", null)
                            }
                            return
                        }

                        val p = profils.getJSONObject(0)

                        if (!p.optBoolean("actif", false)) {
                            mainHandler.post {
                                callback(false, "Compte cible inactif.", null)
                            }
                            return
                        }

                        mainHandler.post {
                            callback(
                                true,
                                "Profil vérifié.",
                                p.optString("entreprise_id")
                            )
                        }

                    } catch (_: Exception) {
                        mainHandler.post {
                            callback(false, "Impossible de lire le profil cible.", null)
                        }
                    }
                }
            }
        })
    }

    // Enregistrer l'adresse de la photo dans la table profils.
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

        val url =
            "${SupabaseConfig.url}/rest/v1/profils?id=eq.$cibleId"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(json.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(false, "Photo envoyée, mais profil non mis à jour.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mainHandler.post {
                        if (it.isSuccessful) {
                            callback(true, "Photo de profil enregistrée.")
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

    // Créer un vendeur et récupérer son identifiant.
    fun creerVendeurAvecId(
        context: Context,
        nomComplet: String,
        email: String,
        motDePasse: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (token.isNullOrBlank()) {
            mainHandler.post {
                callback(false, "Reconnecte-toi à ton compte.", null)
            }
            return
        }

        val donnees = JSONObject().apply {
            put("nomComplet", nomComplet.trim())
            put("email", email.trim())
            put("motDePasse", motDePasse)
        }

        val request = Request.Builder()
            .url("${SupabaseConfig.url}/functions/v1/creer-vendeur")
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(donnees.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(
                        false,
                        "Erreur réseau. Vérifie ta connexion Internet.",
                        null
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val corps = it.body?.string().orEmpty()

                    val resultat = try {
                        JSONObject(corps)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    if (
                        it.isSuccessful &&
                        resultat.optBoolean("succes")
                    ) {
                        val vendeurId =
                            resultat.optString("vendeurId")

                        mainHandler.post {
                            callback(
                                true,
                                "Compte vendeur créé avec succès.",
                                vendeurId.takeIf { id -> id.isNotBlank() }
                            )
                        }
                    } else {
                        val message = resultat.optString(
                            "erreur",
                            "Impossible de créer le compte vendeur (${it.code})."
                        )

                        mainHandler.post {
                            callback(false, message, null)
                        }
                    }
                }
            }
        })
    }

    // Garder l'ancienne signature pour ne pas casser MainActivity.
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
        ) { succes, message, _ ->
            callback(succes, message)
        }
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
            context,
            "/auth/v1/signup",
            json,
            true,
            callback
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
            context,
            "/auth/v1/token?grant_type=password",
            json,
            true,
            callback
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
            context,
            "/auth/v1/token?grant_type=refresh_token",
            json,
            true,
            callback
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
                                editeur.putString("refresh_token", refreshToken)
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
                                    "Compte créé. Vérifie ton e-mail si Supabase demande une confirmation."
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
