
package com.melmatech.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EcranGestionAcces(
    onRetour: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var chargement by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    var utilisateurs by remember {
        mutableStateOf(emptyList<UtilisateurAcces>())
    }

    var permissions by remember {
        mutableStateOf(emptyList<PermissionMelma>())
    }

    var roles by remember {
        mutableStateOf(emptyList<RolePersonnalise>())
    }

    var permissionsIndividuelles by remember {
        mutableStateOf(emptyList<PermissionIndividuelle>())
    }

    var permissionsRoles by remember {
        mutableStateOf(emptyList<PermissionRole>())
    }

    var rolesUtilisateurs by remember {
        mutableStateOf(emptyList<RoleUtilisateur>())
    }

    var utilisateurSelectionne by remember {
        mutableStateOf<String?>(null)
    }

    fun charger() {
        chargement = true
        message = ""

        GestionAccesService.chargerDonnees(context) {
                ok, resultat, donnees ->

            chargement = false
            message = resultat

            if (ok && donnees != null) {
                utilisateurs = donnees.utilisateurs
                permissions = donnees.permissions
                roles = donnees.roles
                permissionsIndividuelles =
                    donnees.permissionsIndividuelles
                permissionsRoles = donnees.permissionsRoles
                rolesUtilisateurs = donnees.rolesUtilisateurs

                if (utilisateurSelectionne == null ||
                    utilisateurs.none {
                        it.id == utilisateurSelectionne
                    }
                ) {
                    utilisateurSelectionne =
                        utilisateurs.firstOrNull()?.id
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        charger()
    }

    val utilisateurActuel = utilisateurs.firstOrNull {
        it.id == utilisateurSelectionne
    }

    val rolesDeUtilisateur = rolesUtilisateurs
        .filter { it.utilisateurId == utilisateurSelectionne }
        .mapNotNull { relation ->
            roles.firstOrNull { it.id == relation.roleId }
        }

    val permissionsDirectes = permissionsIndividuelles
        .filter { it.utilisateurId == utilisateurSelectionne }

    val idsPermissionsRoles = rolesUtilisateurs
        .filter { it.utilisateurId == utilisateurSelectionne }
        .flatMap { relation ->
            permissionsRoles
                .filter { it.roleId == relation.roleId }
                .map { it.permissionId }
        }
        .toSet()

    val violet = Color(0xFF6C3CE9)
    val fond = Color(0xFFF4F2FA)
    val textePrincipal = Color(0xFF242033)
    val texteSecondaire = Color(0xFF777386)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(fond)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = violet)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADMINISTRATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gestion des accès",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        OutlinedButton(
                            onClick = onRetour,
                            shape = RoundedCornerShape(50),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Retour")
                        }
                    }
                    Text(
                        text = "Gérez les utilisateurs, leurs rôles et leurs autorisations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        if (chargement) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = violet)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Chargement des accès...", color = texteSecondaire)
                    }
                }
            }
        } else if (utilisateurs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = message.ifBlank { "Aucun utilisateur trouvé." },
                            color = textePrincipal
                        )
                        Button(
                            onClick = { charger() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CarteStatistique(
                        titre = "Utilisateurs",
                        valeur = utilisateurs.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CarteStatistique(
                        titre = "Rôles",
                        valeur = roles.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    CarteStatistique(
                        titre = "Permissions",
                        valeur = permissions.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Membres de l'entreprise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textePrincipal
                    )
                    Text(
                        text = "Sélectionnez un utilisateur pour consulter ses accès.",
                        style = MaterialTheme.typography.bodySmall,
                        color = texteSecondaire
                    )
                }
            }

            items(utilisateurs, key = { "utilisateur_${it.id}" }) { utilisateur ->
                val selectionne = utilisateur.id == utilisateurSelectionne
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { utilisateurSelectionne = utilisateur.id },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectionne) {
                            Color(0xFFEDE7FF)
                        } else {
                            Color.White
                        }
                    ),
                    border = if (selectionne) {
                        androidx.compose.foundation.BorderStroke(1.dp, violet.copy(alpha = 0.45f))
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectionne) violet else Color(0xFFF0EDF7)
                        ) {
                            Text(
                                text = utilisateur.nomComplet.trim()
                                    .firstOrNull()?.uppercase() ?: "U",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = if (selectionne) Color.White else violet,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = utilisateur.nomComplet,
                                fontWeight = FontWeight.SemiBold,
                                color = textePrincipal
                            )
                            Text(
                                text = utilisateur.roleBase,
                                style = MaterialTheme.typography.bodySmall,
                                color = texteSecondaire
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (selectionne) "Sélectionné"
                                else if (utilisateur.actif) "Actif" else "Inactif",
                                color = if (utilisateur.actif) Color(0xFF218653) else Color(0xFFC62828),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selectionne) {
                                Text(
                                    text = "ACTUEL",
                                    color = violet,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (utilisateurActuel != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PROFIL SÉLECTIONNÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = violet,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = utilisateurActuel.nomComplet,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textePrincipal
                            )
                            Text(
                                text = "Rôle de base : ${utilisateurActuel.roleBase}",
                                color = texteSecondaire,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            HorizontalDivider(color = Color(0xFFECE9F2))
                            Text(
                                text = "Rôles personnalisés",
                                fontWeight = FontWeight.Bold,
                                color = textePrincipal
                            )
                            if (rolesDeUtilisateur.isEmpty()) {
                                Text(
                                    "Aucun rôle personnalisé attribué.",
                                    color = texteSecondaire,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                rolesDeUtilisateur.forEach { role ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF1EDFF)
                                    ) {
                                        Text(
                                            text = role.nom,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 7.dp
                                            ),
                                            color = violet,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textePrincipal
                        )
                        Text(
                            text = "Origine et état des autorisations de cet utilisateur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = texteSecondaire
                        )
                    }
                }

                permissions.groupBy { it.module }.toList().forEach { groupe ->
                    item(key = "module_${groupe.first}") {
                        Text(
                            text = groupe.first.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = violet,
                            modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                        )
                    }

                    items(
                        groupe.second,
                        key = { permission -> "permission_${permission.id}" }
                    ) { permission ->
                        val directe = permissionsDirectes.firstOrNull {
                            it.permissionId == permission.id
                        }
                        val vientDuRole = permission.id in idsPermissionsRoles
                        val statut = when (directe?.effet) {
                            "refuser" -> "Refus individuel"
                            "autoriser" -> "Autorisé individuellement"
                            else -> if (vientDuRole) "Autorisé par un rôle"
                            else "Aucune attribution"
                        }
                        val couleur = when (directe?.effet) {
                            "refuser" -> Color(0xFFC62828)
                            "autoriser" -> Color(0xFF218653)
                            else -> if (vientDuRole) Color(0xFF2366B5) else texteSecondaire
                        }
                        val fondStatut = when (directe?.effet) {
                            "refuser" -> Color(0xFFFFEEEE)
                            "autoriser" -> Color(0xFFEAF7EF)
                            else -> if (vientDuRole) Color(0xFFEAF2FF) else Color(0xFFF1F0F4)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = permission.description.ifBlank { permission.id },
                                    fontWeight = FontWeight.SemiBold,
                                    color = textePrincipal
                                )
                                Text(
                                    text = permission.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = texteSecondaire
                                )
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = fondStatut
                                ) {
                                    Text(
                                        text = statut,
                                        modifier = Modifier.padding(
                                            horizontal = 9.dp,
                                            vertical = 5.dp
                                        ),
                                        color = couleur,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (message.isNotBlank()) {
                item {
                    Text(
                        text = message,
                        color = texteSecondaire,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CarteStatistique(
    titre: String,
    valeur: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                valeur,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                titre,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
