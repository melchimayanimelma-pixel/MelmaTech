
package com.melmatech.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PermissionMelma(
    val id: String,
    val module: String,
    val action: String,
    val description: String
)

data class RolePersonnalise(
    val id: String,
    val nom: String,
    val description: String
)

data class UtilisateurAcces(
    val id: String,
    val nomComplet: String,
    val roleBase: String,
    val actif: Boolean
)

data class PermissionIndividuelle(
    val utilisateurId: String,
    val permissionId: String,
    val effet: String
)

data class PermissionRole(
    val roleId: String,
    val permissionId: String
)

data class RoleUtilisateur(
    val utilisateurId: String,
    val roleId: String
)

data class DonneesGestionAcces(
    val utilisateurs: List<UtilisateurAcces>,
    val permissions: List<PermissionMelma>,
    val roles: List<RolePersonnalise>,
    val permissionsIndividuelles: List<PermissionIndividuelle>,
    val permissionsRoles: List<PermissionRole>,
    val rolesUtilisateurs: List<RoleUtilisateur>
)

object GestionAccesService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun chargerDonnees(
        context: Context,
        callback: (Boolean, String, DonneesGestionAcces?) -> Unit
    ) {
        AuthService.obtenirProfil(context) { ok, message, profil ->

            if (!ok || profil == null) {
                callback(false, message, null)
                return@obtenirProfil
            }

            Thread {
                try {
                    val entrepriseId = profil.entrepriseId

                    val utilisateursJson = get(
                        context,
                        "profils?entreprise_id=eq.$entrepriseId" +
                            "&select=id,nom_complet,role,actif" +
                            "&order=nom_complet.asc"
                    )

                    val permissionsJson = get(
                        context,
                        "permissions?select=id,module,action,description" +
                            "&order=module.asc,action.asc"
                    )

                    val rolesJson = get(
                        context,
                        "roles_personnalises?entreprise_id=eq.$entrepriseId" +
                            "&select=id,nom,description" +
                            "&order=nom.asc"
                    )

                    val permissionsIndividuellesJson = get(
                        context,
                        "utilisateur_permissions?entreprise_id=eq.$entrepriseId" +
                            "&select=utilisateur_id,permission_id,effet"
                    )

                    val permissionsRolesJson = get(
                        context,
                        "role_permissions?entreprise_id=eq.$entrepriseId" +
                            "&select=role_id,permission_id"
                    )

                    val rolesUtilisateursJson = get(
                        context,
                        "utilisateur_roles?entreprise_id=eq.$entrepriseId" +
                            "&select=utilisateur_id,role_id"
                    )

                    val donnees = DonneesGestionAcces(
                        utilisateurs = lireUtilisateurs(utilisateursJson),
                        permissions = lirePermissions(permissionsJson),
                        roles = lireRoles(rolesJson),
                        permissionsIndividuelles =
                            lirePermissionsIndividuelles(
                                permissionsIndividuellesJson
                            ),
                        permissionsRoles =
                            lirePermissionsRoles(permissionsRolesJson),
                        rolesUtilisateurs =
                            lireRolesUtilisateurs(rolesUtilisateursJson)
                    )

                    mainHandler.post {
                        callback(true, "Données chargées.", donnees)
                    }

                } catch (e: Exception) {
                    mainHandler.post {
                        callback(
                            false,
                            e.message ?: "Impossible de charger les accès.",
                            null
                        )
                    }
                }
            }.start()
        }
    }

    private fun get(
        context: Context,
        chemin: String
    ): JSONArray {
        val token = AuthService.obtenirAccessToken(context)
            ?: throw IOException("Session expirée. Reconnecte-toi.")

        val url = "${SupabaseConfig.url}/rest/v1/$chemin"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(
                    "Erreur Supabase ${response.code} : $body"
                )
            }

            return JSONArray(body)
        }
    }

    private fun lireUtilisateurs(json: JSONArray): List<UtilisateurAcces> {
        val resultat = mutableListOf<UtilisateurAcces>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                UtilisateurAcces(
                    id = item.optString("id"),
                    nomComplet = item.optString("nom_complet"),
                    roleBase = item.optString("role"),
                    actif = item.optBoolean("actif", false)
                )
            )
        }

        return resultat
    }

    private fun lirePermissions(json: JSONArray): List<PermissionMelma> {
        val resultat = mutableListOf<PermissionMelma>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                PermissionMelma(
                    id = item.optString("id"),
                    module = item.optString("module"),
                    action = item.optString("action"),
                    description = item.optString("description")
                )
            )
        }

        return resultat
    }

    private fun lireRoles(json: JSONArray): List<RolePersonnalise> {
        val resultat = mutableListOf<RolePersonnalise>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                RolePersonnalise(
                    id = item.optString("id"),
                    nom = item.optString("nom"),
                    description = item.optString("description")
                )
            )
        }

        return resultat
    }

    private fun lirePermissionsIndividuelles(
        json: JSONArray
    ): List<PermissionIndividuelle> {
        val resultat = mutableListOf<PermissionIndividuelle>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                PermissionIndividuelle(
                    utilisateurId = item.optString("utilisateur_id"),
                    permissionId = item.optString("permission_id"),
                    effet = item.optString("effet", "autoriser")
                )
            )
        }

        return resultat
    }

    private fun lirePermissionsRoles(
        json: JSONArray
    ): List<PermissionRole> {
        val resultat = mutableListOf<PermissionRole>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                PermissionRole(
                    roleId = item.optString("role_id"),
                    permissionId = item.optString("permission_id")
                )
            )
        }

        return resultat
    }

    private fun lireRolesUtilisateurs(
        json: JSONArray
    ): List<RoleUtilisateur> {
        val resultat = mutableListOf<RoleUtilisateur>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            resultat.add(
                RoleUtilisateur(
                    utilisateurId = item.optString("utilisateur_id"),
                    roleId = item.optString("role_id")
                )
            )
        }

        return resultat
    }


    private fun mutation(
        context: Context,
        table: String,
        method: String,
        filtre: String = "",
        donnees: JSONObject? = null,
        callback: (Boolean, String) -> Unit
    ) {
        AuthService.obtenirProfil(context) { okProfil, messageProfil, profil ->
            if (!okProfil || profil == null) {
                callback(false, messageProfil)
                return@obtenirProfil
            }
            val token = AuthService.obtenirAccessToken(context)
            val utilisateurId = AuthService.obtenirUtilisateurId(context)
            if (token.isNullOrBlank() || utilisateurId.isNullOrBlank()) {
                callback(false, "Session expirée. Reconnecte-toi.")
                return@obtenirProfil
            }

            val url = "${SupabaseConfig.url}/rest/v1/$table" +
                "?entreprise_id=eq.${profil.entrepriseId}" + filtre
            val builder = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.key)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")

            val body = donnees?.toString()?.toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )
            val request = when (method) {
                "POST" -> builder.post(body ?: "{}".toRequestBody("application/json".toMediaType())).build()
                "PATCH" -> builder.patch(body ?: "{}".toRequestBody("application/json".toMediaType())).build()
                "DELETE" -> builder.delete().build()
                else -> {
                    callback(false, "Méthode non prise en charge.")
                    return@obtenirProfil
                }
            }

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    mainHandler.post {
                        callback(false, "Erreur réseau : ${e.message ?: "connexion impossible"}")
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        val corps = it.body?.string().orEmpty()
                        mainHandler.post {
                            if (it.isSuccessful) callback(true, "Opération effectuée avec succès.")
                            else callback(false, "Erreur Supabase (${it.code}) : $corps")
                        }
                    }
                }
            })
        }
    }

    fun creerRole(
        context: Context,
        nom: String,
        description: String?,
        callback: (Boolean, String, String?) -> Unit
    ) {
        AuthService.obtenirProfil(context) { ok, message, profil ->
            if (!ok || profil == null) {
                callback(false, message, null)
                return@obtenirProfil
            }
            val token = AuthService.obtenirAccessToken(context)
            val userId = AuthService.obtenirUtilisateurId(context)
            if (token.isNullOrBlank() || userId.isNullOrBlank()) {
                callback(false, "Session expirée. Reconnecte-toi.", null)
                return@obtenirProfil
            }
            val data = JSONObject().apply {
                put("entreprise_id", profil.entrepriseId)
                put("cree_par", userId)
                put("nom", nom)
                if (description != null) put("description", description)
            }
            val request = Request.Builder()
                .url("${SupabaseConfig.url}/rest/v1/roles_personnalises")
                .addHeader("apikey", SupabaseConfig.key)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(data.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    mainHandler.post { callback(false, "Erreur réseau : ${e.message}", null) }
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        mainHandler.post {
                            if (!it.isSuccessful) callback(false, "Erreur Supabase (${it.code}) : $body", null)
                            else {
                                val id = try { JSONArray(body).optJSONObject(0)?.optString("id") } catch (_: Exception) { null }
                                callback(true, "Rôle créé.", id)
                            }
                        }
                    }
                }
            })
        }
    }

    fun modifierRole(context: Context, roleId: String, nom: String, description: String?, callback: (Boolean, String) -> Unit) {
        val data = JSONObject().put("nom", nom).put("description", description)
        mutation(context, "roles_personnalises", "PATCH", "&id=eq.$roleId", data, callback)
    }

    fun supprimerRole(context: Context, roleId: String, callback: (Boolean, String) -> Unit) {
        mutation(context, "roles_personnalises", "DELETE", "&id=eq.$roleId", callback = callback)
    }

    fun attribuerPermissionRole(context: Context, roleId: String, permissionId: String, callback: (Boolean, String) -> Unit) {
        val data = JSONObject().put("role_id", roleId).put("permission_id", permissionId)
        AuthService.obtenirProfil(context) { ok, message, profil ->
            if (!ok || profil == null) { callback(false, message); return@obtenirProfil }
            data.put("entreprise_id", profil.entrepriseId)
            mutation(context, "role_permissions", "POST", donnees = data, callback = callback)
        }
    }

    fun retirerPermissionRole(context: Context, roleId: String, permissionId: String, callback: (Boolean, String) -> Unit) {
        mutation(context, "role_permissions", "DELETE", "&role_id=eq.$roleId&permission_id=eq.$permissionId", callback = callback)
    }

    fun attribuerRoleUtilisateur(context: Context, utilisateurId: String, roleId: String, callback: (Boolean, String) -> Unit) {
        AuthService.obtenirProfil(context) { ok, message, profil ->
            if (!ok || profil == null) { callback(false, message); return@obtenirProfil }
            val userId = AuthService.obtenirUtilisateurId(context)
            if (userId.isNullOrBlank()) { callback(false, "Session expirée."); return@obtenirProfil }
            val data = JSONObject()
                .put("entreprise_id", profil.entrepriseId)
                .put("utilisateur_id", utilisateurId)
                .put("role_id", roleId)
                .put("attribue_par", userId)
            mutation(context, "utilisateur_roles", "POST", donnees = data, callback = callback)
        }
    }

    fun retirerRoleUtilisateur(context: Context, utilisateurId: String, roleId: String, callback: (Boolean, String) -> Unit) {
        mutation(
            context,
            "utilisateur_roles",
            "DELETE",
            "&utilisateur_id=eq.$utilisateurId&role_id=eq.$roleId",
            callback = callback
        )
    }

    fun definirPermissionIndividuelle(
        context: Context, utilisateurId: String, permissionId: String, effet: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (effet != "autoriser" && effet != "refuser") {
            callback(false, "Effet de permission invalide.")
            return
        }
        AuthService.obtenirProfil(context) { ok, message, profil ->
            if (!ok || profil == null) { callback(false, message); return@obtenirProfil }
            val userId = AuthService.obtenirUtilisateurId(context)
            if (userId.isNullOrBlank()) { callback(false, "Session expirée."); return@obtenirProfil }
            val token = AuthService.obtenirAccessToken(context)
            if (token.isNullOrBlank()) { callback(false, "Session expirée."); return@obtenirProfil }

            val url = "${SupabaseConfig.url}/rest/v1/utilisateur_permissions" +
                "?entreprise_id=eq.${profil.entrepriseId}&utilisateur_id=eq.$utilisateurId&permission_id=eq.$permissionId"
            val delete = Request.Builder().url(url)
                .addHeader("apikey", SupabaseConfig.key)
                .addHeader("Authorization", "Bearer $token")
                .delete().build()
            client.newCall(delete).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    mainHandler.post { callback(false, "Erreur réseau : ${e.message}") }
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            val body = it.body?.string().orEmpty()
                            mainHandler.post { callback(false, "Erreur Supabase (${it.code}) : $body") }
                            return
                        }
                    }
                    val data = JSONObject()
                        .put("entreprise_id", profil.entrepriseId)
                        .put("utilisateur_id", utilisateurId)
                        .put("permission_id", permissionId)
                        .put("attribue_par", userId)
                        .put("effet", effet)
                    val post = Request.Builder()
                        .url("${SupabaseConfig.url}/rest/v1/utilisateur_permissions")
                        .addHeader("apikey", SupabaseConfig.key)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Content-Type", "application/json")
                        .post(data.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(post).enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            mainHandler.post { callback(false, "Erreur réseau : ${e.message}") }
                        }
                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            response.use {
                                val body = it.body?.string().orEmpty()
                                mainHandler.post {
                                    if (it.isSuccessful) callback(true, "Permission individuelle enregistrée.")
                                    else callback(false, "Erreur Supabase (${it.code}) : $body")
                                }
                            }
                        }
                    })
                }
            })
        }
    }

    fun reinitialiserPermissionIndividuelle(context: Context, utilisateurId: String, permissionId: String, callback: (Boolean, String) -> Unit) {
        mutation(context, "utilisateur_permissions", "DELETE", "&utilisateur_id=eq.$utilisateurId&permission_id=eq.$permissionId", callback = callback)
    }

}
