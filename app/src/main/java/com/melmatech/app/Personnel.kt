package com.melmatech.app

import android.os.Handler
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
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
import java.util.UUID

private val VioletRH = Color(0xFF6C3CE9)
private val FondRH = Color(0xFFF7F7FB)
private val VioletClairRH = Color(0xFFF0EBFF)
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
data class DocumentRH(val id: String, val employeId: String, val nomFichier: String, val cheminStorage: String, val categorie: String, val typeMime: String, val tailleOctets: Long, val creeLe: String)

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

    // Documents RH : schéma déjà configuré dans Supabase.
    fun chargerDocuments(token: String, entrepriseId: String, employeId: String, callback: (Boolean, String, List<DocumentRH>) -> Unit) {
        val url = "${SupabaseConfig.url}/rest/v1/documents_employes?entreprise_id=eq.$entrepriseId&employe_id=eq.$employeId&select=*&order=cree_le.desc"
        SupabaseHttp.client.newCall(req(url, token).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau", emptyList()) } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) { main.post { callback(false, "Documents RH (${it.code}) : $body", emptyList()) }; return }
                try {
                    val a = JSONArray(body)
                    val docs = (0 until a.length()).map { i ->
                        val o = a.getJSONObject(i)
                        DocumentRH(o.optString("id"), o.optString("employe_id"), o.optString("nom_fichier"), o.optString("chemin_storage"), o.optString("categorie"), o.optString("type_mime"), o.optLong("taille_octets"), o.optString("cree_le"))
                    }
                    main.post { callback(true, "", docs) }
                } catch (e: Exception) { main.post { callback(false, "Réponse documents illisible : ${e.message}", emptyList()) } }
            } }
        })
    }

    fun envoyerDocument(context: Context, token: String, entrepriseId: String, employeId: String, uri: Uri, categorie: String, callback: (Boolean, String) -> Unit) {
        try {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val mimeAutorises = setOf("image/jpeg", "image/png", "image/webp", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            if (mime !in mimeAutorises) { callback(false, "Format non autorisé. Utilisez JPG, PNG, WebP, PDF, DOC ou DOCX."); return }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("Lecture du fichier impossible.")
            if (bytes.size > 10 * 1024 * 1024) { callback(false, "Le fichier dépasse la limite de 10 Mo."); return }
            var nom = "document_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) nom = c.getString(0) ?: nom }
            val extension = nom.substringAfterLast('.', "").ifBlank { when (mime) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"; "application/pdf" -> "pdf"; else -> "doc" } }
            val chemin = "$entrepriseId/$employeId/${UUID.randomUUID()}.$extension"
            val uploadUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive/$chemin"
            val upload = req(uploadUrl, token).addHeader("Content-Type", mime).addHeader("x-upsert", "false").post(bytes.toRequestBody(mime.toMediaType())).build()
            SupabaseHttp.client.newCall(upload).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Échec de l'envoi") } }
                override fun onResponse(call: Call, response: Response) { response.use {
                    if (!it.isSuccessful) { val detail = it.body?.string().orEmpty(); main.post { callback(false, "Storage (${it.code}) : $detail") }; return }
                    val metadata = JSONObject().apply {
                        put("entreprise_id", entrepriseId); put("employe_id", employeId); put("nom_fichier", nom)
                        put("chemin_storage", chemin); put("categorie", categorie); put("type_mime", mime); put("taille_octets", bytes.size)
                    }
                    val metaReq = req("${SupabaseConfig.url}/rest/v1/documents_employes", token).addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal").post(metadata.toString().toRequestBody(jsonType)).build()
                    SupabaseHttp.client.newCall(metaReq).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) { main.post { callback(false, "Fichier envoyé, mais métadonnées non enregistrées : ${e.message}") } }
                        override fun onResponse(call: Call, response: Response) { response.use { r ->
                            if (!r.isSuccessful) {
                                main.post { callback(false, "Fichier envoyé, mais enregistrement des métadonnées refusé (${r.code}).") }
                            } else if (categorie == "Photo de profil") {
                                val photoBody = JSONObject().put("photo_url", chemin).toString().toRequestBody(jsonType)
                                val photoReq = req("${SupabaseConfig.url}/rest/v1/employes?id=eq.$employeId", token)
                                    .addHeader("Content-Type", "application/json").patch(photoBody).build()
                                SupabaseHttp.client.newCall(photoReq).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) { main.post { callback(false, "Photo envoyée, mais le profil n'a pas été actualisé : ${e.message}") } }
                                    override fun onResponse(call: Call, response: Response) { response.use { update -> main.post { callback(update.isSuccessful, if (update.isSuccessful) "Photo de profil enregistrée." else "Document ajouté, mais mise à jour de la photo refusée (${update.code}).") } } }
                                })
                            } else {
                                main.post { callback(true, "Document ajouté.") }
                            }
                        } }
                    })
                } }
            })
        } catch (e: Exception) { callback(false, e.message ?: "Impossible de lire le fichier.") }
    }

    fun urlSignee(token: String, chemin: String, callback: (Boolean, String) -> Unit) {
        val url = "${SupabaseConfig.url}/storage/v1/object/sign/rh-prive/$chemin"
        val body = JSONObject().put("expiresIn", 300).toString().toRequestBody(jsonType)
        val request = req(url, token).addHeader("Content-Type", "application/json").post(body).build()
        SupabaseHttp.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { main.post { callback(false, e.message ?: "Erreur réseau") } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val txt = it.body?.string().orEmpty()
                if (!it.isSuccessful) { main.post { callback(false, "Lecture refusée (${it.code}).") }; return }
                val path = JSONObject(txt).optString("signedURL").ifBlank { JSONObject(txt).optString("signedUrl") }
                val absolute = if (path.startsWith("http")) path else "${SupabaseConfig.url}/storage/v1$path"
                main.post { callback(absolute.isNotBlank(), absolute.ifBlank { "URL signée absente." }) }
            } }
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
    var responsableRh by remember { mutableStateOf(false) }
    var employeSelectionne by remember { mutableStateOf<Employe?>(null) }
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
                val role = profil.role.lowercase()
                autorise = role in listOf("admin", "administrateur", "manager", "responsable_rh")
                responsableRh = role == "responsable_rh"
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
                Text("Espace de gestion du personnel", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { actualiser() }, enabled = !chargement) { Icon(Icons.Default.Refresh, "Actualiser", tint = VioletRH) }
        }
        if (!autorise && !chargement) Text("Accès réservé à l'administrateur et au manager.", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.error)
        if (autorise) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = VioletRH), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("VOTRE ÉQUIPE", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
                                Text("${employes.size} employés", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Groups, null, tint = Color.White, modifier = Modifier.size(27.dp)) }
                        }
                        Spacer(Modifier.height(15.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatRH("Actifs", actifs, Modifier.weight(1f)); StatRH("Inactifs", employes.size - actifs, Modifier.weight(1f)); StatRH("Départements", departements.size, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(15.dp))
                        Button(onClick = { edition = null; formulaire = true }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VioletRH)) {
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
                OutlinedTextField(recherche, { recherche = it }, Modifier.fillMaxWidth(), placeholder = { Text("Nom, matricule, poste...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = VioletRH) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VioletRH, unfocusedBorderColor = Color(0xFFE7E5EE), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))
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
            items(affiches, key = { it.id }) { emp -> CarteRH(emp, { employeSelectionne = emp }, { edition = emp; formulaire = true }, {
                PersonnelService.changerStatut(token, emp.id, if (emp.statut.equals("actif", true)) "suspendu" else "actif") { ok, msg -> message = msg; if (ok) actualiser() }
            }) }
        }
    }
    employeSelectionne?.let { emp -> FicheEmployeRH(emp, token, entrepriseId, responsableRh, { employeSelectionne = null }, { employeSelectionne = null; edition = emp; formulaire = true }) }
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
private fun CarteRH(emp: Employe, ouvrir: () -> Unit, modifier: () -> Unit, statut: () -> Unit) {
    val actif = emp.statut.equals("actif", true)
    Card(onClick = ouvrir, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(VioletClairRH), contentAlignment = Alignment.Center) {
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
private fun FicheEmployeRH(emp: Employe, token: String, entrepriseId: String, autoriseDocuments: Boolean, fermer: () -> Unit, modifier: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var docs by remember(emp.id) { mutableStateOf<List<DocumentRH>>(emptyList()) }
    var chargementDocs by remember(emp.id) { mutableStateOf(true) }
    var messageDocs by remember(emp.id) { mutableStateOf("") }
    var categorie by remember { mutableStateOf("CV") }
    var menuCategorie by remember { mutableStateOf(false) }
    var envoi by remember { mutableStateOf(false) }
    fun actualiserDocs() {
        PersonnelService.chargerDocuments(token, entrepriseId, emp.id) { ok, msg, liste ->
            chargementDocs = false
            if (ok) docs = liste else messageDocs = msg
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            envoi = true
            PersonnelService.envoyerDocument(context, token, entrepriseId, emp.id, uri, categorie) { ok, msg ->
                envoi = false; messageDocs = msg
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (ok) actualiserDocs()
            }
        }
    }
    LaunchedEffect(emp.id, autoriseDocuments) { if (autoriseDocuments) actualiserDocs() else chargementDocs = false }
    AlertDialog(
        onDismissRequest = fermer,
        title = { Column { Text(emp.nomAffiche.ifBlank { "Employé" }, fontWeight = FontWeight.Bold); Text("${emp.poste} • ${emp.matricule}", color = Color.Gray, style = MaterialTheme.typography.bodySmall) } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Informations professionnelles", fontWeight = FontWeight.SemiBold, color = VioletRH)
                Text("Département : ${emp.departement.ifBlank { "Non renseigné" }}", style = MaterialTheme.typography.bodySmall)
                Text("Téléphone : ${emp.telephone.ifBlank { "Non renseigné" }}", style = MaterialTheme.typography.bodySmall)
                Text("Date d'embauche : ${emp.dateEmbauche.take(10).ifBlank { "Non renseignée" }}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Folder, null, tint = VioletRH); Spacer(Modifier.width(7.dp)); Text("Dossier RH", fontWeight = FontWeight.Bold) }
                if (!autoriseDocuments) {
                    Text("Les photos et documents sont réservés au responsable RH.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Espace privé • accès responsable RH uniquement", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    ExposedDropdownMenuBox(expanded = menuCategorie, onExpandedChange = { menuCategorie = !menuCategorie }) {
                        OutlinedTextField(value = categorie, onValueChange = {}, readOnly = true, label = { Text("Type de document") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuCategorie) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = menuCategorie, onDismissRequest = { menuCategorie = false }) {
                            listOf("Photo de profil", "CV", "Diplôme", "Contrat", "Pièce d'identité", "Autre").forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { categorie = c; menuCategorie = false }) }
                        }
                    }
                    Button(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }, enabled = !envoi, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = VioletRH)) {
                        Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(6.dp)); Text(if (envoi) "Envoi en cours..." else "Ajouter un document")
                    }
                    if (chargementDocs) LinearProgressIndicator(Modifier.fillMaxWidth(), color = VioletRH)
                    if (messageDocs.isNotBlank()) Text(messageDocs, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    if (!chargementDocs && docs.isEmpty()) Text("Aucun document enregistré.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    docs.forEach { doc ->
                        Card(colors = CardDefaults.cardColors(containerColor = FondRH), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, null, tint = VioletRH)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(doc.nomFichier, maxLines = 1, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("${doc.categorie} • ${if (doc.tailleOctets >= 1048576) String.format("%.1f Mo", doc.tailleOctets / 1048576.0) else "${doc.tailleOctets / 1024} Ko"}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = {
                                    PersonnelService.urlSignee(token, doc.cheminStorage) { ok, result ->
                                        if (ok) try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result))) } catch (_: Exception) { Toast.makeText(context, "Aucune application compatible.", Toast.LENGTH_SHORT).show() }
                                        else Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                    }
                                }) { Icon(Icons.Default.Visibility, "Ouvrir", tint = VioletRH) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = modifier) { Text("Modifier", color = VioletRH) } },
        dismissButton = { TextButton(onClick = fermer) { Text("Fermer") } }
    )
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
