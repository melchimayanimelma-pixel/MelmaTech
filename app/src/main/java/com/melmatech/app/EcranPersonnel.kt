package com.melmatech.app

import android.net.Uri
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val statut: String,
    val telephone: String = "",
    val email: String = "",
    val typeContrat: String = "",
    val nomFamille: String = "",
    val postnom: String = "",
    val prenom: String = "",
    val sexe: String = "",
    val dateNaissance: String = "",
    val adresse: String = "",
    val dateEmbauche: String = "",
    val salaireBase: String = "",
    val notes: String = ""
)

private data class DocumentRH(
    val id: String,
    val nom: String,
    val categorie: String,
    val chemin: String,
    val date: String,
    val typeMime: String
)

private data class PosteRH(
    val id: String,
    val nom: String,
    val description: String,
    val actif: Boolean
)

private data class PresenceRH(
    val id: String, val employe: String, val date: String,
    val arrivee: String, val depart: String, val statut: String
)
private data class CongeRH(
    val id: String, val employeId: String, val employe: String,
    val type: String, val debut: String, val fin: String,
    val motif: String, val statut: String
)
private data class PaieRH(
    val id: String, val employe: String, val debut: String,
    val fin: String, val base: Double, val primes: Double,
    val retenues: Double, val net: Double, val statut: String
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
    var postes by remember { mutableStateOf(emptyList<PosteRH>()) }
    var documents by remember { mutableStateOf(emptyList<DocumentRH>()) }
    var employeSelectionne by remember { mutableStateOf<EmployeListe?>(null) }
    // Sélection indépendante : l’onglet Dossiers commence toujours par l’annuaire.
    var dossierEmployeOuvert by remember { mutableStateOf<EmployeListe?>(null) }
    var departementSelectionne by remember { mutableStateOf<String?>(null) }
    var onglet by remember { mutableStateOf("Accueil RH") }
    var message by remember { mutableStateOf("") }
    val historiqueOnglets = remember { mutableStateListOf("Accueil RH") }

    fun naviguerVers(destination: String) {
        if (onglet != destination) {
            historiqueOnglets.add(destination)
            onglet = destination
        }
    }

    fun revenirOngletPrecedent() {
        if (historiqueOnglets.size > 1) {
            historiqueOnglets.removeAt(historiqueOnglets.lastIndex)
            onglet = historiqueOnglets.last()
        } else {
            onglet = "Accueil RH"
        }
        message = ""
    }

    var chargement by remember { mutableStateOf(true) }
    var recherche by remember { mutableStateOf("") }
    var afficherAjout by remember { mutableStateOf(false) }
    var afficherModification by remember { mutableStateOf(false) }
    var employeEnEdition by remember { mutableStateOf<EmployeListe?>(null) }
    var editNom by remember { mutableStateOf("") }
    var editPostnom by remember { mutableStateOf("") }
    var editPrenom by remember { mutableStateOf("") }
    var editPoste by remember { mutableStateOf("") }
    var editDepartement by remember { mutableStateOf("") }
    var editTelephone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editTypeContrat by remember { mutableStateOf("CDI") }
    var editSexe by remember { mutableStateOf("") }
    var editDateNaissance by remember { mutableStateOf("") }
    var editAdresse by remember { mutableStateOf("") }
    var editDateEmbauche by remember { mutableStateOf("") }
    var editSalaireBase by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editStatut by remember { mutableStateOf("actif") }
    var enregistrementModification by remember { mutableStateOf(false) }
    var nom by remember { mutableStateOf("") }
    var postnom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var poste by remember { mutableStateOf("") }
    var departement by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var typeContrat by remember { mutableStateOf("CDI") }
    var sexe by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var dateEmbauche by remember { mutableStateOf("") }
    var salaireBase by remember { mutableStateOf("") }
    var notesRH by remember { mutableStateOf("") }
    var enregistrement by remember { mutableStateOf(false) }
    var chargementDocuments by remember { mutableStateOf(false) }
    var documentASupprimer by remember { mutableStateOf<DocumentRH?>(null) }
    var suppressionDocument by remember { mutableStateOf(false) }
    var categorieDocument by remember { mutableStateOf("Contrat") }
    var documentApercu by remember { mutableStateOf<DocumentRH?>(null) }
    var pagesApercu by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var chargementApercu by remember { mutableStateOf(false) }
    var erreurApercu by remember { mutableStateOf("") }
    var afficherDepartement by remember { mutableStateOf(false) }
    var nouveauDepartement by remember { mutableStateOf("") }
    var descriptionDepartement by remember { mutableStateOf("") }
    var afficherPoste by remember { mutableStateOf(false) }
    var nouveauPoste by remember { mutableStateOf("") }
    var descriptionPoste by remember { mutableStateOf("") }
    var presences by remember { mutableStateOf(emptyList<PresenceRH>()) }
    var conges by remember { mutableStateOf(emptyList<CongeRH>()) }
    var filtreConges by remember { mutableStateOf("Tous") }
    var bulletins by remember { mutableStateOf(emptyList<PaieRH>()) }
    var filtrePaie by remember { mutableStateOf("Tous") }
    var filtreStatutEmploye by remember { mutableStateOf("Tous") }
    var filtreContratEmploye by remember { mutableStateOf("Tous") }
    var filtreHistoriquePresence by remember { mutableStateOf("Aujourd'hui") }
    var chargementModule by remember { mutableStateOf(false) }
    var afficherDemandeConge by remember { mutableStateOf(false) }
    var employeConge by remember { mutableStateOf("") }
    var typeConge by remember { mutableStateOf("Annuel") }
    var debutConge by remember { mutableStateOf("") }
    var finConge by remember { mutableStateOf("") }
    var motifConge by remember { mutableStateOf("") }
    var afficherBulletin by remember { mutableStateOf(false) }
    var employePaie by remember { mutableStateOf("") }
    var basePaie by remember { mutableStateOf("") }
    var primesPaie by remember { mutableStateOf("0") }
    var retenuesPaie by remember { mutableStateOf("0") }
    var debutPaie by remember { mutableStateOf("") }
    var finPaie by remember { mutableStateOf("") }
    val scrollAjoutEmploye = rememberScrollState()
    LaunchedEffect(afficherAjout) { if (afficherAjout) scrollAjoutEmploye.scrollTo(0) }

    val estRH = profil?.role?.trim()?.lowercase() == "responsable_rh"

    fun authHeaders(builder: Request.Builder, token: String): Request.Builder =
        builder.addHeader("apikey", SupabaseConfig.key)
            .addHeader("Authorization", "Bearer $token")

    fun ajouterDepartement() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        if (nouveauDepartement.isBlank()) { message = "Le nom du département est obligatoire."; return }
        val payload = JSONObject().put("entreprise_id", p.entrepriseId).put("nom", nouveauDepartement.trim())
            .put("description", descriptionDepartement.trim().ifBlank { JSONObject.NULL })
        val req = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/departements"), token)
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { message = "Création du département impossible : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use { mainHandler.post {
                if (it.isSuccessful) {
                    val nomCree = nouveauDepartement.trim()
                    afficherDepartement = false; nouveauDepartement = ""; descriptionDepartement = ""
                    departements = (departements + nomCree).filter { nom -> nom.isNotBlank() }.distinct().sorted()
                    message = "Département ajouté."
                } else message = "Création refusée (${it.code}) : ${it.body?.string().orEmpty().take(120)}"
            } } }
        })
    }

    fun chargerPresences() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        chargementModule = true
        val url = "${SupabaseConfig.url}/rest/v1/presences_employes?entreprise_id=eq.${p.entrepriseId}&select=id,employe_id,date_presence,heure_arrivee,heure_depart,statut,employes(nom,postnom,prenom)&order=date_presence.desc"
        val req = authHeaders(Request.Builder().url(url), token).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { chargementModule = false; message = "Erreur de chargement des présences : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                mainHandler.post {
                    chargementModule = false
                    if (!it.isSuccessful) { message = "Chargement des présences impossible (${it.code})."; return@post }
                    try {
                        val a = JSONArray(body)
                        presences = (0 until a.length()).map { i ->
                            val x = a.getJSONObject(i); val e = x.optJSONObject("employes")
                            PresenceRH(x.optString("id"), listOf(e?.optString("prenom"), e?.optString("nom"), e?.optString("postnom")).filterNotNull().filter { n -> n.isNotBlank() && n != "null" }.joinToString(" ").ifBlank { "Employé" }, x.optString("date_presence"), x.optString("heure_arrivee").take(5).let { v -> if (v == "null") "—" else v }, x.optString("heure_depart").take(5).let { v -> if (v == "null") "—" else v }, x.optString("statut"))
                        }
                    } catch (_: Exception) { message = "Format des présences illisible." }
                }
            } }
        })
    }

    fun enregistrerPresence(employe: EmployeListe, statut: String = "present") {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val payload = JSONObject().put("entreprise_id", p.entrepriseId).put("employe_id", employe.id)
            .put("statut", statut).put("cree_par", p.id)
            .put("heure_arrivee", if (statut == "present" || statut == "retard") now else JSONObject.NULL)
        val req = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/presences_employes"), token)
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { message = "Pointage impossible : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use { mainHandler.post {
                if (it.isSuccessful) { message = "Pointage enregistré pour ${employe.nom}."; chargerPresences() }
                else message = "Pointage refusé (${it.code}) : ${it.body?.string().orEmpty().take(120)}"
            } } }
        })
    }

    fun chargerConges() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        chargementModule = true
        val url = "${SupabaseConfig.url}/rest/v1/demandes_conges?entreprise_id=eq.${p.entrepriseId}&select=id,employe_id,type_conge,date_debut,date_fin,motif,statut,employes(nom,postnom,prenom)&order=cree_le.desc"
        val req = authHeaders(Request.Builder().url(url), token).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { chargementModule = false; message = "Erreur de chargement des congés : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                mainHandler.post {
                    chargementModule = false
                    if (!it.isSuccessful) { message = "Chargement des congés impossible (${it.code})."; return@post }
                    try {
                        val a = JSONArray(body)
                        conges = (0 until a.length()).map { i ->
                            val x = a.getJSONObject(i); val e = x.optJSONObject("employes")
                            CongeRH(x.optString("id"), x.optString("employe_id"), listOf(e?.optString("prenom"), e?.optString("nom"), e?.optString("postnom")).filterNotNull().filter { n -> n.isNotBlank() && n != "null" }.joinToString(" ").ifBlank { "Employé" }, x.optString("type_conge"), x.optString("date_debut"), x.optString("date_fin"), x.optString("motif").takeUnless { v -> v == "null" }.orEmpty(), x.optString("statut"))
                        }
                    } catch (_: Exception) { message = "Format des congés illisible." }
                }
            } }
        })
    }

    fun enregistrerConge() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        if (employeConge.isBlank() || debutConge.isBlank() || finConge.isBlank() || finConge < debutConge) { message = "Sélectionne un employé et des dates valides."; return }
        val payload = JSONObject().put("entreprise_id", p.entrepriseId).put("employe_id", employeConge)
            .put("type_conge", typeConge).put("date_debut", debutConge).put("date_fin", finConge)
            .put("motif", motifConge.ifBlank { JSONObject.NULL }).put("cree_par", p.id)
        val req = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/demandes_conges"), token)
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { message = "Enregistrement impossible : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use { mainHandler.post {
                if (it.isSuccessful) { afficherDemandeConge = false; message = "Demande de congé enregistrée."; chargerConges() }
                else message = "Demande refusée (${it.code}) : ${it.body?.string().orEmpty().take(120)}"
            } } }
        })
    }

    fun deciderConge(id: String, statut: String) {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        val payload = JSONObject().put("statut", statut).put("decision_par", p.id)
            .put("decision_le", java.time.OffsetDateTime.now().toString())
        val req = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/demandes_conges?id=eq.$id&entreprise_id=eq.${p.entrepriseId}"), token)
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal")
            .patch(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { message = "Décision non enregistrée : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use { mainHandler.post {
                if (it.isSuccessful) { message = if (statut == "approuve") "Congé approuvé." else "Congé refusé."; chargerConges() }
                else message = "Modification refusée (${it.code})."
            } } }
        })
    }

    fun chargerPaie() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        chargementModule = true
        val url = "${SupabaseConfig.url}/rest/v1/bulletins_paie?entreprise_id=eq.${p.entrepriseId}&select=id,employe_id,periode_debut,periode_fin,salaire_base,primes,retenues,net_a_payer,statut,employes(nom,postnom,prenom)&order=periode_fin.desc"
        val req = authHeaders(Request.Builder().url(url), token).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { chargementModule = false; message = "Erreur de chargement de la paie : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use {
                val body = it.body?.string().orEmpty()
                mainHandler.post {
                    chargementModule = false
                    if (!it.isSuccessful) { message = "Chargement de la paie impossible (${it.code})."; return@post }
                    try {
                        val a = JSONArray(body)
                        bulletins = (0 until a.length()).map { i ->
                            val x = a.getJSONObject(i); val e = x.optJSONObject("employes")
                            PaieRH(x.optString("id"), listOf(e?.optString("prenom"), e?.optString("nom"), e?.optString("postnom")).filterNotNull().filter { n -> n.isNotBlank() && n != "null" }.joinToString(" ").ifBlank { "Employé" }, x.optString("periode_debut"), x.optString("periode_fin"), x.optDouble("salaire_base"), x.optDouble("primes"), x.optDouble("retenues"), x.optDouble("net_a_payer"), x.optString("statut"))
                        }
                    } catch (_: Exception) { message = "Format des bulletins illisible." }
                }
            } }
        })
    }

    fun marquerBulletinPaye(bulletin: PaieRH) {
        val p = profil ?: return
        if (!estRH) { message = "Action réservée au responsable RH autorisé."; return }
        val token = AuthService.obtenirAccessToken(context) ?: return
        val payload = JSONObject().put("statut", "paye")
        val url = "${SupabaseConfig.url}/rest/v1/bulletins_paie?id=eq.${bulletin.id}&entreprise_id=eq.${p.entrepriseId}&statut=eq.brouillon"
        val req = authHeaders(Request.Builder().url(url), token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { message = "Mise à jour du paiement impossible : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mainHandler.post {
                        if (it.isSuccessful) {
                            message = "Bulletin marqué comme payé."
                            chargerPaie()
                        } else {
                            message = "Modification du bulletin refusée (${it.code})."
                        }
                    }
                }
            }
        })
    }

    fun creerBulletin() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        val base = basePaie.toDoubleOrNull(); val primes = primesPaie.toDoubleOrNull(); val retenues = retenuesPaie.toDoubleOrNull()
        if (employePaie.isBlank() || debutPaie.isBlank() || finPaie.isBlank() || finPaie < debutPaie || base == null || primes == null || retenues == null || base < 0 || primes < 0 || retenues < 0 || base + primes < retenues) { message = "Vérifie l'employé, les dates et les montants."; return }
        val payload = JSONObject().put("entreprise_id", p.entrepriseId).put("employe_id", employePaie)
            .put("periode_debut", debutPaie).put("periode_fin", finPaie).put("salaire_base", base)
            .put("primes", primes).put("retenues", retenues).put("cree_par", p.id)
        val req = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/bulletins_paie"), token)
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainHandler.post { message = "Création du bulletin impossible : ${e.message}" } }
            override fun onResponse(call: Call, response: Response) { response.use { mainHandler.post {
                if (it.isSuccessful) { afficherBulletin = false; message = "Bulletin créé en brouillon."; chargerPaie() }
                else message = "Création refusée (${it.code}) : ${it.body?.string().orEmpty().take(120)}"
            } } }
        })
    }

    fun chargerDocuments(employe: EmployeListe) {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context) ?: return
        chargementDocuments = true
        val url = "${SupabaseConfig.url}/rest/v1/documents_employes?entreprise_id=eq.${p.entrepriseId}&employe_id=eq.${employe.id}&select=id,nom_fichier,categorie,chemin_storage,cree_le,type_mime&order=cree_le.desc"
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
                                DocumentRH(d.optString("id"), d.optString("nom_fichier"), d.optString("categorie"), d.optString("chemin_storage"), d.optString("cree_le"), d.optString("type_mime"))
                            }
                            message = if (documents.isEmpty()) "Aucun document dans ce dossier pour le moment." else ""
                        } catch (_: Exception) { message = "Réponse des dossiers illisible." }
                    }
                }
            }
        })
    }

    fun supprimerDocumentRH(document: DocumentRH) {
        val p = profil
        val token = AuthService.obtenirAccessToken(context)
        if (p == null || token.isNullOrBlank()) {
            message = "Session expirée. Reconnecte-toi."
            return
        }
        if (!estRH) {
            message = "La suppression est réservée au responsable RH autorisé."
            return
        }
        suppressionDocument = true
        val storageUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive"
        val storagePayload = JSONObject().put("prefixes", JSONArray().put(document.chemin))
        val storageRequest = authHeaders(Request.Builder().url(storageUrl), token)
            .addHeader("Content-Type", "application/json")
            .method("DELETE", storagePayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(storageRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    suppressionDocument = false
                    message = "Suppression du fichier impossible : ${e.message}"
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val detail = it.body?.string().orEmpty()
                        mainHandler.post {
                            suppressionDocument = false
                            message = "Le stockage a refusé la suppression (${it.code}) : ${detail.take(120)}"
                        }
                        return
                    }
                }
                val metadataUrl = "${SupabaseConfig.url}/rest/v1/documents_employes?id=eq.${Uri.encode(document.id)}&entreprise_id=eq.${Uri.encode(p.entrepriseId)}"
                val metadataRequest = authHeaders(Request.Builder().url(metadataUrl), token)
                    .addHeader("Prefer", "return=minimal")
                    .delete()
                    .build()
                client.newCall(metadataRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        mainHandler.post {
                            suppressionDocument = false
                            message = "Fichier supprimé du stockage, mais la fiche documentaire n'a pas pu être mise à jour. Actualise le dossier."
                            chargerDocuments(employeSelectionne ?: return@post)
                        }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            mainHandler.post {
                                suppressionDocument = false
                                if (it.isSuccessful) {
                                    if (documentApercu?.id == document.id) {
                                        documentApercu = null
                                        pagesApercu = emptyList()
                                    }
                                    documentASupprimer = null
                                    message = "Document supprimé du dossier."
                                    employeSelectionne?.let { employe -> chargerDocuments(employe) }
                                } else {
                                    message = "Fichier supprimé du stockage, mais suppression de sa fiche refusée (${it.code}). Actualise le dossier."
                                    employeSelectionne?.let { employe -> chargerDocuments(employe) }
                                }
                            }
                        }
                    }
                })
            }
        })
    }

    fun telechargerDocumentRH(document: DocumentRH) {
        val accessToken = AuthService.obtenirAccessToken(context)
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(context, "Session expirée. Reconnecte-toi.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val downloadUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive/${Uri.encode(document.chemin, "/")}"
            val extension = when (document.typeMime.lowercase()) {
                "application/pdf" -> "pdf"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> document.typeMime.substringAfter("/", "bin").substringBefore(";")
            }
            val nomFichier = if (document.nom.matches(Regex("\\d{7,}")) && !document.nom.contains('.')) {
                "${document.categorie.lowercase()}_${document.id.take(8)}.$extension"
            } else document.nom.replace(Regex("[/\\\\]"), "_")
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(nomFichier)
                .setDescription("Téléchargement du dossier RH")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .addRequestHeader("apikey", SupabaseConfig.key)
                .addRequestHeader("Authorization", "Bearer $accessToken")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nomFichier)
            } else {
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, nomFichier)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "Téléchargement lancé. Consulte les notifications.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Téléchargement impossible : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun chargerPostes(entrepriseId: String, token: String) {
        val url = "${SupabaseConfig.url}/rest/v1/postes?entreprise_id=eq.$entrepriseId&actif=eq.true&select=id,nom,description,actif&order=nom.asc"
        val request = authHeaders(Request.Builder().url(url), token).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { message = "Impossible de charger les postes : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        mainHandler.post { message = "Chargement des postes refusé (${it.code})." }
                        return
                    }
                    try {
                        val array = JSONArray(body)
                        val resultat = (0 until array.length()).map { index ->
                            val item = array.getJSONObject(index)
                            PosteRH(
                                id = item.optString("id"),
                                nom = item.optString("nom"),
                                description = item.optString("description").takeUnless { value -> value == "null" }.orEmpty(),
                                actif = item.optBoolean("actif", true)
                            )
                        }
                        mainHandler.post { postes = resultat }
                    } catch (_: Exception) {
                        mainHandler.post { message = "La liste des postes reçue est illisible." }
                    }
                }
            }
        })
    }

    fun ajouterPoste() {
        val p = profil ?: return
        val token = AuthService.obtenirAccessToken(context)
        if (token.isNullOrBlank()) { message = "Session expirée. Reconnecte-toi."; return }
        if (nouveauPoste.isBlank()) { message = "Le nom du poste est obligatoire."; return }
        val payload = JSONObject()
            .put("entreprise_id", p.entrepriseId)
            .put("nom", nouveauPoste.trim())
            .put("description", descriptionPoste.trim().ifBlank { JSONObject.NULL })
        val request = authHeaders(Request.Builder().url("${SupabaseConfig.url}/rest/v1/postes"), token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { message = "Création du poste impossible : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    val detail = if (it.isSuccessful) "" else it.body?.string().orEmpty().take(140)
                    mainHandler.post {
                        if (code in 200..299) {
                            afficherPoste = false
                            nouveauPoste = ""
                            descriptionPoste = ""
                            message = "Poste ajouté avec succès."
                            chargerPostes(p.entrepriseId, token)
                        } else {
                            message = "Création du poste refusée ($code) : $detail"
                        }
                    }
                }
            }
        })
    }

    fun chargerEmployes() {
        if (employes.isEmpty()) chargement = true
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
            val url = "${SupabaseConfig.url}/rest/v1/employes?entreprise_id=eq.${p.entrepriseId}&select=id,matricule,nom,postnom,prenom,sexe,date_naissance,adresse,photo_url,poste,departement,date_embauche,type_contrat,salaire_base,statut,notes&order=nom.asc"
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
                                        statut = item.optString("statut", "actif"),
                                        telephone = item.optString("telephone").takeUnless { v -> v == "null" }.orEmpty(),
                                        email = item.optString("email").takeUnless { v -> v == "null" }.orEmpty(),
                                        typeContrat = item.optString("type_contrat").takeUnless { v -> v == "null" }.orEmpty(),
                                        nomFamille = item.optString("nom").takeUnless { v -> v == "null" }.orEmpty(),
                                        postnom = item.optString("postnom").takeUnless { v -> v == "null" }.orEmpty(),
                                        prenom = item.optString("prenom").takeUnless { v -> v == "null" }.orEmpty(),
                                        sexe = item.optString("sexe").takeUnless { v -> v == "null" }.orEmpty(),
                                        dateNaissance = item.optString("date_naissance").takeUnless { v -> v == "null" }.orEmpty(),
                                        adresse = item.optString("adresse").takeUnless { v -> v == "null" }.orEmpty(),
                                        dateEmbauche = item.optString("date_embauche").takeUnless { v -> v == "null" }.orEmpty(),
                                        salaireBase = if (item.isNull("salaire_base")) "" else item.optString("salaire_base"),
                                        notes = item.optString("notes").takeUnless { v -> v == "null" }.orEmpty()
                                    )
                                }
                                if (onglet == "Employés" && message.isBlank()) message = ""
                            } catch (_: Exception) { message = "La réponse du serveur est illisible." }
                        }
                    }
                }
            })

            chargerPostes(p.entrepriseId, token)
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
        if (dateNaissance.isNotBlank() && !dateNaissance.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { message = "Date de naissance invalide (AAAA-MM-JJ)."; return }
        if (dateEmbauche.isNotBlank() && !dateEmbauche.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { message = "Date d'embauche invalide (AAAA-MM-JJ)."; return }
        if (salaireBase.isNotBlank() && (salaireBase.toBigDecimalOrNull() == null || salaireBase.toBigDecimalOrNull()!! < java.math.BigDecimal.ZERO)) { message = "Le salaire doit être un montant positif ou nul."; return }
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
            .put("sexe", sexe.ifBlank { JSONObject.NULL })
            .put("date_naissance", dateNaissance.trim().ifBlank { JSONObject.NULL })
            .put("adresse", adresse.trim().ifBlank { JSONObject.NULL })
            .put("salaire_base", salaireBase.trim().toBigDecimalOrNull() ?: JSONObject.NULL)
            .put("notes", notesRH.trim().ifBlank { JSONObject.NULL })
            .put("cree_par", p.id)
        if (dateEmbauche.isNotBlank()) data.put("date_embauche", dateEmbauche.trim())
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
                            sexe = ""; dateNaissance = ""; adresse = ""; dateEmbauche = ""; salaireBase = ""; notesRH = ""
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

    fun modifierEmploye() {
        val p = profil
        val cible = employeEnEdition
        val token = AuthService.obtenirAccessToken(context)
        if (p == null || cible == null || token.isNullOrBlank()) {
            message = "Session invalide. Reconnecte-toi."
            return
        }
        if (editNom.isBlank() || editPoste.isBlank()) {
            message = "Le nom et le poste sont obligatoires."
            return
        }
        if (editDateEmbauche.isBlank() || !editDateEmbauche.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            message = "La date d'embauche est obligatoire et doit respecter le format AAAA-MM-JJ."
            return
        }
        if (editDateNaissance.isNotBlank() && !editDateNaissance.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            message = "La date de naissance doit respecter le format AAAA-MM-JJ."
            return
        }
        if (editSalaireBase.isNotBlank() && (editSalaireBase.trim().toBigDecimalOrNull() == null || editSalaireBase.trim().toBigDecimalOrNull()!! < java.math.BigDecimal.ZERO)) {
            message = "Le salaire doit être un montant positif ou nul."
            return
        }
        enregistrementModification = true
        val payload = JSONObject()
            .put("nom", editNom.trim())
            .put("postnom", editPostnom.trim().ifBlank { JSONObject.NULL })
            .put("prenom", editPrenom.trim().ifBlank { JSONObject.NULL })
            .put("poste", editPoste.trim())
            .put("departement", editDepartement.trim().ifBlank { JSONObject.NULL })
            .put("telephone", editTelephone.trim().ifBlank { JSONObject.NULL })
            .put("email", editEmail.trim().ifBlank { JSONObject.NULL })
            .put("type_contrat", editTypeContrat)
            .put("sexe", editSexe.trim().ifBlank { JSONObject.NULL })
            .put("date_naissance", editDateNaissance.trim().ifBlank { JSONObject.NULL })
            .put("adresse", editAdresse.trim().ifBlank { JSONObject.NULL })
            .put("date_embauche", editDateEmbauche.trim())
            .put("salaire_base", editSalaireBase.trim().toBigDecimalOrNull() ?: JSONObject.NULL)
            .put("notes", editNotes.trim().ifBlank { JSONObject.NULL })
            .put("statut", editStatut)
            .put("modifie_le", java.time.Instant.now().toString())
        val url = "${SupabaseConfig.url}/rest/v1/employes?id=eq.${Uri.encode(cible.id)}&entreprise_id=eq.${Uri.encode(p.entrepriseId)}"
        val request = authHeaders(Request.Builder().url(url), token)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .patch(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { enregistrementModification = false; message = "Modification impossible : ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val code = it.code
                    val detail = if (it.isSuccessful) "" else it.body?.string().orEmpty().take(160)
                    mainHandler.post {
                        enregistrementModification = false
                        if (code in 200..299) {
                            afficherModification = false
                            employeEnEdition = null
                            message = "Fiche employé mise à jour."
                            chargerEmployes()
                        } else {
                            message = "Modification refusée ($code) : $detail"
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
            val displayName = try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                }
            } catch (_: Exception) { null }
                ?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
                ?: "document-${System.currentTimeMillis()}.bin"
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
                        .post(metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
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

    BackHandler(
        enabled = documentApercu != null || afficherAjout || afficherPoste || afficherDepartement || onglet != "Accueil RH"
    ) {
        when {
            documentApercu != null -> {
                documentApercu = null
                pagesApercu = emptyList()
                chargementApercu = false
            }
            afficherModification -> { afficherModification = false; employeEnEdition = null }
            afficherAjout -> afficherAjout = false
            afficherPoste -> afficherPoste = false
            afficherDepartement -> afficherDepartement = false
            else -> revenirOngletPrecedent()
        }
    }

    fun actualiserOnglet() {
        message = ""
        when (onglet) {
            "Accueil RH", "Employés", "Dossiers", "Organisation" -> chargerEmployes()
            "Présences" -> chargerPresences()
            "Congés" -> chargerConges()
            "Paie" -> chargerPaie()
            "Rapports" -> { chargerPresences(); chargerConges(); chargerPaie() }
        }
        if (onglet == "Dossiers") dossierEmployeOuvert?.let { chargerDocuments(it) }
    }

    LaunchedEffect(Unit) { chargerEmployes() }
    LaunchedEffect(onglet, profil) {
        if (profil != null) when (onglet) {
            "Employés", "Organisation" -> chargerEmployes()
            "Dossiers" -> dossierEmployeOuvert?.let { chargerDocuments(it) }
            "Présences" -> chargerPresences()
            "Congés" -> chargerConges()
            "Paie" -> chargerPaie()
            "Rapports" -> { chargerPresences(); chargerConges(); chargerPaie() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(fondRH).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MELMATECH RH", color = violetRH, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Espace RH", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(profil?.nomComplet.orEmpty(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { actualiserOnglet() }, enabled = !chargement && !chargementModule && !chargementDocuments) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = violetRH)
                }
                TextButton(onClick = onRetour) { Text("Quitter") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCompactRH("Employés", employes.size.toString())
                StatCompactRH("Actifs", employes.count { it.statut == "actif" }.toString())
                StatCompactRH("Inactifs", employes.count { it.statut != "actif" }.toString())
            }
        }

        val sectionsRH = listOf("Accueil RH", "Employés", "Dossiers", "Organisation", "Présences", "Congés", "Paie", "Rapports")
        var menuNavigationOuvert by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ESPACE DE TRAVAIL", color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(onglet, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Box {
                OutlinedButton(onClick = { menuNavigationOuvert = true }) {
                    Text("Navigation RH")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                }
                DropdownMenu(expanded = menuNavigationOuvert, onDismissRequest = { menuNavigationOuvert = false }) {
                    sectionsRH.forEach { destination ->
                        DropdownMenuItem(
                            text = { Text(destination, fontWeight = if (onglet == destination) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (onglet == destination) ({ Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = violetRH) }) else null,
                            onClick = {
                                if (onglet != destination) {
                                    historiqueOnglets.add(destination)
                                    onglet = destination
                                    message = ""
                                    departementSelectionne = null
                                }
                                menuNavigationOuvert = false
                            }
                        )
                    }
                }
            }
        }
        if (chargement) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (onglet != "Accueil RH") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(onglet, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (onglet == "Dossiers") Spacer(Modifier.weight(1f))
                }
            }
            when (onglet) {
                "Accueil RH" -> {
                    Text("Que souhaitez-vous gérer ?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CarteMenuRH("Employés", "Liste et fiches du personnel", Modifier.weight(1f)) { naviguerVers("Employés") }
                                CarteMenuRH("Dossiers", "Dossiers individuels du personnel", Modifier.weight(1f)) {
                                    dossierEmployeOuvert = null
                                    recherche = ""
                                    departementSelectionne = null
                                    naviguerVers("Dossiers")
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CarteMenuRH("Organisation", "Départements et postes", Modifier.weight(1f)) { naviguerVers("Organisation") }
                                CarteMenuRH("Présences", "Pointage et absences", Modifier.weight(1f)) { naviguerVers("Présences") }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CarteMenuRH("Congés", "Demandes et validations", Modifier.weight(1f)) { naviguerVers("Congés") }
                                CarteMenuRH("Paie", "Salaires et bulletins", Modifier.weight(1f)) { naviguerVers("Paie") }
                            }
                        }
                        item {
                            CarteMenuRH("Rapports RH", "Effectifs et indicateurs", Modifier.fillMaxWidth()) { naviguerVers("Rapports") }
                        }
                    }
                }
                "Employés" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(recherche, { recherche = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Rechercher : nom, matricule, poste, téléphone ou e-mail") })
                        Button(onClick = { afficherAjout = true }) { Text("Ajouter") }
                    }
                    val departementsDisponibles = (departements + employes.map { it.departement })
                        .filter { it.isNotBlank() && it != "null" }.distinct().sorted()
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = departementSelectionne == null, onClick = { departementSelectionne = null }, label = { Text("Tous (${employes.size})") })
                        departementsDisponibles.forEach { dep ->
                            val nombre = employes.count { it.departement.equals(dep, true) }
                            FilterChip(selected = departementSelectionne.equals(dep, true), onClick = { departementSelectionne = if (departementSelectionne.equals(dep, true)) null else dep }, label = { Text("$dep ($nombre)") })
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tous", "Actifs", "Inactifs").forEach { filtre ->
                            val nombre = when (filtre) {
                                "Actifs" -> employes.count { it.statut.equals("actif", true) }
                                "Inactifs" -> employes.count { !it.statut.equals("actif", true) }
                                else -> employes.size
                            }
                            FilterChip(
                                selected = filtreStatutEmploye == filtre,
                                onClick = { filtreStatutEmploye = filtre },
                                label = { Text("$filtre ($nombre)") }
                            )
                        }
                    }
                    val contratsDisponibles = employes.map { it.typeContrat }.filter { it.isNotBlank() && it != "null" }.distinct().sorted()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filtreContratEmploye == "Tous",
                            onClick = { filtreContratEmploye = "Tous" },
                            label = { Text("Tous contrats") }
                        )
                        contratsDisponibles.forEach { contrat ->
                            val nombre = employes.count { it.typeContrat.equals(contrat, true) }
                            FilterChip(
                                selected = filtreContratEmploye.equals(contrat, true),
                                onClick = { filtreContratEmploye = if (filtreContratEmploye.equals(contrat, true)) "Tous" else contrat },
                                label = { Text("$contrat ($nombre)") }
                            )
                        }
                    }
                    val filtres = employes.filter {
                        (departementSelectionne == null || it.departement.equals(departementSelectionne, true)) &&
                        (filtreStatutEmploye == "Tous" ||
                            (filtreStatutEmploye == "Actifs" && it.statut.equals("actif", true)) ||
                            (filtreStatutEmploye == "Inactifs" && !it.statut.equals("actif", true))) &&
                        (filtreContratEmploye == "Tous" || it.typeContrat.equals(filtreContratEmploye, true)) &&
                        (it.nom.contains(recherche, true) || it.matricule.contains(recherche, true) || it.poste.contains(recherche, true) || it.departement.contains(recherche, true) || it.telephone.contains(recherche, true) || it.email.contains(recherche, true))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (departementSelectionne == null) "Tous les employés" else departementSelectionne!!, fontWeight = FontWeight.SemiBold)
                        Text("${filtres.size} résultat(s)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    if (filtres.isEmpty()) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.People, null, tint = violetRH, modifier = Modifier.size(32.dp))
                                Text(if (employes.isEmpty()) "Aucun employé enregistré" else "Aucun résultat", fontWeight = FontWeight.Bold)
                                Text(if (employes.isEmpty()) "Ajoute le premier membre du personnel." else "Essaie un autre nom ou département.", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtres, key = { it.id }) { e ->
                                Card(Modifier.fillMaxWidth().clickable { employeSelectionne = e; dossierEmployeOuvert = e; documents = emptyList(); naviguerVers("Dossiers"); chargerDocuments(e) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(46.dp).background(Color(0xFFEDE7FF), RoundedCornerShape(23.dp)), contentAlignment = Alignment.Center) {
                                            Text(e.nom.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "E" }, color = violetRH, fontWeight = FontWeight.Bold)
                                        }
                                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(e.nom.ifBlank { "Employé sans nom" }, fontWeight = FontWeight.Bold)
                                            Text("${e.matricule} • ${e.poste.ifBlank { "Poste non défini" }}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                            Text("${e.departement.ifBlank { "Département non défini" }} • ${e.statut}", color = violetRH, style = MaterialTheme.typography.labelMedium)
                                        }
                                        TextButton(onClick = {
                                            employeEnEdition = e
                                            editNom = e.nomFamille
                                            editPostnom = e.postnom
                                            editPrenom = e.prenom
                                            editPoste = e.poste
                                            editDepartement = e.departement
                                            editTelephone = e.telephone
                                            editEmail = e.email
                                            editTypeContrat = e.typeContrat.ifBlank { "CDI" }
                                            editSexe = e.sexe
                                            editDateNaissance = e.dateNaissance
                                            editAdresse = e.adresse
                                            editDateEmbauche = e.dateEmbauche
                                            editSalaireBase = e.salaireBase
                                            editNotes = e.notes
                                            editStatut = e.statut.ifBlank { "actif" }
                                            afficherModification = true
                                        }) { Text("Modifier") }
                                        Icon(Icons.Filled.ArrowForward, contentDescription = "Ouvrir le dossier", tint = violetRH)
                                    }
                                }
                            }
                        }
                    }
                }
                "Dossiers" -> {
                    val cible = dossierEmployeOuvert
                    if (cible == null) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Sélectionnez un employé pour consulter son dossier.", color = Color.Gray)
                            OutlinedTextField(
                                value = recherche,
                                onValueChange = { recherche = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Rechercher un employé ou un matricule") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                            )
                            val depsDossiers = (departements + employes.map { it.departement })
                                .filter { it.isNotBlank() && it != "null" }.distinct().sorted()
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = departementSelectionne == null, onClick = { departementSelectionne = null }, label = { Text("Tous (${employes.size})") })
                                depsDossiers.forEach { dep ->
                                    FilterChip(
                                        selected = departementSelectionne.equals(dep, true),
                                        onClick = { departementSelectionne = if (departementSelectionne.equals(dep, true)) null else dep },
                                        label = { Text("$dep (${employes.count { it.departement.equals(dep, true) }})") }
                                    )
                                }
                            }
                            val employesFiltresDossiers = employes.filter { e ->
                                (departementSelectionne == null || e.departement.equals(departementSelectionne, true)) &&
                                    (e.nom.contains(recherche, true) || e.matricule.contains(recherche, true) ||
                                     e.poste.contains(recherche, true) || e.departement.contains(recherche, true))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Répertoire du personnel", fontWeight = FontWeight.SemiBold)
                                Text("${employesFiltresDossiers.size} employé(s)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            if (employesFiltresDossiers.isEmpty()) {
                                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text(if (employes.isEmpty()) "Aucun employé enregistré." else "Aucun employé ne correspond à la recherche.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    employesFiltresDossiers.groupBy { it.departement.ifBlank { "Sans département" } }
                                        .toSortedMap().forEach { (dep, equipe) ->
                                            item(key = "dep_$dep") {
                                                Text("$dep · ${equipe.size}", color = violetRH, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                            }
                                            items(equipe, key = { it.id }) { e ->
                                                Card(
                                                    Modifier.fillMaxWidth().clickable {
                                                        employeSelectionne = e
                                                        dossierEmployeOuvert = e
                                                        documents = emptyList()
                                                        chargerDocuments(e)
                                                    },
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(Modifier.size(44.dp).background(Color(0xFFEDE7FF), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                                                            Text(e.nom.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "E" }, color = violetRH, fontWeight = FontWeight.Bold)
                                                        }
                                                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Text(e.nom.ifBlank { "Employé sans nom" }, fontWeight = FontWeight.SemiBold)
                                                            Text("${e.matricule} · ${e.poste.ifBlank { "Poste non défini" }}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Icon(Icons.Filled.ChevronRight, contentDescription = "Ouvrir le dossier", tint = violetRH)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    } else {
                        // Une seule zone défilante pour voir les informations ET les documents sans écran coupé.
                        key(cible.id) {
                            val etatListeDossier = rememberLazyListState()
                            LaunchedEffect(onglet, cible.id) {
                                if (onglet == "Dossiers") etatListeDossier.scrollToItem(0)
                            }
                            LazyColumn(
                                state = etatListeDossier,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Dossier du personnel", fontWeight = FontWeight.Bold)
                                        Text(cible.nom, color = Color.Gray)
                                        Text("${cible.matricule} • ${cible.departement}", color = violetRH, style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = { dossierEmployeOuvert = null; employeSelectionne = null; documents = emptyList(); recherche = ""; departementSelectionne = null }) { Text("Tous les employés") }
                                }
                            }
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Informations personnelles", color = violetRH, fontWeight = FontWeight.Bold)
                                        LigneInfoRH("Nom complet", cible.nom)
                                        LigneInfoRH("Sexe", cible.sexe.ifBlank { "Non renseigné" }.replaceFirstChar { it.uppercase() })
                                        LigneInfoRH("Date de naissance", cible.dateNaissance.ifBlank { "Non renseignée" })
                                        LigneInfoRH("Téléphone", cible.telephone.ifBlank { "Non renseigné" })
                                        LigneInfoRH("E-mail", cible.email.ifBlank { "Non renseigné" })
                                        LigneInfoRH("Adresse", cible.adresse.ifBlank { "Non renseignée" })
                                        Divider(color = fondRH)
                                        Text("Informations professionnelles", color = violetRH, fontWeight = FontWeight.Bold)
                                        LigneInfoRH("Matricule", cible.matricule)
                                        LigneInfoRH("Poste", cible.poste.ifBlank { "Non renseigné" })
                                        LigneInfoRH("Département", cible.departement.ifBlank { "Non affecté" })
                                        LigneInfoRH("Type de contrat", cible.typeContrat.ifBlank { "Non renseigné" })
                                        LigneInfoRH("Date d'embauche", cible.dateEmbauche.ifBlank { "Non renseignée" })
                                        LigneInfoRH("Salaire de base", cible.salaireBase.ifBlank { "Non renseigné" })
                                        LigneInfoRH("Statut", cible.statut.replaceFirstChar { it.uppercase() })
                                        if (cible.notes.isNotBlank()) LigneInfoRH("Notes RH", cible.notes)
                                    }
                                }
                            }
                            if (estRH) {
                                item {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Documents du personnel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${documents.size} fichier(s)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                item {
                                    var menuCategorie by remember { mutableStateOf(false) }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box {
                                            OutlinedButton(onClick = { menuCategorie = true }) { Text(categorieDocument) }
                                            DropdownMenu(expanded = menuCategorie, onDismissRequest = { menuCategorie = false }) {
                                                listOf("Contrat", "CV", "Diplôme", "Identité", "Photo", "Certificat", "Autre").forEach { cat ->
                                                    DropdownMenuItem(text = { Text(cat) }, onClick = { categorieDocument = cat; menuCategorie = false })
                                                }
                                            }
                                        }
                                        Button(onClick = { choisirDocument.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Filled.UploadFile, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Ajouter un document")
                                        }
                                    }
                                    Text("Documents confidentiels : accès réservé au responsable RH autorisé.", color = Color(0xFF8A5A00), style = MaterialTheme.typography.bodySmall)
                                }
                                if (chargementDocuments) item {
                                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                }
                                if (documents.isEmpty() && !chargementDocuments) item {
                                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = violetRH, modifier = Modifier.size(32.dp))
                                            Text("Aucun document dans ce dossier", fontWeight = FontWeight.SemiBold)
                                            Text("Ajoute le CV, le contrat, les diplômes ou les pièces d'identité de cet employé.", color = Color.Gray)
                                        }
                                    }
                                }
                                documents.groupBy { it.categorie.ifBlank { "Autres documents" } }.toSortedMap().forEach { (categorie, docsCategorie) ->
                                    item { Text("${categorie} (${docsCategorie.size})", color = violetRH, fontWeight = FontWeight.Bold) }
                                    items(docsCategorie, key = { it.id }) { d ->
                                        val nomInconnu = d.nom.matches(Regex("\\d{7,}")) && !d.nom.contains('.')
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (d.typeMime.contains("pdf", true)) Icons.Filled.PictureAsPdf else if (d.typeMime.startsWith("image/")) Icons.Filled.Image else Icons.Filled.InsertDriveFile,
                                                        contentDescription = null,
                                                        tint = violetRH,
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Text(if (nomInconnu) "Nom original indisponible" else d.nom, fontWeight = FontWeight.SemiBold)
                                                        Text("${d.categorie} • ${d.date.take(10)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                                        if (nomInconnu) Text("Ancien fichier importé sans son nom d'origine", color = Color(0xFF9A6700), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = {
                                                        documentApercu = d; pagesApercu = emptyList(); erreurApercu = ""; chargementApercu = true
                                                        val accessToken = AuthService.obtenirAccessToken(context)
                                                        if (accessToken.isNullOrBlank()) { chargementApercu = false; erreurApercu = "Session expirée. Reconnecte-toi." }
                                                        else {
                                                            val fileUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive/${Uri.encode(d.chemin, "/")}"
                                                            val previewRequest = authHeaders(Request.Builder().url(fileUrl), accessToken).get().build()
                                                            client.newCall(previewRequest).enqueue(object : Callback {
                                                                override fun onFailure(call: Call, e: IOException) { mainHandler.post { chargementApercu = false; erreurApercu = "Ouverture impossible : ${e.message}" } }
                                                                override fun onResponse(call: Call, response: Response) { response.use { res ->
                                                                    if (!res.isSuccessful) { mainHandler.post { chargementApercu = false; erreurApercu = "Document inaccessible (${res.code})." }; return }
                                                                    try {
                                                                        val bytes = res.body?.bytes() ?: throw IOException("Fichier vide")
                                                                        val mime = d.typeMime.lowercase()
                                                                        val pages = if (mime == "application/pdf" || d.nom.endsWith(".pdf", true)) {
                                                                            val temp = java.io.File(context.cacheDir, "rh_preview_${d.id}.pdf"); temp.writeBytes(bytes)
                                                                            val fd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY); val renderer = PdfRenderer(fd)
                                                                            try { (0 until renderer.pageCount.coerceAtMost(10)).map { pageIndex ->
                                                                                val page = renderer.openPage(pageIndex)
                                                                                try { val width = 900; val height = (width.toFloat() * page.height / page.width).toInt().coerceAtLeast(1); val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); bitmap }
                                                                                finally { page.close() }
                                                                            } } finally { renderer.close(); fd.close(); temp.delete() }
                                                                        } else if (mime.startsWith("image/")) { listOf(BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Image non reconnue")) } else emptyList()
                                                                        mainHandler.post { pagesApercu = pages; chargementApercu = false; if (pages.isEmpty()) erreurApercu = "Aperçu intégré indisponible pour ce format. Télécharge le fichier pour l'ouvrir." }
                                                                    } catch (e: Exception) { mainHandler.post { chargementApercu = false; erreurApercu = "Impossible de lire le document : ${e.message}" } }
                                                                } }
                                                            })
                                                        }
                                                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Visualiser", fontSize = 13.sp, maxLines = 1, softWrap = false)
                                                    }
                                                    Button(
                                                        onClick = { telechargerDocumentRH(d) },
                                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Télécharger", fontSize = 13.sp, maxLines = 1, softWrap = false)
                                                    }
                                                }
                                                if (estRH) {
                                                    TextButton(
                                                        onClick = { documentASupprimer = d },
                                                        enabled = !suppressionDocument,
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color(0xFFB42318))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Supprimer", color = Color(0xFFB42318))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item { Text("Les documents personnels sont confidentiels et réservés au responsable RH.", color = Color.Gray) }
                            }
                            }
                        }
                    }
                }
                "Organisation" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Structure de l'entreprise",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${departements.size} département(s) • ${postes.size} poste(s)",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { afficherPoste = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Work, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Nouveau poste", maxLines = 1)
                            }
                            Button(
                                onClick = { afficherDepartement = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AccountTree, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Département", maxLines = 1)
                            }
                        }
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Text("DÉPARTEMENTS", color = violetRH, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        if (departements.isEmpty()) item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.AccountTree, null, tint = violetRH, modifier = Modifier.size(30.dp))
                                    Text("Aucun département", fontWeight = FontWeight.Bold)
                                    Text("Crée les départements pour structurer les équipes.", color = Color.Gray)
                                }
                            }
                        }
                        items(departements, key = { "dep_$it" }) { dep ->
                            val effectif = employes.count { it.departement.equals(dep, true) }
                            Card(modifier = Modifier.fillMaxWidth().clickable { departementSelectionne = dep; recherche = ""; naviguerVers("Employés") }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountTree, null, tint = violetRH, modifier = Modifier.size(28.dp))
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(dep, fontWeight = FontWeight.Bold)
                                        Text("${effectif} employé(s)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Filled.ArrowForward, null, tint = violetRH)
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("POSTES", color = violetRH, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        if (postes.isEmpty()) item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Work, null, tint = violetRH, modifier = Modifier.size(28.dp))
                                    Text("Aucun poste défini", fontWeight = FontWeight.Bold)
                                    Text("Ajoute les fonctions de l'entreprise : comptable, vendeur, responsable RH, etc.", color = Color.Gray)
                                }
                            }
                        }
                        items(postes, key = { "poste_${it.id}" }) { posteRH ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Work, null, tint = violetRH, modifier = Modifier.size(26.dp))
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(posteRH.nom, fontWeight = FontWeight.SemiBold)
                                        if (posteRH.description.isNotBlank()) Text(posteRH.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("${employes.count { it.poste.equals(posteRH.nom, true) }} salarié(s)", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                "Présences" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Pointage du personnel", fontWeight = FontWeight.Bold); Text(java.time.LocalDate.now().toString(), color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { chargerPresences() }) { Icon(Icons.Filled.Refresh, null); Text("Actualiser") }
                    }
                    if (chargementModule) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val pointagesDuJour = presences.filter { it.date == java.time.LocalDate.now().toString() }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCompactRH("Présents", pointagesDuJour.count { it.statut == "present" }.toString())
                            StatCompactRH("En retard", pointagesDuJour.count { it.statut == "retard" }.toString())
                            StatCompactRH("Absents", pointagesDuJour.count { it.statut == "absent" }.toString())
                        }
                    }
                    Text("Pointer le personnel", fontWeight = FontWeight.SemiBold)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(employes, key = { it.id }) { e ->
                            val pointageExistant = pointagesDuJour.firstOrNull { it.employe == e.nom }
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.People, null, tint = violetRH, modifier = Modifier.size(24.dp))
                                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                            Text(e.nom, fontWeight = FontWeight.SemiBold)
                                            Text(e.poste.ifBlank { "Poste non défini" }, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (pointageExistant != null) {
                                            Text(
                                                pointageExistant.statut.replaceFirstChar { it.uppercase() },
                                                color = when (pointageExistant.statut) {
                                                    "present" -> Color(0xFF16834B)
                                                    "retard" -> Color(0xFFB7791F)
                                                    else -> Color(0xFFB42318)
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text("Non pointé", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { enregistrerPresence(e, "present") },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) { Text("Présent", maxLines = 1) }
                                        OutlinedButton(
                                            onClick = { enregistrerPresence(e, "retard") },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) { Text("Retard", maxLines = 1) }
                                        OutlinedButton(
                                            onClick = { enregistrerPresence(e, "absent") },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) { Text("Absent", maxLines = 1) }
                                    }
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Historique des pointages", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = filtreHistoriquePresence == "Aujourd'hui",
                                        onClick = { filtreHistoriquePresence = "Aujourd'hui" },
                                        label = { Text("Aujourd'hui") }
                                    )
                                    FilterChip(
                                        selected = filtreHistoriquePresence == "Tous",
                                        onClick = { filtreHistoriquePresence = "Tous" },
                                        label = { Text("Tout l'historique") }
                                    )
                                }
                            }
                        }
                        val historiqueFiltre = presences.filter {
                            filtreHistoriquePresence == "Tous" || it.date == java.time.LocalDate.now().toString()
                        }
                        items(historiqueFiltre, key = { it.id }) { p ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EventAvailable, null, tint = violetRH)
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(p.employe, fontWeight = FontWeight.SemiBold); Text("${p.date} • Arrivée ${p.arrivee} • Départ ${p.depart}", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                                    Text(p.statut.replaceFirstChar { it.uppercase() }, color = if (p.statut == "present") Color(0xFF16834B) else Color(0xFFB45309), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (historiqueFiltre.isEmpty()) item {
                            Text("Aucun pointage pour cette période.", color = Color.Gray)
                        }
                    }
                }
                "Congés" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Demandes et validations", fontWeight = FontWeight.Bold)
                        Button(onClick = { afficherDemandeConge = true }) { Icon(Icons.Filled.BeachAccess, null); Spacer(Modifier.width(6.dp)); Text("Nouvelle demande") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCompactRH("En attente", conges.count { it.statut == "en_attente" }.toString())
                        StatCompactRH("Approuvés", conges.count { it.statut == "approuve" }.toString())
                        StatCompactRH("Refusés", conges.count { it.statut == "refuse" }.toString())
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tous", "En attente", "Approuvés", "Refusés").forEach { filtre ->
                            val statutFiltre = when (filtre) {
                                "En attente" -> "en_attente"
                                "Approuvés" -> "approuve"
                                "Refusés" -> "refuse"
                                else -> ""
                            }
                            val nombre = if (statutFiltre.isBlank()) conges.size else conges.count { it.statut == statutFiltre }
                            FilterChip(
                                selected = filtreConges == filtre,
                                onClick = { filtreConges = filtre },
                                label = { Text("$filtre ($nombre)") }
                            )
                        }
                    }
                    if (chargementModule) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val congesFiltres = conges.filter { demande ->
                        when (filtreConges) {
                            "En attente" -> demande.statut == "en_attente"
                            "Approuvés" -> demande.statut == "approuve"
                            "Refusés" -> demande.statut == "refuse"
                            else -> true
                        }
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(congesFiltres, key = { it.id }) { x ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.BeachAccess, null, tint = violetRH); Text("  ${x.employe}", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(x.statut.replace("_", " "), color = if (x.statut == "approuve") Color(0xFF16834B) else Color(0xFF9A6700), style = MaterialTheme.typography.labelMedium) }
                                    Text("${x.type} • ${x.debut} au ${x.fin}", color = Color.Gray)
                                    if (x.motif.isNotBlank()) Text(x.motif, style = MaterialTheme.typography.bodySmall)
                                    if (x.statut == "en_attente") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { deciderConge(x.id, "refuse") }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Cancel, null); Text(" Refuser") }
                                        Button(onClick = { deciderConge(x.id, "approuve") }, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.CheckCircle, null); Text(" Approuver") }
                                    }
                                }
                            }
                        }
                        if (congesFiltres.isEmpty() && !chargementModule) item {
                            Text(
                                if (conges.isEmpty()) "Aucune demande de congé enregistrée."
                                else "Aucune demande dans ce filtre.",
                                color = Color.Gray
                            )
                        }
                    }
                }
                "Paie" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Bulletins de salaire", fontWeight = FontWeight.Bold); Text("Montants en devise de l'entreprise", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = { afficherBulletin = true }) { Icon(Icons.Filled.Payments, null); Spacer(Modifier.width(5.dp)); Text("Créer") }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatCompactRH("Bulletins", bulletins.size.toString())
                            StatCompactRH("En brouillon", bulletins.count { it.statut == "brouillon" }.toString())
                            StatCompactRH("Payés", bulletins.count { it.statut == "paye" }.toString())
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tous", "Brouillons", "Payés").forEach { filtre ->
                            val nombre = when (filtre) {
                                "Brouillons" -> bulletins.count { it.statut == "brouillon" }
                                "Payés" -> bulletins.count { it.statut == "paye" }
                                else -> bulletins.size
                            }
                            FilterChip(
                                selected = filtrePaie == filtre,
                                onClick = { filtrePaie = filtre },
                                label = { Text("$filtre ($nombre)") }
                            )
                        }
                    }
                    if (chargementModule) LinearProgressIndicator(Modifier.fillMaxWidth())
                    val bulletinsFiltres = bulletins.filter {
                        when (filtrePaie) {
                            "Brouillons" -> it.statut == "brouillon"
                            "Payés" -> it.statut == "paye"
                            else -> true
                        }
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bulletinsFiltres, key = { it.id }) { b ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Payments, null, tint = violetRH); Text("  ${b.employe}", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(b.statut, color = if (b.statut == "paye") Color(0xFF16834B) else Color(0xFF9A6700)) }
                                    Text("Période : ${b.debut} au ${b.fin}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    Text("Base : ${"%.2f".format(b.base)}  •  Primes : ${"%.2f".format(b.primes)}  •  Retenues : ${"%.2f".format(b.retenues)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Net à payer : ${"%.2f".format(b.net)}", color = violetRH, fontWeight = FontWeight.Bold)
                                    if (b.statut == "brouillon" && estRH) {
                                        Button(
                                            onClick = { marquerBulletinPaye(b) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Confirmer le paiement")
                                        }
                                    }
                                }
                            }
                        }
                        if (bulletinsFiltres.isEmpty() && !chargementModule) item {
                            Text(
                                if (bulletins.isEmpty()) "Aucun bulletin de paie enregistré." else "Aucun bulletin dans ce filtre.",
                                color = Color.Gray
                            )
                        }
                    }
                }
                "Rapports" -> {
                    Text("Vue d'ensemble des ressources humaines", fontWeight = FontWeight.Bold)
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarteIndicateurRH("Effectif", employes.size.toString(), Icons.Filled.People, Modifier.weight(1f))
                            CarteIndicateurRH("Actifs", employes.count { it.statut == "actif" }.toString(), Icons.Filled.CheckCircle, Modifier.weight(1f))
                        } }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarteIndicateurRH("Pointages chargés", presences.size.toString(), Icons.Filled.EventAvailable, Modifier.weight(1f))
                            CarteIndicateurRH("Congés en attente", conges.count { it.statut == "en_attente" }.toString(), Icons.Filled.Schedule, Modifier.weight(1f))
                        } }
                        item { Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Masse salariale enregistrée", fontWeight = FontWeight.Bold)
                                Text("${"%.2f".format(bulletins.sumOf { it.net })}", color = violetRH, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("Total des bulletins actuellement chargés, sans extrapolation.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        } }
                    }
                }
            }
        }

        if (message.isNotBlank()) Text(message, color = if (message.contains("succès") || message.contains("ajouté")) Color(0xFF16834B) else Color(0xFFB45309), style = MaterialTheme.typography.bodySmall)
    }

    if (documentASupprimer != null) {
        val selectedForDelete = documentASupprimer!!
        AlertDialog(
            onDismissRequest = { if (!suppressionDocument) documentASupprimer = null },
            title = { Text("Supprimer ce document ?") },
            text = {
                Text("« ${selectedForDelete.nom} » sera retiré du dossier de cet employé et du stockage privé. Cette action est définitive.")
            },
            confirmButton = {
                TextButton(
                    onClick = { supprimerDocumentRH(selectedForDelete) },
                    enabled = !suppressionDocument
                ) {
                    if (suppressionDocument) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Supprimer", color = Color(0xFFB42318))
                }
            },
            dismissButton = {
                TextButton(onClick = { documentASupprimer = null }, enabled = !suppressionDocument) {
                    Text("Annuler")
                }
            }
        )
    }

    if (documentApercu != null) {
        val selected = documentApercu!!
        Dialog(onDismissRequest = { documentApercu = null; pagesApercu = emptyList() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f), shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (selected.nom.matches(Regex("\\d{7,}")) && !selected.nom.contains('.')) "Nom original indisponible" else selected.nom, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("${selected.categorie} • ${selected.date.take(10)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { documentApercu = null; pagesApercu = emptyList() }) { Text("Fermer") }
                    }
                    when {
                        chargementApercu -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        pagesApercu.isNotEmpty() -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(pagesApercu) { index, bitmap ->
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Page ${index + 1} / ${pagesApercu.size}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    ApercuPageRH(bitmap = bitmap, numero = index + 1, total = pagesApercu.size)
                                }
                            }
                        }
                        else -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(erreurApercu, color = Color.Gray) }
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val accessToken = AuthService.obtenirAccessToken(context)
                        if (accessToken.isNullOrBlank()) {
                            Toast.makeText(context, "Session expirée. Reconnecte-toi.", Toast.LENGTH_LONG).show()
                        } else {
                            try {
                                val downloadUrl = "${SupabaseConfig.url}/storage/v1/object/rh-prive/${Uri.encode(selected.chemin, "/")}"
                                val extension = when (selected.typeMime.lowercase()) {
                                    "application/pdf" -> "pdf"
                                    "image/jpeg" -> "jpg"
                                    "image/png" -> "png"
                                    "image/webp" -> "webp"
                                    else -> selected.typeMime.substringAfter("/", "bin").substringBefore(";")
                                }
                                val nomFichier = if (selected.nom.matches(Regex("\\d{7,}")) && !selected.nom.contains('.')) {
                                    "${selected.categorie.lowercase()}_${selected.id.take(8)}.${extension}"
                                } else selected.nom.replace(Regex("[/\\\\]"), "_")
                                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                    .setTitle(nomFichier)
                                    .setDescription("Téléchargement du dossier RH")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .addRequestHeader("apikey", SupabaseConfig.key)
                                    .addRequestHeader("Authorization", "Bearer ${accessToken}")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nomFichier)
                                } else {
                                    request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, nomFichier)
                                }
                                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                manager.enqueue(request)
                                Toast.makeText(context, "Téléchargement lancé. Consulte les notifications.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Téléchargement impossible : ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("Télécharger le document") }
                }
            }
        }
    }

    if (afficherDepartement) {
        AlertDialog(
            onDismissRequest = { afficherDepartement = false },
            title = { Text("Ajouter un département") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nouveauDepartement, { nouveauDepartement = it }, label = { Text("Nom du département") }, singleLine = true)
                OutlinedTextField(descriptionDepartement, { descriptionDepartement = it }, label = { Text("Description (facultative)") }, minLines = 2)
            } },
            confirmButton = { Button(onClick = { ajouterDepartement() }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { afficherDepartement = false }) { Text("Annuler") } }
        )
    }

    if (afficherPoste) {
        AlertDialog(
            onDismissRequest = { afficherPoste = false },
            title = { Text("Ajouter un poste") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nouveauPoste,
                        onValueChange = { nouveauPoste = it },
                        label = { Text("Intitulé du poste *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descriptionPoste,
                        onValueChange = { descriptionPoste = it },
                        label = { Text("Description (facultative)") },
                        minLines = 2
                    )
                    Text("Le poste sera enregistré pour cette entreprise uniquement.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { ajouterPoste() }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { afficherPoste = false }) { Text("Annuler") } }
        )
    }

    if (afficherDemandeConge) {
        AlertDialog(
            onDismissRequest = { afficherDemandeConge = false },
            title = { Text("Nouvelle demande de congé") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var menuEmployeConge by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuEmployeConge = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(employes.firstOrNull { it.id == employeConge }?.nom ?: "Choisir un employé")
                        }
                        DropdownMenu(expanded = menuEmployeConge, onDismissRequest = { menuEmployeConge = false }) {
                            employes.forEach { e -> DropdownMenuItem(text = { Text(e.nom) }, onClick = { employeConge = e.id; menuEmployeConge = false }) }
                        }
                    }
                    var menuTypeConge by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuTypeConge = true }, modifier = Modifier.fillMaxWidth()) { Text("Type : $typeConge") }
                        DropdownMenu(expanded = menuTypeConge, onDismissRequest = { menuTypeConge = false }) {
                            listOf("Annuel", "Maladie", "Maternité", "Paternité", "Exceptionnel", "Sans solde").forEach { t ->
                                DropdownMenuItem(text = { Text(t) }, onClick = { typeConge = t; menuTypeConge = false })
                            }
                        }
                    }
                    OutlinedTextField(debutConge, { debutConge = it }, label = { Text("Date de début (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(finConge, { finConge = it }, label = { Text("Date de fin (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(motifConge, { motifConge = it }, label = { Text("Motif") }, minLines = 2)
                }
            },
            confirmButton = { Button(onClick = { enregistrerConge() }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { afficherDemandeConge = false }) { Text("Annuler") } }
        )
    }

    if (afficherBulletin) {
        AlertDialog(
            onDismissRequest = { afficherBulletin = false },
            title = { Text("Créer un bulletin de paie") },
            text = {
                Column(Modifier.heightIn(max = 500.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var menuEmployePaie by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuEmployePaie = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(employes.firstOrNull { it.id == employePaie }?.nom ?: "Choisir un employé")
                        }
                        DropdownMenu(expanded = menuEmployePaie, onDismissRequest = { menuEmployePaie = false }) {
                            employes.forEach { e -> DropdownMenuItem(text = { Text(e.nom) }, onClick = { employePaie = e.id; basePaie = ""; menuEmployePaie = false }) }
                        }
                    }
                    OutlinedTextField(debutPaie, { debutPaie = it }, label = { Text("Début période (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(finPaie, { finPaie = it }, label = { Text("Fin période (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(basePaie, { basePaie = it }, label = { Text("Salaire de base") }, singleLine = true)
                    OutlinedTextField(primesPaie, { primesPaie = it }, label = { Text("Primes") }, singleLine = true)
                    OutlinedTextField(retenuesPaie, { retenuesPaie = it }, label = { Text("Retenues") }, singleLine = true)
                    val net = (basePaie.toDoubleOrNull() ?: 0.0) + (primesPaie.toDoubleOrNull() ?: 0.0) - (retenuesPaie.toDoubleOrNull() ?: 0.0)
                    Text("Net à payer : ${"%.2f".format(net)}", color = violetRH, fontWeight = FontWeight.Bold)
                    Text("Le bulletin sera créé en brouillon. Sa validation et son paiement restent des opérations distinctes.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { creerBulletin() }) { Text("Créer le bulletin") } },
            dismissButton = { TextButton(onClick = { afficherBulletin = false }) { Text("Annuler") } }
        )
    }

    if (afficherModification && employeEnEdition != null) {
        AlertDialog(
            onDismissRequest = { if (!enregistrementModification) { afficherModification = false; employeEnEdition = null } },
            title = { Text("Modifier la fiche employé") },
            text = {
                Column(
                    Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(editNom, { editNom = it }, label = { Text("Nom *") }, singleLine = true)
                    OutlinedTextField(editPostnom, { editPostnom = it }, label = { Text("Postnom") }, singleLine = true)
                    OutlinedTextField(editPrenom, { editPrenom = it }, label = { Text("Prénom") }, singleLine = true)
                    OutlinedTextField(editPoste, { editPoste = it }, label = { Text("Poste *") }, singleLine = true)
                    OutlinedTextField(editDepartement, { editDepartement = it }, label = { Text("Département") }, singleLine = true)
                    OutlinedTextField(editTelephone, { editTelephone = it }, label = { Text("Téléphone") }, singleLine = true)
                    OutlinedTextField(editEmail, { editEmail = it }, label = { Text("E-mail") }, singleLine = true)
                    var menuSexeEdition by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuSexeEdition = true }) { Text("Sexe : " + (editSexe.ifBlank { "Non renseigné" })) }
                        DropdownMenu(expanded = menuSexeEdition, onDismissRequest = { menuSexeEdition = false }) {
                            listOf("homme", "femme", "autre", "").forEach { valeur ->
                                DropdownMenuItem(text = { Text(valeur.ifBlank { "Non renseigné" }) }, onClick = { editSexe = valeur; menuSexeEdition = false })
                            }
                        }
                    }
                    OutlinedTextField(editDateNaissance, { editDateNaissance = it }, label = { Text("Date de naissance (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(editAdresse, { editAdresse = it }, label = { Text("Adresse") })
                    OutlinedTextField(editDateEmbauche, { editDateEmbauche = it }, label = { Text("Date d'embauche (AAAA-MM-JJ)") }, singleLine = true)
                    OutlinedTextField(editSalaireBase, { editSalaireBase = it }, label = { Text("Salaire de base") }, singleLine = true)
                    OutlinedTextField(editNotes, { editNotes = it }, label = { Text("Notes RH") }, minLines = 2)
                    var menuStatutEdition by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuStatutEdition = true }) { Text("Statut : $editStatut") }
                        DropdownMenu(expanded = menuStatutEdition, onDismissRequest = { menuStatutEdition = false }) {
                            listOf("actif", "suspendu", "licencie").forEach { valeur ->
                                DropdownMenuItem(text = { Text(valeur.replaceFirstChar { it.uppercase() }) }, onClick = { editStatut = valeur; menuStatutEdition = false })
                            }
                        }
                    }
                    var menuContratEdition by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { menuContratEdition = true }) { Text("Contrat : $editTypeContrat") }
                        DropdownMenu(expanded = menuContratEdition, onDismissRequest = { menuContratEdition = false }) {
                            listOf("CDI", "CDD", "Journalier", "Stage", "Consultant", "Autre").forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { editTypeContrat = type; menuContratEdition = false })
                            }
                        }
                    }
                    Text("Le matricule et l'historique documentaire ne sont pas modifiés par ce formulaire.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { modifierEmploye() }, enabled = !enregistrementModification) {
                    Text(if (enregistrementModification) "Enregistrement…" else "Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherModification = false; employeEnEdition = null }, enabled = !enregistrementModification) { Text("Annuler") }
            }
        )
    }

    if (afficherAjout) {
        AlertDialog(
            onDismissRequest = { if (!enregistrement) afficherAjout = false },
            title = { Text("Ajouter un employé") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.heightIn(max = 440.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(scrollAjoutEmploye), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(nom, { nom = it }, label = { Text("Nom *") }, singleLine = true)
                        OutlinedTextField(postnom, { postnom = it }, label = { Text("Postnom") }, singleLine = true)
                        OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, singleLine = true)
                        OutlinedTextField(poste, { poste = it }, label = { Text("Poste *") }, singleLine = true)
                        OutlinedTextField(departement, { departement = it }, label = { Text("Département") }, singleLine = true)
                        OutlinedTextField(telephone, { telephone = it }, label = { Text("Téléphone") }, singleLine = true)
                        OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true)
                        var menuSexeCreation by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { menuSexeCreation = true }) { Text("Sexe : " + (sexe.ifBlank { "Non renseigné" })) }
                            DropdownMenu(expanded = menuSexeCreation, onDismissRequest = { menuSexeCreation = false }) {
                                listOf("homme", "femme", "autre", "").forEach { valeur ->
                                    DropdownMenuItem(text = { Text(valeur.ifBlank { "Non renseigné" }) }, onClick = { sexe = valeur; menuSexeCreation = false })
                                }
                            }
                        }
                        OutlinedTextField(dateNaissance, { dateNaissance = it }, label = { Text("Date de naissance (AAAA-MM-JJ)") }, singleLine = true)
                        OutlinedTextField(adresse, { adresse = it }, label = { Text("Adresse") })
                        OutlinedTextField(dateEmbauche, { dateEmbauche = it }, label = { Text("Date d'embauche (AAAA-MM-JJ, facultatif)") }, singleLine = true)
                        OutlinedTextField(salaireBase, { salaireBase = it }, label = { Text("Salaire de base") }, singleLine = true)
                        OutlinedTextField(notesRH, { notesRH = it }, label = { Text("Notes RH") }, minLines = 2)
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
private fun LigneInfoRH(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(libelle, color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.9f))
        Text(valeur, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.1f))
    }
}

