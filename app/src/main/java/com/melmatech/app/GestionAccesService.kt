
package com.melmatech.app

import android.content.Context
import android.os.Handler
import android.os.Looper
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
}
