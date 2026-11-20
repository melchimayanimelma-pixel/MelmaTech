package com.melmatech.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AccessPurple = Color(0xFF6840D9)
private val AccessBg = Color(0xFFF6F5FA)
private val AccessInk = Color(0xFF252238)
private val AccessMuted = Color(0xFF777386)
private val AccessGreen = Color(0xFF198754)
private val AccessRed = Color(0xFFC43D4B)
private val AccessBlue = Color(0xFF2867B2)

@Composable
fun EcranGestionAcces(onRetour: () -> Unit) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var users by remember { mutableStateOf(emptyList<UtilisateurAcces>()) }
    var permissions by remember { mutableStateOf(emptyList<PermissionMelma>()) }
    var roles by remember { mutableStateOf(emptyList<RolePersonnalise>()) }
    var directPermissions by remember { mutableStateOf(emptyList<PermissionIndividuelle>()) }
    var rolePermissions by remember { mutableStateOf(emptyList<PermissionRole>()) }
    var userRoles by remember { mutableStateOf(emptyList<RoleUtilisateur>()) }
    var search by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }

    var showCreateRole by remember { mutableStateOf(false) }
    var newRoleName by remember { mutableStateOf("") }
    var newRoleDescription by remember { mutableStateOf("") }
    var editingRole by remember { mutableStateOf<RolePersonnalise?>(null) }
    var editRoleName by remember { mutableStateOf("") }
    var editRoleDescription by remember { mutableStateOf("") }

    fun reload() {
        loading = true
        GestionAccesService.chargerDonnees(context) { ok, result, data ->
            loading = false
            message = result
            if (ok && data != null) {
                users = data.utilisateurs
                permissions = data.permissions
                roles = data.roles
                directPermissions = data.permissionsIndividuelles
                rolePermissions = data.permissionsRoles
                userRoles = data.rolesUtilisateurs
                if (selectedUserId != null && users.none { it.id == selectedUserId }) {
                    selectedUserId = null
                    showDetails = false
                }
            }
        }
    }

    fun finish(ok: Boolean, result: String) {
        busy = false
        message = result
        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
        if (ok) reload()
    }

    LaunchedEffect(Unit) { reload() }

    val selectedUser = users.firstOrNull { it.id == selectedUserId }
    val assignedRoles = userRoles.filter { it.utilisateurId == selectedUserId }
        .mapNotNull { link -> roles.firstOrNull { it.id == link.roleId } }
    val userOverrides = directPermissions.filter { it.utilisateurId == selectedUserId }
    val inheritedPermissionIds = userRoles.filter { it.utilisateurId == selectedUserId }
        .flatMap { link -> rolePermissions.filter { it.roleId == link.roleId }.map { it.permissionId } }
        .toSet()

    if (showCreateRole) {
        AlertDialog(
            onDismissRequest = { if (!busy) showCreateRole = false },
            title = { Text("Créer un rôle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(newRoleName, { newRoleName = it }, label = { Text("Nom du rôle") }, singleLine = true)
                    OutlinedTextField(newRoleDescription, { newRoleDescription = it }, label = { Text("Description (facultative)") })
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && newRoleName.isNotBlank(), onClick = {
                    busy = true
                    GestionAccesService.creerRole(context, newRoleName.trim(), newRoleDescription.trim().ifBlank { null }) { ok, result, _ ->
                        busy = false
                        message = result
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                        if (ok) {
                            showCreateRole = false
                            newRoleName = ""
                            newRoleDescription = ""
                            reload()
                        }
                    }
                }) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { showCreateRole = false }) { Text("Annuler") } }
        )
    }

    editingRole?.let { role ->
        AlertDialog(
            onDismissRequest = { if (!busy) editingRole = null },
            title = { Text("Modifier le rôle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(editRoleName, { editRoleName = it }, label = { Text("Nom") }, singleLine = true)
                    OutlinedTextField(editRoleDescription, { editRoleDescription = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && editRoleName.isNotBlank(), onClick = {
                    busy = true
                    GestionAccesService.modifierRole(context, role.id, editRoleName.trim(), editRoleDescription.trim().ifBlank { null }) { ok, result ->
                        editingRole = null
                        finish(ok, result)
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { editingRole = null }) { Text("Annuler") } }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AccessBg) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ADMINISTRATION", color = AccessPurple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(if (showDetails) "Fiche utilisateur" else "Gestion des accès", color = AccessInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { if (showDetails) showDetails = false else onRetour() }) {
                    Text(if (showDetails) "← Membres" else "Retour")
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccessPurple)
                        Spacer(Modifier.height(12.dp))
                        Text("Chargement des accès…", color = AccessMuted)
                    }
                }
            } else if (!showDetails) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AccessPurple)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("CONTRÔLE DES AUTORISATIONS", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("Qui peut faire quoi ?", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Choisissez un membre pour gérer ses rôles et ses permissions.", color = Color.White.copy(alpha = .9f))
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccessStat("Membres", users.size.toString(), Modifier.weight(1f))
                            AccessStat("Rôles", roles.size.toString(), Modifier.weight(1f))
                            AccessStat("Permissions", permissions.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(15.dp),
                            label = { Text("Rechercher un utilisateur") }
                        )
                    }
                    item {
                        Text("Membres de l'entreprise", color = AccessInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Touchez un membre pour ouvrir sa fiche.", color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    val filtered = users.filter {
                        it.nomComplet.contains(search, true) || it.roleBase.contains(search, true)
                    }
                    if (filtered.isEmpty()) {
                        item { Text("Aucun utilisateur trouvé.", color = AccessMuted) }
                    } else {
                        items(filtered, key = { "user_${it.id}" }) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedUserId = user.id
                                    tab = 0
                                    showDetails = true
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                                    Surface(Modifier.size(48.dp), shape = CircleShape, color = Color(0xFFEDE8FF)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(user.nomComplet.trim().firstOrNull()?.uppercase() ?: "U", color = AccessPurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(user.nomComplet, color = AccessInk, fontWeight = FontWeight.SemiBold)
                                        Text(user.roleBase.replaceFirstChar { it.uppercase() }, color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                                        Text(if (user.actif) "Compte actif" else "Compte inactif", color = if (user.actif) AccessGreen else AccessRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text("›", color = AccessPurple, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    item {
                        HorizontalDivider(color = Color(0xFFE6E2EE))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Rôles personnalisés", color = AccessInk, fontWeight = FontWeight.Bold)
                                Text("Créer et gérer les profils d'accès.", color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { showCreateRole = true }, colors = ButtonDefaults.buttonColors(containerColor = AccessPurple), shape = RoundedCornerShape(12.dp)) {
                                Text("Créer un rôle")
                            }
                        }
                    }
                    items(roles, key = { "role_${it.id}" }) { role ->
                        Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(role.nom, color = AccessInk, fontWeight = FontWeight.SemiBold)
                                if (role.description.isNotBlank()) Text(role.description, color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                                Row {
                                    TextButton(onClick = {
                                        editingRole = role
                                        editRoleName = role.nom
                                        editRoleDescription = role.description
                                    }) { Text("Modifier") }
                                    TextButton(enabled = !busy, onClick = {
                                        busy = true
                                        GestionAccesService.supprimerRole(context, role.id) { ok, result -> finish(ok, result) }
                                    }) { Text("Supprimer", color = AccessRed) }
                                }
                            }
                        }
                    }
                    if (message.isNotBlank()) item { Text(message, color = AccessMuted, style = MaterialTheme.typography.bodySmall) }
                }
            } else if (selectedUser != null) {
                Column(Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        shape = RoundedCornerShape(19.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(Modifier.size(50.dp), shape = CircleShape, color = Color(0xFFEDE8FF)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(selectedUser!!.nomComplet.trim().firstOrNull()?.uppercase() ?: "U", color = AccessPurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(selectedUser!!.nomComplet, color = AccessInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text(selectedUser!!.roleBase.replaceFirstChar { it.uppercase() }, color = AccessMuted)
                            }
                            Surface(shape = RoundedCornerShape(30.dp), color = if (selectedUser!!.actif) Color(0xFFE8F6ED) else Color(0xFFFFEAEC)) {
                                Text(if (selectedUser!!.actif) "Actif" else "Inactif", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (selectedUser!!.actif) AccessGreen else AccessRed, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = AccessPurple) {
                        listOf("Profil", "Rôles", "Permissions").forEachIndexed { index, title ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) })
                        }
                    }

                    when (tab) {
                        0 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { AccessSectionTitle("Résumé du compte", "Informations et niveau d'accès") }
                            item { AccessInfo("Nom complet", selectedUser!!.nomComplet) }
                            item { AccessInfo("Rôle de base", selectedUser!!.roleBase) }
                            item { AccessInfo("Statut", if (selectedUser!!.actif) "Actif" else "Inactif") }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccessStat("Rôles", assignedRoles.size.toString(), Modifier.weight(1f))
                                    AccessStat("Droits directs", userOverrides.size.toString(), Modifier.weight(1f))
                                    AccessStat("Droits hérités", inheritedPermissionIds.size.toString(), Modifier.weight(1f))
                                }
                            }
                            item {
                                Button(onClick = { tab = 1 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccessPurple), shape = RoundedCornerShape(12.dp)) { Text("Gérer ses rôles") }
                                OutlinedButton(onClick = { tab = 2 }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Modifier ses permissions") }
                            }
                        }
                        1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { AccessSectionTitle("Rôles de ${selectedUser!!.nomComplet}", "Attribuez un rôle pour donner un ensemble de permissions.") }
                            if (assignedRoles.isNotEmpty()) {
                                item { Text("Rôles attribués", color = AccessInk, fontWeight = FontWeight.Bold) }
                                items(assignedRoles, key = { "assigned_${it.id}" }) { role ->
                                    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(role.nom, color = AccessInk, fontWeight = FontWeight.SemiBold)
                                                Text("Attribué", color = AccessGreen, style = MaterialTheme.typography.bodySmall)
                                            }
                                            TextButton(enabled = !busy && selectedUser!!.actif, onClick = {
                                                busy = true
                                                GestionAccesService.retirerRoleUtilisateur(context, selectedUser!!.id) { ok, result -> finish(ok, result) }
                                            }) { Text("Retirer", color = AccessRed) }
                                        }
                                    }
                                }
                            }
                            item { Text("Rôles disponibles", color = AccessInk, fontWeight = FontWeight.Bold) }
                            val available = roles.filter { role -> assignedRoles.none { it.id == role.id } }
                            if (available.isEmpty()) item { Text("Aucun autre rôle disponible.", color = AccessMuted) }
                            items(available, key = { "available_${it.id}" }) { role ->
                                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(role.nom, color = AccessInk, fontWeight = FontWeight.SemiBold)
                                            Text(role.description.ifBlank { "Aucune description" }, color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Button(enabled = !busy && selectedUser!!.actif, onClick = {
                                            busy = true
                                            GestionAccesService.attribuerRoleUtilisateur(context, selectedUser!!.id, role.id) { ok, result -> finish(ok, result) }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = AccessPurple), shape = RoundedCornerShape(11.dp)) { Text("Attribuer") }
                                    }
                                }
                            }
                        }
                        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { AccessSectionTitle("Permissions", "Un refus individuel est prioritaire sur les permissions héritées des rôles.") }
                            permissions.groupBy { it.module }.toList().forEach { group ->
                                item(key = "module_${group.first}") {
                                    Text(group.first.replaceFirstChar { it.uppercase() }, color = AccessPurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                                }
                                items(group.second, key = { "permission_${it.id}" }) { permission ->
                                    val override = userOverrides.firstOrNull { it.permissionId == permission.id }
                                    val inherited = permission.id in inheritedPermissionIds
                                    val status = when (override?.effet) {
                                        "refuser" -> "Refus individuel"
                                        "autoriser" -> "Autorisé individuellement"
                                        else -> if (inherited) "Hérité d'un rôle" else "Non attribué"
                                    }
                                    val statusColor = when (override?.effet) {
                                        "refuser" -> AccessRed
                                        "autoriser" -> AccessGreen
                                        else -> if (inherited) AccessBlue else AccessMuted
                                    }
                                    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(permission.description.ifBlank { permission.id }, color = AccessInk, fontWeight = FontWeight.SemiBold)
                                            Text(permission.id, color = AccessMuted, style = MaterialTheme.typography.bodySmall)
                                            Text(status, color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                TextButton(enabled = !busy && selectedUser!!.actif, onClick = {
                                                    busy = true
                                                    GestionAccesService.definirPermissionIndividuelle(context, selectedUser!!.id, permission.id, "autoriser") { ok, result -> finish(ok, result) }
                                                }) { Text("Autoriser", color = AccessGreen) }
                                                TextButton(enabled = !busy && selectedUser!!.actif, onClick = {
                                                    busy = true
                                                    GestionAccesService.definirPermissionIndividuelle(context, selectedUser!!.id, permission.id, "refuser") { ok, result -> finish(ok, result) }
                                                }) { Text("Refuser", color = AccessRed) }
                                                if (override != null) {
                                                    TextButton(enabled = !busy && selectedUser!!.actif, onClick = {
                                                        busy = true
                                                        GestionAccesService.reinitialiserPermissionIndividuelle(context, selectedUser!!.id, permission.id) { ok, result -> finish(ok, result) }
                                                    }) { Text("Réinitialiser", color = AccessBlue) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Utilisateur introuvable.", color = AccessMuted)
                }
            }
        }
    }
}

@Composable
private fun AccessStat(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = AccessInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, color = AccessMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AccessSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = AccessInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = AccessMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AccessInfo(title: String, value: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = AccessMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AccessInk, fontWeight = FontWeight.SemiBold)
        }
    }
}
