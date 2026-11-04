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

    // Récupérer l'entreprise de l'utilisateur connecté.
    fun obtenirEntrepriseId(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { succes, message, profil ->
            callback(succes, message, profil?.entrepriseId)
        }
    }

    // Récupérer le rôle de l'utilisateur connecté.
    fun obtenirRole(
        context: Context,
        callback: (Boolean, String, String?) -> Unit
    ) {
        obtenirProfil(context) { succes, message, profil ->
            callback(succes, message, profil?.role)
        }
    }

    // Construire l'URL publique d'une photo.
    private fun urlPubliquePhoto(chemin: String): String {
        return "${SupabaseConfig.url}/storage/v1/object/public/" +
            "photos-profils/$chemin"
    }

    // Envoyer une photo dans Supabase Storage.
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

        if (typeMime !in listOf(
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

        obtenirProfil(context) { succes, message, profilAdmin ->

            if (!succes || profilAdmin == null) {
                callback(false, message, null)
                return@obtenirProfil
            }

            val role = profilAdmin.role.lowercase()

            if (role !in listOf("administrateur", "admin", "manager")) {
                callback(
                    false,
                    "Seul un administrateur peut modifier les photos.",
                    null
                )
                return@obtenirProfil
            }

            verifierProfilCible(context, utilisateurCibleId) {
                    cibleOK, _, entrepriseCible ->

                if (
                    !cibleOK ||
                    entrepriseCible != profilAdmin.entrepriseId
                ) {
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

                val nomFichier = "${UUID.randomUUID()}.$extension"

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

    // Vérifier que le profil cible appartient à la même entreprise.
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
                    callback(
                        false,
                        "Photo envoyée, mais profil non mis à jour."
                    )
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
    // Modèle représentant un vendeur ou un utilisateur de l'entreprise.
    data class UtilisateurEntreprise(
        val id: String,
        val nomComplet: String,
        val email: String,
        val role: String,
        val entrepriseId: String,
        val photoUrl: String?,
        val actif: Boolean
    )

    // Créer un vendeur et récupérer son identifiant.
    fun creerVendeurAvecId(
        context: Context,
        nomComplet: String,
        email: String,
        motDePasse: String,
        callback: (Boolean, String, String?) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("nomComplet", nomComplet)
            put("email", email)
            put("motDePasse", motDePasse)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, resultat ->
            val vendeurId = resultat?.optString("vendeurId")
                ?.takeIf { it.isNotBlank() && it != "null" }

            callback(succes, message, vendeurId)
        }
    }

    // Version simplifiée de la création d'un vendeur.
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

    // Lister les vendeurs de l'entreprise.
    fun listerVendeurs(
        context: Context,
        callback: (Boolean, String, List<UtilisateurEntreprise>) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "lister")
        }

        appelerGestionVendeurs(context, donnees) { succes, message, resultat ->

            if (!succes || resultat == null) {
                callback(succes, message, emptyList())
                return@appelerGestionVendeurs
            }

            try {
                val tableau = resultat.optJSONArray("utilisateurs")
                    ?: JSONArray()

                val utilisateurs = mutableListOf<UtilisateurEntreprise>()

                for (i in 0 until tableau.length()) {
                    val item = tableau.getJSONObject(i)

                    utilisateurs.add(
                        UtilisateurEntreprise(
                            id = item.optString("id"),
                            nomComplet = item.optString("nomComplet"),
                            email = item.optString("email"),
                            role = item.optString("role"),
                            entrepriseId = item.optString("entrepriseId"),
                            photoUrl = if (item.isNull("photoUrl")) {
                                null
                            } else {
                                item.optString("photoUrl").takeIf {
                                    it.isNotBlank()
                                }
                            },
                            actif = item.optBoolean("actif", false)
                        )
                    )
                }

                callback(true, message, utilisateurs)

            } catch (_: Exception) {
                callback(
                    false,
                    "Impossible de lire la liste des vendeurs.",
                    emptyList()
                )
            }
        }
    }

    // Modifier le nom et l'adresse e-mail d'un vendeur.
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
            put("nomComplet", nomComplet)
            put("email", email)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, _ ->
            callback(succes, message)
        }
    }

    // Modifier le mot de passe d'un vendeur.
    fun changerMotDePasseVendeur(
        context: Context,
        vendeurId: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "mot_de_passe")
            put("vendeurId", vendeurId)
            put("motDePasse", motDePasse)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, _ ->
            callback(succes, message)
        }
    }

    // Désactiver un vendeur.
    fun desactiverVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "desactiver")
            put("vendeurId", vendeurId)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, _ ->
            callback(succes, message)
        }
    }

    // Réactiver un vendeur.
    fun reactiverVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "reactiver")
            put("vendeurId", vendeurId)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, _ ->
            callback(succes, message)
        }
    }

    // Supprimer logiquement un vendeur.
    // La fonction serveur doit conserver son historique de ventes.
    fun supprimerVendeur(
        context: Context,
        vendeurId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("action", "supprimer")
            put("vendeurId", vendeurId)
        }

        appelerGestionVendeurs(context, donnees) { succes, message, _ ->
            callback(succes, message)
        }
    }

    // Fonction centrale utilisée par toutes les opérations vendeurs.
    // ATTENTION : cette fonction ne doit exister qu'une seule fois.
    private fun appelerGestionVendeurs(
        context: Context,
        donnees: JSONObject,
        callback: (Boolean, String, JSONObject?) -> Unit
    ) {
        val token = obtenirAccessToken(context)

        if (token.isNullOrBlank()) {
            mainHandler.post {
                callback(false, "Session expirée. Reconnecte-toi.", null)
            }
            return
        }

        val url =
            "${SupabaseConfig.url}/functions/v1/creer-vendeur"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(donnees.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(false, "Erreur réseau : ${e.message}", null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    try {
                        val resultat = JSONObject(body)
                        val succes = it.isSuccessful &&
                            resultat.optBoolean("succes", false)

                        val message = if (succes) {
                            "Opération effectuée avec succès."
                        } else {
                            resultat.optString(
                                "erreur",
                                "L'opération a échoué."
                            )
                        }

                        mainHandler.post {
                            callback(succes, message, resultat)
                        }

                    } catch (_: Exception) {
                        mainHandler.post {
                            callback(
                                false,
                                "Réponse du serveur illisible.",
                                null
                            )
                        }
                    }
                }
            }
        })
    }

    // Créer un compte utilisateur directement avec Supabase Auth.
    fun creerCompte(
        context: Context,
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("email", email)
            put("password", motDePasse)
        }

        envoyerRequete(
            context,
            "/auth/v1/signup",
            donnees,
            false,
            callback
        )
    }

    // Connecter un utilisateur.
    fun connexion(
        context: Context,
        email: String,
        motDePasse: String,
        callback: (Boolean, String) -> Unit
    ) {
        val donnees = JSONObject().apply {
            put("email", email)
            put("password", motDePasse)
        }

        envoyerRequete(
            context,
            "/auth/v1/token?grant_type=password",
            donnees,
            true,
            callback
        )
    }

    // Renouveler la session.
    fun actualiserSession(
        context: Context,
        callback: (Boolean, String) -> Unit
    ) {
        val refreshToken = preferences(context)
            .getString("refresh_token", null)

        if (refreshToken.isNullOrBlank()) {
            mainHandler.post {
                callback(false, "Aucune session à renouveler.")
            }
            return
        }

        val donnees = JSONObject().apply {
            put("refresh_token", refreshToken)
        }

        envoyerRequete(
            context,
            "/auth/v1/token?grant_type=refresh_token",
            donnees,
            true,
            callback
        )
    }

    // Envoyer une requête d'authentification à Supabase.
    private fun envoyerRequete(
        context: Context,
        chemin: String,
        donnees: JSONObject,
        sauvegarderSession: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val url = "${SupabaseConfig.url}$chemin"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Content-Type", "application/json")
            .post(donnees.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    callback(false, "Erreur réseau : ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()

                    try {
                        val resultat = JSONObject(body)

                        if (!it.isSuccessful) {
                            val erreur = resultat.optString(
                                "msg",
                                resultat.optString(
                                    "message",
                                    resultat.optString(
                                        "error_description",
                                        "Connexion impossible."
                                    )
                                )
                            )

                            mainHandler.post {
                                callback(false, erreur)
                            }
                            return
                        }

                        if (sauvegarderSession) {
                            val accessToken =
                                resultat.optString("access_token")

                            val refreshToken =
                                resultat.optString("refresh_token")

                            val utilisateurId =
                                resultat.optJSONObject("user")
                                    ?.optString("id")

                            if (
                                accessToken.isBlank() ||
                                utilisateurId.isNullOrBlank()
                            ) {
                                mainHandler.post {
                                    callback(
                                        false,
                                        "La réponse de connexion est incomplète."
                                    )
                                }
                                return
                            }

                            preferences(context).edit()
                                .putString("access_token", accessToken)
                                .putString("refresh_token", refreshToken)
                                .putString("user_id", utilisateurId)
                                .apply()
                        }

                        mainHandler.post {
                            callback(true, "Opération réussie.")
                        }

                    } catch (_: Exception) {
                        mainHandler.post {
                            callback(
                                false,
                                "Impossible de lire la réponse du serveur."
                            )
                        }
                    }
                }
            }
        })
    }
}