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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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

private val VioletPersonnel = Color(0xFF6C3CE9)

 data class Employe(
    val id: String,
    val matricule: String,
    val nom: String,
    val postnom: String,
    val prenom: String,
    val telephone: String,
    val poste: String,
    val departement: String,
    val dateEmbauche: String,
    val photoUrl: String?,
    val statut: String
) {
    val nomAffiche: String get() = listOf(prenom, nom, postnom).filter { it.isNotBlank() }.joinToString(" ")
}

object PersonnelService {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val main = Handler(Looper.getMainLooper())

    fun charger(token: String, entrepriseId: String, callback: (Boolean, String, List<Employe>) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/employes?entreprise_id=eq.$entrepriseId&select=*&order=cree_le.desc"
        val request = Request.Builder().url(url)
            .addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token").get().build()
        SupabaseHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau", emptyList()) } }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        main.post { callback(false, "Supabase (${it.code}) : $body", emptyList()) }
                        return
                    }
                    try {
                        val array = JSONArray(body)
                        val result = (0 until array.length()).map { i ->
                            val o = array.getJSONObject(i)
                            Employe(o.optString("id"), o.optString("matricule"), o.optString("nom"), o.optString("postnom"), o.optString("prenom"), o.optString("telephone"), o.optString("poste"), o.optString("departement"), o.optString("date_embauche"), o.optString("photo_url").takeIf { value -> value.isNotBlank() }, o.optString("statut", "actif"))
                        }
                        main.post { callback(true, "", result) }
                    } catch (e: Exception) { main.post { callback(false, "Réponse des employés illisible : ${e.message}", emptyList()) } }
                }
            }
        })
    }

    fun enregistrer(token: String, entrepriseId: String, id: String?, nom: String, postnom: String, prenom: String, telephone: String, poste: String, departement: String, dateEmbauche: String, callback: (Boolean, String) -> Unit) {
        val data = JSONObject().apply {
            put("entreprise_id", entrepriseId); put("nom", nom.trim()); put("postnom", postnom.trim().ifBlank { JSONObject.NULL })
            put("prenom", prenom.trim().ifBlank { JSONObject.NULL }); put("telephone", telephone.trim().ifBlank { JSONObject.NULL })
            put("poste", poste.trim()); put("departement", departement.trim().ifBlank { JSONObject.NULL })
            if (dateEmbauche.isNotBlank()) put("date_embauche", dateEmbauche)
        }
        val url = if (id == null) "${SupabaseConfig.url}/rest/v1/employes" else "${SupabaseConfig.url}/rest/v1/employes?id=eq.$id"
        val builder = Request.Builder().url(url).addHeader("apikey", SupabaseConfig.key).addHeader("Authorization", "Bearer $token").addHeader("Content-Type", "application/json").addHeader("Prefer", "return=representation")
        val request = if (id == null) builder.post(data.toString().toRequestBody(jsonType)).build() else builder.patch(data.toString().toRequestBody(jsonType)).build()
        SupabaseHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau") } }
            override fun onResponse(call: Call, response: Response) { response.use { val body = it.body?.string().orEmpty(); main.post { callback(it.isSuccessful, if (it.isSuccessful) "Employé enregistré. Matricule généré automatiquement." else "Erreur Supabase (${it.code}) : $body") } } }
        })
    }

    fun changerStatut(token: String, id: String, statut: String, callback: (Boolean, String) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/employes?id=eq.$id"
        val body = JSONObject().put("statut", statut).toString().toRequestBody(jsonType)
        val request = Request.Builder().url(url).addHeader("apikey", SupabaseConfig.key).addHeader("Authorization", "Bearer $token").addHeader("Content-Type", "application/json").patch(body).build()
        SupabaseHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau") } }
            override fun onResponse(call: Call, response: Response) { response.use { main.post { callback(it.isSuccessful, if (it.isSuccessful) "Statut mis à jour." else "Erreur Supabase (${it.code}).") } } }
        })
    }
}