@Composable
private fun ApercuPageRH(bitmap: Bitmap, numero: Int, total: Int) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 4f)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Page $numero / $total • Pincer pour agrandir", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Page $numero du document",
            modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = zoom, scaleY = zoom).transformable(transformState),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StatCompactRH(titre: String, valeur: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valeur, color = violetRH, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(titre, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CarteMenuRH(titre: String, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icone = when (titre) {
        "Employés" -> Icons.Filled.People
        "Dossiers" -> Icons.Filled.Folder
        "Organisation" -> Icons.Filled.AccountTree
        "Présences" -> Icons.Filled.EventAvailable
        "Congés" -> Icons.Filled.BeachAccess
        "Paie" -> Icons.Filled.Payments
        else -> Icons.Filled.Assessment
    }
    Card(modifier = modifier.heightIn(min = 146.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(42.dp).background(Color(0xFFEDE7FF), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(icone, contentDescription = titre, tint = violetRH, modifier = Modifier.size(23.dp))
            }
            Text(titre, color = Color(0xFF252238), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = Color.Gray, style = MaterialTheme.typography.bodySmall, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Ouvrir", color = violetRH, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.ArrowForward, null, tint = violetRH, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun CarteIndicateurRH(titre: String, valeur: String, icone: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, null, tint = violetRH, modifier = Modifier.size(24.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(valeur, color = violetRH, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(titre, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
