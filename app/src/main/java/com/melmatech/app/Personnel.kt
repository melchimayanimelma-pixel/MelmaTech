package com.melmatech.app

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val VioletRH = Color(0xFF6C3CE9)
private val FondRH = Color(0xFFF6F5FA)
private val EncreRH = Color(0xFF242033)
private val VertRH = Color(0xFF168653)

data class Employe(
    val id: String, val matricule: String, val nom: String, val postnom: String,
    val prenom: String, val telephone: String, val poste: String, val departement: String,
    val dateEmbauche: String, val photoUrl: String?, val statut: String,
    val posteId: String? = null, val departementId: String? = null
) {
    val nomAffiche: String get() = listOf(prenom, nom, postnom).filter { it.isNotBlank() }.joinToString(" ")
}
data class DepartementRH(val id: String, val nom: String)
data class PosteRH(val id: String, val nom: String, val departementId: String?)

object PersonnelService {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val main = Handler(Looper.getMainLooper())
    private fun req(url: String, token: String) = Request.Builder().url(url)
        .addHeader("apikey", SupabaseConfig.key).addHeader("Authorization", "Bearer $token")

    fun charger(token: String, entrepriseId: String, callback: (Boolean, String, List<Employe>) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/employes?entreprise_id=eq.$entrepriseId&select=*&order=cree_le.desc"
        SupabaseHttp.client.newCall(req(url, token).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau", emptyList()) } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) { main.post { callback(false, "Supabase (${it.code}) : $body", emptyList()) }; return }
                try {
                    val a = JSONArray(body)
                    val list = (0 until a.length()).map { i -> val o = a.getJSONObject(i); Employe(
                        o.optString("id"), o.optString("matricule"), o.optString("nom"), o.optString("postnom"),
                        o.optString("prenom"), o.optString("telephone"), o.optString("poste"), o.optString("departement"),
                        o.optString("date_embauche"), o.optString("photo_url").takeIf { v -> v.isNotBlank() },
                        o.optString("statut", "actif"), o.optString("poste_id").takeIf { v -> v.isNotBlank() },
                        o.optString("departement_id").takeIf { v -> v.isNotBlank() }
                    ) }
                    main.post { callback(true, "", list) }
                } catch (e: Exception) { main.post { callback(false, "Données employés illisibles : ${e.message}", emptyList()) } }
            } }
        })
    }

    fun chargerDepartements(token: String, entrepriseId: String, callback: (Boolean, String, List<DepartementRH>) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/departements?entreprise_id=eq.$entrepriseId&actif=eq.true&select=id,nom&order=nom.asc"
        chargerListe(url, token, callback) { o -> DepartementRH(o.optString("id"), o.optString("nom")) }
    }
    fun chargerPostes(token: String, entrepriseId: String, callback: (Boolean, String, List<PosteRH>) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/postes?entreprise_id=eq.$entrepriseId&actif=eq.true&select=id,nom,departement_id&order=nom.asc"
        chargerListe(url, token, callback) { o -> PosteRH(o.optString("id"), o.optString("nom"), o.optString("departement_id").takeIf { v -> v.isNotBlank() }) }
    }
    private fun <T> chargerListe(url: String, token: String, callback: (Boolean, String, List<T>) -> Unit, mapper: (JSONObject) -> T) {
        SupabaseHttp.client.newCall(req(url, token).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau", emptyList()) } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) { main.post { callback(false, "Supabase (${it.code}) : $body", emptyList()) }; return }
                try { val a = JSONArray(body); val l = (0 until a.length()).map { i -> mapper(a.getJSONObject(i)) }; main.post { callback(true, "", l) } }
                catch (e: Exception) { main.post { callback(false, "Réponse illisible : ${e.message}", emptyList()) } }
            } }
        })
    }

    fun enregistrer(token: String, entrepriseId: String, id: String?, nom: String, postnom: String,
        prenom: String, telephone: String, poste: PosteRH, departement: DepartementRH?, date: String,
        callback: (Boolean, String) -> Unit) {
        val data = JSONObject().apply {
            put("entreprise_id", entrepriseId); put("nom", nom.trim())
            put("postnom", postnom.trim().ifBlank { JSONObject.NULL }); put("prenom", prenom.trim().ifBlank { JSONObject.NULL })
            put("telephone", telephone.trim().ifBlank { JSONObject.NULL })
            // Les champs texte historiques restent renseignés pour compatibilité.
            put("poste", poste.nom); put("departement", departement?.nom ?: JSONObject.NULL)
            put("poste_id", poste.id); put("departement_id", departement?.id ?: JSONObject.NULL)
            if (date.isNotBlank()) put("date_embauche", date)
        }
        val url = if (id == null) "${SupabaseConfig.url}/rest/v1/employes" else "${SupabaseConfig.url}/rest/v1/employes?id=eq.$id"
        val b = req(url, token).addHeader("Content-Type", "application/json").addHeader("Prefer", "return=representation")
        val body = data.toString().toRequestBody(jsonType)
        val request = if (id == null) b.post(body).build() else b.patch(body).build()
        SupabaseHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau") } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val msg = if (it.isSuccessful) "Employé enregistré. Matricule généré automatiquement." else "Supabase (${it.code}) : ${it.body?.string().orEmpty()}"
                main.post { callback(it.isSuccessful, msg) }
            } }
        })
    }
    fun changerStatut(token: String, id: String, statut: String, callback: (Boolean, String) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/employes?id=eq.$id"
        val body = JSONObject().put("statut", statut).toString().toRequestBody(jsonType)
        SupabaseHttp.client.newCall(req(url, token).addHeader("Content-Type", "application/json").patch(body).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau") } }
            override fun onResponse(call: Call, response: Response) { response.use { main.post { callback(it.isSuccessful, if (it.isSuccessful) "Statut mis à jour." else "Erreur Supabase (${it.code}).") } } }
        })
    }
}
private object SupabaseHttp { val client = okhttp3.OkHttpClient() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPersonnel(onRetour: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var token by remember { mutableStateOf(AuthService.obtenirAccessToken(context).orEmpty()) }
    var entrepriseId by remember { mutableStateOf("") }
    var autorise by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(true) }
    var employes by remember { mutableStateOf<List<Employe>>(emptyList()) }
    var departements by remember { mutableStateOf<List<DepartementRH>>(emptyList()) }
    var postes by remember { mutableStateOf<List<PosteRH>>(emptyList()) }
    var recherche by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var formulaire by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Employe?>(null) }
    var filtre by remember { mutableStateOf("Tous") }

    fun actualiser() {
        if (token.isBlank() || entrepriseId.isBlank()) return
        chargement = true
        PersonnelService.charger(token, entrepriseId) { ok, msg, l -> if (ok) employes = l else message = msg
            PersonnelService.chargerDepartements(token, entrepriseId) { okD, msgD, d -> if (okD) departements = d else if (message.isBlank()) message = msgD
                PersonnelService.chargerPostes(token, entrepriseId) { okP, msgP, p -> if (okP) postes = p else if (message.isBlank()) message = msgP; chargement = false }
            }
        }
    }
    LaunchedEffect(Unit) {
        AuthService.obtenirProfil(context) { ok, msg, profil ->
            if (ok && profil != null) {
                autorise = profil.role.lowercase() in listOf("admin", "administrateur", "manager")
                entrepriseId = profil.entrepriseId; token = AuthService.obtenirAccessToken(context).orEmpty()
                if (autorise && entrepriseId.isNotBlank()) actualiser() else { chargement = false; message = "Profil ou entreprise introuvable." }
            } else { chargement = false; message = msg }
        }
    }
    val actifs = employes.count { it.statut.equals("actif", true) }
    val affiches = employes.filter { e ->
        val q = e.nomAffiche.contains(recherche, true) || e.matricule.contains(recherche, true) || e.poste.contains(recherche, true) || e.departement.contains(recherche, true)
        q && when (filtre) { "Actifs" -> e.statut.equals("actif", true); "Inactifs" -> !e.statut.equals("actif", true); else -> true }
    }

    Column(Modifier.fillMaxSize().background(FondRH)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetour) { Icon(Icons.Default.ArrowBack, "Retour", tint = EncreRH) }
            Column(Modifier.weight(1f)) {
                Text("Ressources humaines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EncreRH)
                Text("Gestion du personnel", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { actualiser() }, enabled = !chargement) { Icon(Icons.Default.Refresh, "Actualiser", tint = VioletRH) }
        }
        if (!autorise && !chargement) Text("Accès réservé à l'administrateur et au manager.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error)
        if (autorise) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = VioletRH)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("VOTRE ÉQUIPE", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
                                Text("${employes.size} employés", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = Color.White, modifier = Modifier.size(27.dp)) }
                        }
                        Spacer(Modifier.height(15.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatRH("Actifs", actifs, Modifier.weight(1f)); StatRH("Inactifs", employes.size - actifs, Modifier.weight(1f)); StatRH("Départements", departements.size, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(15.dp))
                        Button(onClick = { edition = null; formulaire = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VioletRH)) {
                            Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Ajouter un employé", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InfoRH("${departements.size} départements", Modifier.weight(1f))
                    InfoRH("${postes.size} postes", Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(recherche, { recherche = it }, Modifier.fillMaxWidth(), placeholder = { Text("Nom, matricule, poste...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(15.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Tous", "Actifs", "Inactifs").forEach { f ->
                    FilterChip(selected = filtre == f, onClick = { filtre = f }, label = { Text(f) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEAE3FF), selectedLabelColor = VioletRH))
                } }
            }
            if (chargement) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = VioletRH) }
            if (message.isNotBlank()) item { Text(message, color = if (message.contains("Erreur", true)) MaterialTheme.colorScheme.error else Color.Gray, style = MaterialTheme.typography.bodySmall) }
            if (!chargement && affiches.isEmpty()) item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, null, tint = VioletRH, modifier = Modifier.size(36.dp)); Spacer(Modifier.height(8.dp))
                        Text(if (recherche.isBlank()) "Aucun employé enregistré" else "Aucun résultat", fontWeight = FontWeight.SemiBold)
                        Text("Les employés de votre entreprise apparaîtront ici.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(affiches, key = { it.id }) { emp -> CarteRH(emp, { edition = emp; formulaire = true }, {
                PersonnelService.changerStatut(token, emp.id, if (emp.statut.equals("actif", true)) "suspendu" else "actif") { ok, msg -> message = msg; if (ok) actualiser() }
            }) }
        }
    }
    if (formulaire) FormulaireRH(edition, departements, postes, { nom, postnom, prenom, tel, poste, dep, date ->
        PersonnelService.enregistrer(token, entrepriseId, edition?.id, nom, postnom, prenom, tel, poste, dep, date) { ok, msg -> message = msg; if (ok) { formulaire = false; actualiser() } }
    }, { formulaire = false })
}