// Client HTTP partagé avec le service RH. Le reste du projet conserve ses services existants.
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
    var recherche by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var formulaire by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Employe?>(null) }

    fun actualiser() {
        if (token.isBlank() || entrepriseId.isBlank()) return
        chargement = true
        PersonnelService.charger(token, entrepriseId) { ok, msg, liste -> chargement = false; message = msg; if (ok) employes = liste }
    }
    LaunchedEffect(Unit) {
        AuthService.obtenirProfil(context) { ok, msg, profil ->
            if (ok && profil != null) {
                autorise = profil.role.lowercase() in listOf("admin", "administrateur", "manager")
                entrepriseId = profil.entrepriseId
                token = AuthService.obtenirAccessToken(context).orEmpty()
                if (autorise && entrepriseId.isNotBlank()) actualiser() else { chargement = false; message = "Profil ou entreprise introuvable." }
            } else { chargement = false; message = msg }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F5F9)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetour) { Icon(Icons.Default.ArrowBack, contentDescription = "Retour") }
            Column(Modifier.weight(1f)) { Text("Gestion du personnel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Employés de votre entreprise", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
            IconButton(onClick = { edition = null; formulaire = true }) { Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter", tint = VioletPersonnel) }
        }
        if (!autorise && !chargement) Text("Accès réservé à l'administrateur et au manager.", color = MaterialTheme.colorScheme.error)
        if (autorise) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = VioletPersonnel), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("${employes.size}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Employés enregistrés", color = Color.White) }
                    Button(onClick = { edition = null; formulaire = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VioletPersonnel)) { Text("Ajouter") }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = recherche, onValueChange = { recherche = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Rechercher un employé...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            if (chargement) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, color = if (message.startsWith("Erreur")) MaterialTheme.colorScheme.error else Color.Gray, style = MaterialTheme.typography.bodySmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(employes.filter { it.nomAffiche.contains(recherche, true) || it.matricule.contains(recherche, true) || it.poste.contains(recherche, true) }, key = { it.id }) { emp ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFEDE5FF)), contentAlignment = Alignment.Center) { Text(emp.nomAffiche.firstOrNull()?.uppercase() ?: "E", color = VioletPersonnel, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(emp.nomAffiche.ifBlank { "Employé" }, fontWeight = FontWeight.SemiBold)
                                Text("${emp.poste} • ${emp.matricule}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(if (emp.statut == "actif") "Actif" else emp.statut, style = MaterialTheme.typography.labelSmall, color = if (emp.statut == "actif") Color(0xFF168653) else Color.Gray)
                            }
                            TextButton(onClick = { edition = emp; formulaire = true }) { Text("Modifier") }
                            TextButton(onClick = { PersonnelService.changerStatut(token, emp.id, if (emp.statut == "actif") "suspendu" else "actif") { ok, msg -> message = msg; if (ok) actualiser() } }) { Text(if (emp.statut == "actif") "Désactiver" else "Réactiver") }
                        }
                    }
                }
            }
        }
    }

    if (formulaire) {
        var nom by remember(edition) { mutableStateOf(edition?.nom.orEmpty()) }
        var postnom by remember(edition) { mutableStateOf(edition?.postnom.orEmpty()) }
        var prenom by remember(edition) { mutableStateOf(edition?.prenom.orEmpty()) }
        var telephone by remember(edition) { mutableStateOf(edition?.telephone.orEmpty()) }
        var poste by remember(edition) { mutableStateOf(edition?.poste.orEmpty()) }
        var departement by remember(edition) { mutableStateOf(edition?.departement.orEmpty()) }
        var date by remember(edition) { mutableStateOf(edition?.dateEmbauche.orEmpty().ifBlank { java.time.LocalDate.now().toString() }) }
        var sauvegarde by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { if (!sauvegarde) formulaire = false }, title = { Text(if (edition == null) "Ajouter un employé" else "Modifier l'employé") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Matricule attribué automatiquement après l'enregistrement.", style = MaterialTheme.typography.bodySmall, color = VioletPersonnel)
                OutlinedTextField(nom, { nom = it }, label = { Text("Nom *") }, singleLine = true)
                OutlinedTextField(postnom, { postnom = it }, label = { Text("Postnom") }, singleLine = true)
                OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, singleLine = true)
                OutlinedTextField(telephone, { telephone = it }, label = { Text("Téléphone") }, singleLine = true)
                OutlinedTextField(poste, { poste = it }, label = { Text("Poste occupé *") }, singleLine = true)
                OutlinedTextField(departement, { departement = it }, label = { Text("Département") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Date d'embauche (AAAA-MM-JJ)") }, singleLine = true)
            }
        }, confirmButton = { TextButton(enabled = !sauvegarde && nom.isNotBlank() && poste.isNotBlank(), onClick = {
            sauvegarde = true
            PersonnelService.enregistrer(token, entrepriseId, edition?.id, nom, postnom, prenom, telephone, poste, departement, date) { ok, msg -> sauvegarde = false; message = msg; if (ok) { formulaire = false; actualiser() } }
        }) { Text(if (sauvegarde) "Enregistrement..." else "Enregistrer") } }, dismissButton = { TextButton(onClick = { formulaire = false }) { Text("Annuler") } })
    }
}
