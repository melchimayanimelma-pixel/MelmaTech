
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Gestion des accès",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Utilisateurs, rôles et permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            OutlinedButton(onClick = onRetour) {
                Text("Retour")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (chargement) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Chargement des accès...")
            }
        } else if (utilisateurs.isEmpty() && message.isNotBlank()) {
            Text(message)

            Button(
                onClick = { charger() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Réessayer")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sélectionner un utilisateur",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(utilisateurs, key = { it.id }) { utilisateur ->
                    val selectionne =
                        utilisateur.id == utilisateurSelectionne

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                utilisateurSelectionne = utilisateur.id
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectionne)
                                Color(0xFFE8EAF6)
                            else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    utilisateur.nomComplet,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    utilisateur.roleBase,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                if (selectionne) "Sélectionné"
                                else if (utilisateur.actif) "Actif"
                                else "Inactif",
                                color = if (utilisateur.actif)
                                    Color(0xFF2E7D32)
                                else Color.Red
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp)
            )

            if (utilisateurActuel != null) {
                Text(
                    text = utilisateurActuel.nomComplet,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Rôle de base : ${utilisateurActuel.roleBase}",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Rôles personnalisés",
                    fontWeight = FontWeight.Bold
                )

                if (rolesDeUtilisateur.isEmpty()) {
                    Text(
                        "Aucun rôle personnalisé attribué.",
                        color = Color.Gray
                    )
                } else {
                    rolesDeUtilisateur.forEach { role ->
                        Text("• ${role.nom}")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Permissions",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Vue actuelle des autorisations individuelles " +
                        "et des permissions provenant des rôles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(
                        permissions.groupBy { it.module }.toList(),
                        key = { it.first }
                    ) { groupe ->
                        Text(
                            text = groupe.first.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3949AB),
                            modifier = Modifier.padding(
                                top = 8.dp,
                                bottom = 3.dp
                            )
                        )

                        groupe.second.forEach { permission ->
                            val directe = permissionsDirectes.firstOrNull {
                                it.permissionId == permission.id
                            }

                            val vientDuRole =
                                permission.id in idsPermissionsRoles

                            val statut = when (directe?.effet) {
                                "refuser" -> "REFUS INDIVIDUEL"
                                "autoriser" -> "AUTORISÉ INDIVIDUELLEMENT"
                                else -> if (vientDuRole) {
                                    "AUTORISÉ PAR UN RÔLE"
                                } else {
                                    "AUCUNE ATTRIBUTION"
                                }
                            }

                            val couleur = when (directe?.effet) {
                                "refuser" -> Color(0xFFC62828)
                                "autoriser" -> Color(0xFF2E7D32)
                                else -> if (vientDuRole) {
                                    Color(0xFF1565C0)
                                } else {
                                    Color.Gray
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        permission.description.ifBlank {
                                            permission.id
                                        },
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        permission.id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )

                                    Text(
                                        statut,
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
        }

        if (message.isNotBlank() && !chargement &&
            utilisateurs.isNotEmpty()
        ) {
            Text(
                text = message,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
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