@Composable
private fun StatRH(label: String, valeur: Int, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .13f)).padding(10.dp)) {
        Text(valeur.toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall)
    }
}
@Composable
private fun InfoRH(texte: String, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(13.dp)).background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texte, color = EncreRH, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}
@Composable
private fun CarteRH(emp: Employe, modifier: () -> Unit, statut: () -> Unit) {
    val actif = emp.statut.equals("actif", true)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEDE7FF)), contentAlignment = Alignment.Center) {
                Text(emp.nomAffiche.firstOrNull()?.uppercase() ?: "E", color = VioletRH, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(emp.nomAffiche.ifBlank { "Employé" }, color = EncreRH, fontWeight = FontWeight.SemiBold)
                Text("${emp.poste} • ${emp.matricule}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                if (emp.departement.isNotBlank()) Text(emp.departement, color = VioletRH, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp)); Text(if (actif) "● Actif" else "● ${emp.statut}", color = if (actif) VertRH else Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = modifier) { Icon(Icons.Default.Edit, "Modifier", tint = VioletRH) }
                TextButton(onClick = statut, contentPadding = PaddingValues(horizontal = 3.dp)) { Text(if (actif) "Désactiver" else "Réactiver", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormulaireRH(emp: Employe?, departements: List<DepartementRH>, postes: List<PosteRH>,
    enregistrer: (String, String, String, String, PosteRH, DepartementRH?, String) -> Unit, fermer: () -> Unit) {
    var nom by remember(emp) { mutableStateOf(emp?.nom.orEmpty()) }
    var postnom by remember(emp) { mutableStateOf(emp?.postnom.orEmpty()) }
    var prenom by remember(emp) { mutableStateOf(emp?.prenom.orEmpty()) }
    var tel by remember(emp) { mutableStateOf(emp?.telephone.orEmpty()) }
    var date by remember(emp) { mutableStateOf(emp?.dateEmbauche.orEmpty().take(10).ifBlank { java.time.LocalDate.now().toString() }) }
    var dep by remember(emp, departements) { mutableStateOf(departements.firstOrNull { it.id == emp?.departementId } ?: departements.firstOrNull { it.nom.equals(emp?.departement, true) }) }
    var poste by remember(emp, postes) { mutableStateOf(postes.firstOrNull { it.id == emp?.posteId } ?: postes.firstOrNull { it.nom.equals(emp?.poste, true) }) }
    var menuDep by remember { mutableStateOf(false) }; var menuPoste by remember { mutableStateOf(false) }
    var sauvegarde by remember { mutableStateOf(false) }
    val postesDisponibles = postes.filter { it.departementId == null || dep == null || it.departementId == dep?.id }
    AlertDialog(onDismissRequest = { if (!sauvegarde) fermer() }, title = {
        Column { Text(if (emp == null) "Nouvel employé" else "Modifier l'employé", fontWeight = FontWeight.Bold); Text("Informations professionnelles", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
    }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Le matricule sera attribué automatiquement.", color = VioletRH, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(nom, { nom = it }, Modifier.fillMaxWidth(), label = { Text("Nom *") }, singleLine = true)
            OutlinedTextField(postnom, { postnom = it }, Modifier.fillMaxWidth(), label = { Text("Postnom") }, singleLine = true)
            OutlinedTextField(prenom, { prenom = it }, Modifier.fillMaxWidth(), label = { Text("Prénom") }, singleLine = true)
            OutlinedTextField(tel, { tel = it }, Modifier.fillMaxWidth(), label = { Text("Téléphone") }, singleLine = true)
            ExposedDropdownMenuBox(expanded = menuDep, onExpandedChange = { menuDep = !menuDep }) {
                OutlinedTextField(dep?.nom ?: "Sans département", {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true, label = { Text("Département") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuDep) })
                ExposedDropdownMenu(expanded = menuDep, onDismissRequest = { menuDep = false }) {
                    DropdownMenuItem(text = { Text("Sans département") }, onClick = { dep = null; poste = null; menuDep = false })
                    departements.forEach { d -> DropdownMenuItem(text = { Text(d.nom) }, onClick = { dep = d; if (poste?.departementId != null && poste?.departementId != d.id) poste = null; menuDep = false }) }
                }
            }
            ExposedDropdownMenuBox(expanded = menuPoste, onExpandedChange = { menuPoste = !menuPoste }) {
                OutlinedTextField(poste?.nom ?: "Sélectionner un poste", {}, Modifier.fillMaxWidth().menuAnchor(), readOnly = true, label = { Text("Poste occupé *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuPoste) })
                ExposedDropdownMenu(expanded = menuPoste, onDismissRequest = { menuPoste = false }) {
                    postesDisponibles.forEach { p -> DropdownMenuItem(text = { Text(p.nom) }, onClick = { poste = p; menuPoste = false }) }
                }
            }
            if (departements.isEmpty() || postes.isEmpty()) Text("Configurez d'abord au moins un département et un poste dans l'organisation RH.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date d'embauche (AAAA-MM-JJ)") }, singleLine = true)
        }
    }, confirmButton = {
        Button(enabled = !sauvegarde && nom.isNotBlank() && poste != null, onClick = { poste?.let { sauvegarde = true; enregistrer(nom, postnom, prenom, tel, it, dep, date) } }, colors = ButtonDefaults.buttonColors(containerColor = VioletRH)) {
            Text(if (sauvegarde) "Enregistrement..." else "Enregistrer")
        }
    }, dismissButton = { TextButton(onClick = fermer, enabled = !sauvegarde) { Text("Annuler") } })
}
