package com.melmatech.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EcranGestionAcces(
    onRetour: () -> Unit
) {
    val context = LocalContext.current

    var chargement by remember { mutableStateOf(true) }
    var operationEnCours by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var utilisateurs by remember { mutableStateOf(emptyList<UtilisateurAcces>()) }
    var permissions by remember { mutableStateOf(emptyList<PermissionMelma>()) }
    var roles by remember { mutableStateOf(emptyList<RolePersonnalise>()) }
    var permissionsIndividuelles by remember { mutableStateOf(emptyList<PermissionIndividuelle>()) }
    var permissionsRoles by remember { mutableStateOf(emptyList<PermissionRole>()) }
    var rolesUtilisateurs by remember { mutableStateOf(emptyList<RoleUtilisateur>()) }

    var utilisateurSelectionne by remember { mutableStateOf<String?>(null) }
    var afficherCreationRole by remember { mutableStateOf(false) }
    var nomNouveauRole by remember { mutableStateOf("") }
    var descriptionNouveauRole by remember { mutableStateOf("") }
    var roleEnEdition by remember { mutableStateOf<RolePersonnalise?>(null) }
    var nomRoleEdition by remember { mutableStateOf("") }
    var descriptionRoleEdition by remember { mutableStateOf("") }
    var rolePourPermissions by remember { mutableStateOf<RolePersonnalise?>(null) }

    val violet = MaterialTheme.colorScheme.primary
    val fond = MaterialTheme.colorScheme.background
    val textePrincipal = MaterialTheme.colorScheme.onSurface
    val texteSecondaire = MaterialTheme.colorScheme.onSurfaceVariant

    fun charger() {
        chargement = true
        message = ""
        GestionAccesService.chargerDonnees(context) { ok, resultat, donnees ->
            chargement = false
            message = resultat
            if (ok && donnees != null) {
                utilisateurs = donnees.utilisateurs
                permissions = donnees.permissions
                roles = donnees.roles
                permissionsIndividuelles = donnees.permissionsIndividuelles
                permissionsRoles = donnees.permissionsRoles
                rolesUtilisateurs = donnees.rolesUtilisateurs

                if (utilisateurSelectionne == null ||
                    utilisateurs.none { it.id == utilisateurSelectionne }
                ) {
                    utilisateurSelectionne = utilisateurs.firstOrNull()?.id
                }
            }
        }
    }

    fun terminerOperation(ok: Boolean, resultat: String) {
        operationEnCours = false
        message = resultat
        Toast.makeText(context, resultat, Toast.LENGTH_LONG).show()
        if (ok) charger()
    }

    LaunchedEffect(Unit) {
        charger()
    }

    val utilisateurActuel = utilisateurs.firstOrNull {
        it.id == utilisateurSelectionne
    }

    val rolesDeUtilisateur = rolesUtilisateurs
        .filter { it.utilisateurId == utilisateurSelectionne }
        .mapNotNull { relation -> roles.firstOrNull { it.id == relation.roleId } }

    val permissionsDirectes = permissionsIndividuelles
        .filter { it.utilisateurId == utilisateurSelectionne }

    val idsPermissionsRoles = rolesUtilisateurs
        .filter { it.utilisateurId == utilisateurSelectionne }
        .flatMap { relation ->
            permissionsRoles.filter { it.roleId == relation.roleId }
                .map { it.permissionId }
        }.toSet()

    if (afficherCreationRole) {
        AlertDialog(
            onDismissRequest = { if (!operationEnCours) afficherCreationRole = false },
            title = { Text("Créer un rôle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nomNouveauRole,
                        onValueChange = { nomNouveauRole = it },
                        label = { Text("Nom du rôle") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descriptionNouveauRole,
                        onValueChange = { descriptionNouveauRole = it },
                        label = { Text("Description (facultative)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !operationEnCours && nomNouveauRole.isNotBlank(),
                    onClick = {
                        operationEnCours = true
                        GestionAccesService.creerRole(
                            context,
                            nomNouveauRole.trim(),
                            descriptionNouveauRole.trim().ifBlank { null }
                        ) { ok, resultat, _ ->
                            operationEnCours = false
                            message = resultat
                            Toast.makeText(context, resultat, Toast.LENGTH_LONG).show()
                            if (ok) {
                                afficherCreationRole = false
                                nomNouveauRole = ""
                                descriptionNouveauRole = ""
                                charger()
                            }
                        }
                    }
                ) { Text("Créer") }
            },
            dismissButton = {
                TextButton(
                    enabled = !operationEnCours,
                    onClick = { afficherCreationRole = false }
                ) { Text("Annuler") }
            }
        )
    }

    roleEnEdition?.let { role ->
        AlertDialog(
            onDismissRequest = { if (!operationEnCours) roleEnEdition = null },
            title = { Text("Modifier le rôle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nomRoleEdition,
                        onValueChange = { nomRoleEdition = it },
                        label = { Text("Nom du rôle") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descriptionRoleEdition,
                        onValueChange = { descriptionRoleEdition = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !operationEnCours && nomRoleEdition.isNotBlank(),
                    onClick = {
                        operationEnCours = true
                        GestionAccesService.modifierRole(
                            context,
                            role.id,
                            nomRoleEdition.trim(),
                            descriptionRoleEdition.trim().ifBlank { null }
                        ) { ok, resultat ->
                            operationEnCours = false
                            message = resultat
                            Toast.makeText(context, resultat, Toast.LENGTH_LONG).show()
                            if (ok) {
                                roleEnEdition = null
                                charger()
                            }
                        }
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(
                    enabled = !operationEnCours,
                    onClick = { roleEnEdition = null }
                ) { Text("Annuler") }
            }
        )
    }

    rolePourPermissions?.let { role ->
        AlertDialog(
            onDismissRequest = { rolePourPermissions = null },
            title = { Text("Permissions : ${role.nom}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cochez les permissions à attribuer en utilisant les boutons ci-dessous.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(permissions, key = { "role_permission_${it.id}" }) { permission ->
                            val dejaAttribuee = permissionsRoles.any {
                                it.roleId == role.id && it.permissionId == permission.id
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    permission.description.ifBlank { permission.id },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    enabled = !operationEnCours,
                                    onClick = {
                                        operationEnCours = true
                                        val callback: (Boolean, String) -> Unit = { ok, resultat ->
                                            operationEnCours = false
                                            message = resultat
                                            Toast.makeText(
                                                context,
                                                resultat,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            if (ok) {
                                                charger()
                                                rolePourPermissions = null
                                            }
                                        }

                                        if (dejaAttribuee) {
                                            GestionAccesService.retirerPermissionRole(
                                                context,
                                                role.id,
                                                permission.id,
                                                callback
                                            )
                                        } else {
                                            GestionAccesService.attribuerPermissionRole(
                                                context,
                                                role.id,
                                                permission.id,
                                                callback
                                            )
                                        }
                                    }
                                ) {
                                    Text(if (dejaAttribuee) "Retirer" else "Ajouter")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { rolePourPermissions = null }) { Text("Fermer") }
            }
        )
    }

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
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ADMINISTRATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Gestion des accès",
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
                        ) { Text("Retour") }
                    }
                    Text(
                        "Gérez les utilisateurs, leurs rôles et leurs autorisations.",
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
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = violet)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Chargement des accès...", color = texteSecondaire)
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CarteStatistique("Utilisateurs", utilisateurs.size.toString(), Modifier.weight(1f))
                    CarteStatistique("Rôles", roles.size.toString(), Modifier.weight(1f))
                    CarteStatistique("Permissions", permissions.size.toString(), Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rôles personnalisés",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textePrincipal
                        )
                        Text(
                            "Créer et configurer les rôles de l'entreprise.",
                            style = MaterialTheme.typography.bodySmall,
                            color = texteSecondaire
                        )
                    }
                    Button(onClick = { afficherCreationRole = true }) {
                        Text("Créer")
                    }
                }
            }

            if (roles.isEmpty()) {
                item { Text("Aucun rôle personnalisé.", color = texteSecondaire) }
            } else {
                items(roles, key = { "role_${it.id}" }) { role ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(role.nom, fontWeight = FontWeight.Bold, color = textePrincipal)
                            if (role.description.isNotBlank()) {
                                Text(role.description, color = texteSecondaire)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { rolePourPermissions = role }) {
                                    Text("Permissions")
                                }
                                OutlinedButton(onClick = {
                                    roleEnEdition = role
                                    nomRoleEdition = role.nom
                                    descriptionRoleEdition = role.description
                                }) { Text("Modifier") }
                                TextButton(
                                    enabled = !operationEnCours,
                                    onClick = {
                                        operationEnCours = true
                                        GestionAccesService.supprimerRole(context, role.id) { ok, resultat ->
                                            terminerOperation(ok, resultat)
                                        }
                                    }
                                ) { Text("Supprimer", color = Color(0xFFC62828)) }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Membres de l'entreprise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textePrincipal
                )
                Text(
                    "Sélectionnez un utilisateur pour consulter et modifier ses accès.",
                    style = MaterialTheme.typography.bodySmall,
                    color = texteSecondaire
                )
            }

            if (utilisateurs.isEmpty()) {
                item { Text(message.ifBlank { "Aucun utilisateur trouvé." }, color = texteSecondaire) }
            } else {
                items(utilisateurs, key = { "utilisateur_${it.id}" }) { utilisateur ->
                    val selectionne = utilisateur.id == utilisateurSelectionne
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            utilisateurSelectionne = utilisateur.id
                        },
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectionne) Color(0xFFEDE7FF) else Color.White
                        ),
                        border = if (selectionne) BorderStroke(1.dp, violet.copy(alpha = 0.45f)) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectionne) violet else Color(0xFFF0EDF7)
                            ) {
                                Text(
                                    utilisateur.nomComplet.trim().firstOrNull()?.uppercase() ?: "U",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = if (selectionne) Color.White else violet,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(utilisateur.nomComplet, fontWeight = FontWeight.SemiBold, color = textePrincipal)
                                Text(utilisateur.roleBase, style = MaterialTheme.typography.bodySmall, color = texteSecondaire)
                            }
                            Text(
                                if (utilisateur.actif) "Actif" else "Inactif",
                                color = if (utilisateur.actif) Color(0xFF218653) else Color(0xFFC62828),
                                style = MaterialTheme.typography.labelMedium
                            )
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
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text("PROFIL SÉLECTIONNÉ", style = MaterialTheme.typography.labelSmall, color = violet, fontWeight = FontWeight.Bold)
                            Text(utilisateurActuel.nomComplet, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textePrincipal)
                            Text("Rôle de base : ${utilisateurActuel.roleBase}", color = texteSecondaire)
                            HorizontalDivider(color = Color(0xFFECE9F2))
                            Text("Rôles personnalisés", fontWeight = FontWeight.Bold, color = textePrincipal)

                            if (rolesDeUtilisateur.isEmpty()) {
                                Text("Aucun rôle personnalisé attribué.", color = texteSecondaire)
                            } else {
                                rolesDeUtilisateur.forEach { role ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(role.nom, color = violet, fontWeight = FontWeight.SemiBold)
                                        TextButton(
                                            enabled = !operationEnCours,
                                            onClick = {
                                                operationEnCours = true
                                                GestionAccesService.retirerRoleUtilisateur(
                                                    context, utilisateurActuel.id, role.id
                                                ) { ok, resultat -> terminerOperation(ok, resultat) }
                                            }
                                        ) { Text("Retirer") }
                                    }
                                }
                            }

                            roles.filter { role -> rolesDeUtilisateur.none { it.id == role.id } }
                                .forEach { role ->
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !operationEnCours && utilisateurActuel.actif,
                                        onClick = {
                                            operationEnCours = true
                                            GestionAccesService.attribuerRoleUtilisateur(
                                                context, utilisateurActuel.id, role.id
                                            ) { ok, resultat -> terminerOperation(ok, resultat) }
                                        }
                                    ) { Text("Attribuer : ${role.nom}") }
                                }

                            HorizontalDivider(color = Color(0xFFECE9F2))
                            Text("Permissions individuelles", fontWeight = FontWeight.Bold, color = textePrincipal)
                            Text(
                                "Une permission individuelle peut autoriser ou refuser un accès, même si le rôle le permet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = texteSecondaire
                            )
                        }
                    }
                }

                permissions.groupBy { it.module }.toList().forEach { groupe ->
                    item(key = "module_${groupe.first}") {
                        Text(
                            groupe.first.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = violet,
                            modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                        )
                    }

                    items(groupe.second, key = { permission -> "permission_${permission.id}" }) { permission ->
                        val directe = permissionsDirectes.firstOrNull { it.permissionId == permission.id }
                        val vientDuRole = permission.id in idsPermissionsRoles
                        val statut = when (directe?.effet) {
                            "refuser" -> "Refus individuel"
                            "autoriser" -> "Autorisé individuellement"
                            else -> if (vientDuRole) "Autorisé par un rôle" else "Aucune attribution"
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
                                modifier = Modifier.fillMaxWidth().padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(permission.description.ifBlank { permission.id }, fontWeight = FontWeight.SemiBold, color = textePrincipal)
                                Text(permission.id, style = MaterialTheme.typography.bodySmall, color = texteSecondaire)
                                Surface(shape = RoundedCornerShape(7.dp), color = fondStatut) {
                                    Text(
                                        statut,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        color = couleur,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextButton(
                                        enabled = !operationEnCours && utilisateurActuel.actif,
                                        onClick = {
                                            operationEnCours = true
                                            GestionAccesService.definirPermissionIndividuelle(
                                                context, utilisateurActuel.id, permission.id, "autoriser"
                                            ) { ok, resultat -> terminerOperation(ok, resultat) }
                                        }
                                    ) { Text("Autoriser", color = Color(0xFF218653)) }

                                    TextButton(
                                        enabled = !operationEnCours && utilisateurActuel.actif,
                                        onClick = {
                                            operationEnCours = true
                                            GestionAccesService.definirPermissionIndividuelle(
                                                context, utilisateurActuel.id, permission.id, "refuser"
                                            ) { ok, resultat -> terminerOperation(ok, resultat) }
                                        }
                                    ) { Text("Refuser", color = Color(0xFFC62828)) }

                                    if (directe != null) {
                                        TextButton(
                                            enabled = !operationEnCours && utilisateurActuel.actif,
                                            onClick = {
                                                operationEnCours = true
                                                GestionAccesService.reinitialiserPermissionIndividuelle(
                                                    context, utilisateurActuel.id, permission.id
                                                ) { ok, resultat -> terminerOperation(ok, resultat) }
                                            }
                                        ) { Text("Réinitialiser") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            item {
                Text(
                    message,
                    color = texteSecondaire,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(valeur, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(titre, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
