package com.melmatech.app

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

private data class EmployeListe(
    val id: String,
    val matricule: String,
    val nom: String,
    val poste: String,
    val departement: String,
    val statut: String
)

private data class DocumentRH(
    val id: String,
    val nom: String,
    val categorie: String,
    val chemin: String,
    val date: String
)

private val violetRH = Color(0xFF6C3CE9)
private val fondRH = Color(0xFFF5F3FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonnel(onRetour: () -> Unit) {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var profil by remember { mutableStateOf<ProfilUtilisateur?>(null) }
    var employes by remember { mutableStateOf(emptyList<EmployeListe>()) }
    var departements by remember { mutableStateOf(emptyList<String>()) }
    var documents by remember { mutableStateOf(emptyList<DocumentRH>()) }
    var employeSelectionne by remember { mutableStateOf<EmployeListe?>(null) }
    var onglet by remember { mutableStateOf("Employés") }
    var chargement by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }
    var afficherAjout by remember { mutableStateOf(false) }
    var nom by remember { mutableStateOf("") }
    var postnom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var poste by remember { mutableStateOf("") }
    var departement by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var typeContrat by remember { mutableStateOf("CDI") }
    var enregistrement by remember { mutableStateOf(false) }
    var chargementDocuments by remember { mutableStateOf(false) }
    var categorieDocument by remember { mutableStateOf("Contrat") }

    val estRH = profil?.role?.trim()?.lowercase() == "responsable_rh"

    fun authHeaders(builder: Request.Builder, token: String): Request.Builder =
        builder.addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")

    fun chargerDocuments(employe: EmployeListe) {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        chargementDocuments = true
        val url = "${SupabaseConfig.url}/rest/v1/documents_employes?entreprise_id=eq.${p.entrepriseId}&employe_id=eq.${employe.id}&select=id,nom_fichier,categorie,chemin_storage,cree_le&order=cree_le.desc"
        val request = authHeaders(Request.Builder().url(url), token).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { chargementDocuments = false; message = "Erreur de chargement des dossiers : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    mainHandler.post {
                        chargementDocuments = false
                        if (!it.isSuccessful) { message = "Impossible de charger les dossiers (${it.code})."; return@post }
                        try {
                            val array = JSONArray(body)
                            documents = (0 until array.length()).map { i ->
                                val d = array.getJSONObject(i)
                                DocumentRH(d.optString("id"), d.optString("nom_fichier"), d.optString("categorie"), d.optString("chemin_storage"), d.optString("cree_le"))
                            }
                            message = if (documents.isEmpty()) "Aucun document dans ce dossier pour le moment." else ""
                        } catch (_: Exception) { message = "Réponse des dossiers illisible." }
                    }
                }
            }
        })
    }

    fun chargerEmployes() {
        chargement = true
        message = ""
        AuthService.obtenirProfil(context) { okProfil, texteProfil, p ->
            if (!okProfil || p == null) {
                chargement = false
                message = texteProfil
                return@obtenirProfil
            }
            profil = p
            val token = AuthService.obtenirAccessToken(context)
            if (token.isNullOrBlank()) {
                chargement = false
                message = "Session expirée. Reconnecte-toi."
                return@obtenirProfil
            }
            val url = "${SupabaseConfig.url}/rest/v1/employes?entreprise_id=eq.${p.entrepriseId}&select=id,matricule,nom,postnom,prenom,poste,departement,statut&order=nom.asc"
            val request = authHeaders(Request.Builder().url(url), token).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mainHandler.post { chargement = false; message = "Erreur réseau : ${e.message ?: "connexion impossible"}" }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        mainHandler.post {
                            chargement = false
                            if (!it.isSuccessful) { message = "Impossible de charger le personnel (${it.code})."; return@post }
                            try {
                                val array = JSONArray(body)
                                employes = (0 until array.length()).map { index ->
                                    val item = array.getJSONObject(index)
                                    EmployeListe(
                                        id = item.optString("id"),
                                        matricule = item.optString("matricule"),
                                        nom = listOf(item.optString("prenom"), item.optString("nom"), item.optString("postnom"))
                                            .filter { part -> part.isNotBlank() && part != "null" }.joinToString(" "),
                                        poste = item.optString("poste").takeUnless { v -> v == "null" }.orEmpty(),
                                        departement = item.optString("departement").takeUnless { v -> v == "null" }.orEmpty(),
                                        statut = item.optString("statut", "actif")
                                    )
                                }
                                if (onglet == "Employés" && message.isBlank()) message = ""
                            } catch (_: Exception) { message = "La réponse du serveur est illisible." }
                        }
                    }
                }
            })

            val urlDepartements = "${SupabaseConfig.url}/rest/v1/departements?entreprise_id=eq.${p.entrepriseId}&actif=eq.true&select=nom&order=nom.asc"
            val reqDep = authHeaders(Request.Builder().url(urlDepartements), token).get().build()
            client.newCall(reqDep).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        if (it.isSuccessful) try {
                            val a = JSONArray(body)
                            val noms = (0 until a.length()).map { i -> a.getJSONObject(i).optString("nom") }
                            mainHandler.post { departements = noms }
                        } catch (_: Exception) { }
                    }
                }
            })
        }
    }

    fun ajouterEmploye() {
        val p = profil
        val token = AuthService.obtenirAccessToken(context)
        if (p == null || token.isNullOrBlank()) { message = "Session invalide. Reconnecte-toi."; return }
        if (nom.isBlank() || poste.isBlank()) { message = "Le nom et le poste sont obligatoires."; return }
        enregistrement = true
        val matricule = "EMP-" + UUID.randomUUID().toString().take(8).uppercase()
        val data = JSONObject()
            .put("entreprise_id", p.entrepriseId)
            .put("matricule", matricule)
            .put("nom", nom.trim())
            .put("postnom", postnom.trim().ifBlank { JSONObject.NULL })
            .put("prenom", prenom.trim().ifBlank { JSONObject.NULL })
            .put("poste", poste.trim())
            .put("departement", departement.trim().ifBlank { JSONObject.NULL })
            .put("telephone", telephone.trim().ifBlank { JSONObject.NULL })
            .put("email", email.trim().ifBlank { JSONObject.NULL })
            .put("type_contrat", typeContrat)
            .put("cree_par", p.id)
        val url = "${SupabaseConfig.url}/rest/v1/employes"
        val request = authHeaders(Request.Builder().url(url), token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(data.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { enregistrement = false; message = "Erreur réseau : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mainHandler.post {
                        enregistrement = false
                        if (it.isSuccessful) {
                            afficherAjout = false
                            nom = ""; postnom = ""; prenom = ""; poste = ""; departement = ""; telephone = ""; email = ""
                            message = "Employé ajouté avec succès."
                            chargerEmployes()
                        } else {
                            message = "Échec de l'ajout (${it.code}) : ${it.body?.string().orEmpty().take(180)}"
                        }
                    }
                }
            }
        })
    }

    val choisirDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val p = profil
        val employe = employeSelectionne
        val token = AuthService.obtenirAccessToken(context)
        if (uri == null || p == null || employe == null || token.isNullOrBlank()) return@rememberLauncherForActivityResult
        if (!estRH) { message = "Les dossiers confidentiels sont réservés au responsable RH."; return@rememberLauncherForActivityResult }
        chargementDocuments = true
        try {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty() || bytes.size > 12 * 1024 * 1024) {
                chargementDocuments = false
                message = "Fichier vide ou trop volumineux (maximum 12 Mo)."
                return@rememberLauncherForActivityResult
            }
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf { it.isNotBlank() } ?: "document-${System.currentTimeMillis()}"
            val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val path = "${p.entrepriseId}/${employe.id}/${UUID.randomUUID()}_$safeName"
            val uploadUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive/$path"
            val upload = authHeaders(Request.Builder().url(uploadUrl), token)
                .addHeader("Content-Type", mime)
                .addHeader("x-upsert", "false")
                .post(bytes.toRequestBody(mime.toMediaType()))
                .build()
            client.newCall(upload).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mainHandler.post { chargementDocuments = false; message = "Envoi du document impossible : ${e.message}" }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            val err = it.body?.string().orEmpty()
                            mainHandler.post { chargementDocuments = false; message = "Stockage refusé (${it.code}) : ${err.take(160)}" }
                            return
                        }
                    }
                    val metadata = JSONObject()
                        .put("entreprise_id", p.entrepriseId)
                        .put("employe_id", employe.id)
                        .put("nom_fichier", displayName)
                        .put("chemin_storage", path)
                        .put("categorie", categorieDocument)
                        .put("type_mime", mime)
                        .put("taille_octets", bytes.size)
                        .put("ajoute_par", p.id)
                    val metaRequest = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/documents_employes"), token)
                        .addHeader("Content-Type", "application/json")
                        .post(metadata.toString().toRequestBody("application/json; charset=utf-8"))
                        .build()
                    client.newCall(metaRequest).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            mainHandler.post { chargementDocuments = false; message = "Fichier envoyé, mais indexation impossible. Contacte le support RH." }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                mainHandler.post {
                                    chargementDocuments = false
                                    if (it.isSuccessful) { message = "Document ajouté au dossier."; chargerDocuments(employe) }
                                    else message = "Fichier envoyé, mais enregistrement du dossier refusé (${it.code})."
                                }
                            }
                        }
                    })
                }
            })
        } catch (e: Exception) {
            chargementDocuments = false
            message = "Lecture du fichier impossible : ${e.message}"
        }
    }

    LaunchedEffect(Unit) { chargerEmployes() }

    Column(
        modifier = Modifier.fillMaxSize().background(fondRH).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RESSOURCES HUMAINES", color = violetRH, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Gestion du personnel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(profil?.nomComplet.orEmpty(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRetour) { Text("Retour") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatRH("Employés", employes.size.toString(), Modifier.weight(1f))
            StatRH("Actifs", employes.count { it.statut == "actif" }.toString(), Modifier.weight(1f))
            StatRH("Inactifs", employes.count { it.statut != "actif" }.toString(), Modifier.weight(1f))
        }

        ScrollableTabRow(selectedTabIndex = listOf("Employés", "Dossiers", "Organisation", "Présences", "Congés", "Paie", "Rapports").indexOf(onglet).coerceAtLeast(0), edgePadding = 0.dp) {
            listOf("Employés", "Dossiers", "Organisation", "Présences", "Congés", "Paie", "Rapports").forEach { tab ->
                Tab(selected = onglet == tab, onClick = { onglet = tab; message = "" }, text = { Text(tab) })
            }
        }

        if (chargement) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (onglet) {
                "Employés" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(recherche, { recherche = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Rechercher un employé") })
                        Button(onClick = { afficherAjout = true }) { Text("Ajouter") }
                    }
                    val filtres = employes.filter { it.nom.contains(recherche, true) || it.matricule.contains(recherche, true) || it.poste.contains(recherche, true) }
                    if (filtres.isEmpty()) {
                        Text(if (employes.isEmpty()) "Aucun employé enregistré pour cette entreprise." else "Aucun résultat.")
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtres, key = { it.id }) { e ->
                                Card(Modifier.fillMaxWidth().clickable { employeSelectionne = e; onglet = "Dossiers"; chargerDocuments(e) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(e.nom.ifBlank { "Employé sans nom" }, fontWeight = FontWeight.Bold)
                                        Text("${e.matricule} • ${e.poste}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        Text("${e.departement.ifBlank { "Département non défini" }} • ${e.statut}", color = violetRH, style = MaterialTheme.typography.labelMedium)
                                        Text("Ouvrir le dossier  ›", color = violetRH, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                "Dossiers" -> {
                    val cible = employeSelectionne
                    if (cible == null) {
                        Text("Sélectionne un employé dans l'onglet Employés pour consulter son dossier.")
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Dossier du personnel", fontWeight = FontWeight.Bold)
                                Text(cible.nom, color = Color.Gray)
                            }
                            TextButton(onClick = { employeSelectionne = null; documents = emptyList() }) { Text("Changer") }
                        }
                        if (estRH) {
                            var menuCategorie by remember { mutableStateOf(false) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box {
                                    OutlinedButton(onClick = { menuCategorie = true }) { Text(categorieDocument) }
                                    DropdownMenu(expanded = menuCategorie, onDismissRequest = { menuCategorie = false }) {
                                        listOf("Contrat", "CV", "Diplôme", "Identité", "Photo", "Certificat", "Autre").forEach { cat ->
                                            DropdownMenuItem(text = { Text(cat) }, onClick = { categorieDocument = cat; menuCategorie = false })
                                        }
                                    }
                                }
                                Button(onClick = { choisirDocument.launch(arrayOf("*/*")) }) { Text("Ajouter un document") }
                            }
                            Text("Accès strictement réservé au responsable RH autorisé.", color = Color(0xFF8A5A00), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Les documents personnels sont confidentiels et réservés au responsable RH.", color = Color.Gray)
                        }
                        if (chargementDocuments) CircularProgressIndicator()
                        if (documents.isEmpty() && !chargementDocuments) Text(message.ifBlank { "Aucun document enregistré." }, color = Color.Gray)
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(documents, key = { it.id }) { d ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text(d.nom, fontWeight = FontWeight.SemiBold)
                                        Text("${d.categorie} • ${d.date.take(10)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                "Organisation" -> {
                    Text("Départements de l'entreprise", fontWeight = FontWeight.Bold)
                    if (departements.isEmpty()) Text("Aucun département actif enregistré.")
                    else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(departements) { dep ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Text(dep, Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("Les postes sont associés aux employés et seront administrables dans une prochaine étape.")
                }
                "Présences", "Congés", "Paie", "Rapports" -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(onglet, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Cette rubrique est prévue dans le module RH. Son écran sera activé après la mise en place de ses tables, règles de sécurité et traitements afin de ne pas afficher de fausses données.", color = Color.Gray)
                            Text("Aucune donnée fictive n'est affichée.", color = violetRH, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) Text(message, color = if (message.contains("succès") || message.contains("ajouté")) Color(0xFF16834B) else Color(0xFFB45309), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { chargerEmployes() }, modifier = Modifier.fillMaxWidth(), enabled = !chargement && !enregistrement) { Text("Actualiser les données") }
    }

    if (afficherAjout) {
        AlertDialog(
            onDismissRequest = { if (!enregistrement) afficherAjout = false },
            title = { Text("Ajouter un employé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.heightIn(max = 440.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(nom, { nom = it }, label = { Text("Nom *") }, singleLine = true)
                        OutlinedTextField(postnom, { postnom = it }, label = { Text("Postnom") }, singleLine = true)
                        OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, singleLine = true)
                        OutlinedTextField(poste, { poste = it }, label = { Text("Poste *") }, singleLine = true)
                        OutlinedTextField(departement, { departement = it }, label = { Text("Département") }, singleLine = true)
                        OutlinedTextField(telephone, { telephone = it }, label = { Text("Téléphone") }, singleLine = true)
                        OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true)
                        var menuContrat by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { menuContrat = true }) { Text("Contrat : $typeContrat") }
                            DropdownMenu(expanded = menuContrat, onDismissRequest = { menuContrat = false }) {
                                listOf("CDI", "CDD", "Journalier", "Stage", "Consultant", "Autre").forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { typeContrat = type; menuContrat = false })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { ajouterEmploye() }, enabled = !enregistrement) { Text(if (enregistrement) "Enregistrement…" else "Enregistrer") } },
            dismissButton = { TextButton(onClick = { afficherAjout = false }, enabled = !enregistrement) { Text("Annuler") } }
        )
    }
}

@Composable
private fun StatRH(titre: String, valeur: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(valeur, color = violetRH, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(titre, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
