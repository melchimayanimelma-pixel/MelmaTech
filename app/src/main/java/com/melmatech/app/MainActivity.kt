package com.melmatech.app

import android.content.Context
import android.os.Bundle
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.os.ParcelFileDescriptor
import android.net.Uri
import android.util.Base64

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale

import coil.compose.rememberAsyncImagePainter
import org.json.JSONArray
import org.json.JSONObject

import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.os.CancellationSignal
import java.io.FileInputStream
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.res.painterResource


data class Produit(
    val id: Long,
    val nom: String,
    val achat: Double,
    val vente: Double,
    val quantite: Int,
    val supabaseId: String? = null
)

data class LigneVente(
    val produitId: Long,
    val nom: String,
    val quantite: Int,
    val prixUnitaire: Double,
    val prixAchat: Double
) {
    val total: Double get() = prixUnitaire * quantite
}

data class Vente(
    val id: Long,
    val client: String,
    val lignes: List<LigneVente>,
    val paye: Double,
    val date: Long
) {
    val total: Double get() = lignes.sumOf { it.total }
    val reste: Double get() = (total - paye).coerceAtLeast(0.0)
    val cout: Double get() = lignes.sumOf {
        it.prixAchat * it.quantite
    }
}

data class Depense(
    val id: Long,
    val description: String,
    val montant: Double,
    val date: Long
)

fun argent(montant: Double, devise: String = "USD"): String {
    val montantFormate = String.format(
        Locale.US,
        "%,.2f",
        montant
    )

    return if (devise == "CDF") {
        "$montantFormate FC"
    } else {
        "$$montantFormate"
    }
}
fun formaterDate(timestamp: Long): String {
    val format = SimpleDateFormat(
        "dd/MM/yyyy à HH:mm",
        Locale.FRANCE
    )

    return format.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelmaTechApp(context: Context) {

    val prefs = remember {
        context.getSharedPreferences("melmatech", Context.MODE_PRIVATE)
    }

    var utilisateurConnecte by remember {
        mutableStateOf(AuthService.estConnecte(context))
    }

    var roleUtilisateur by remember {
        mutableStateOf<String?>(null)
    }

    var page by remember { mutableStateOf("Accueil") }
    var devise by remember {
    mutableStateOf(prefs.getString("devise", "USD") ?: "USD")
    }
    
    // Informations de l'entreprise
    var nomEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_nom", "") ?: "")
    }
    
var lieuEntreprise by remember {
    mutableStateOf(prefs.getString("entreprise_lieu", "") ?: "")
}

var nomGerant by remember {
    mutableStateOf(prefs.getString("entreprise_gerant", "") ?: "")
}

var fonctionGerant by remember {
    mutableStateOf(
        prefs.getString("entreprise_fonction_gerant", "Gérant") ?: "Gérant"
    )
}


    var adresseEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_adresse", "") ?: "")
    }

    var emailEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_email", "") ?: "")
    }

    var telephoneEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_telephone", "") ?: "")
    }

    var rccmEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_rccm", "") ?: "")
    }

    var idNatEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_idnat", "") ?: "")
    }

    var impotEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_impot", "") ?: "")
    }

    var logoEntreprise by remember {
        mutableStateOf(prefs.getString("entreprise_logo", "") ?: "")
    }
    var tailleLogoEntreprise by remember {
    mutableStateOf(prefs.getFloat("entreprise_logo_taille", 1f))
    }

    var messageEntreprise by remember {
        mutableStateOf("")
    }

    val choisirLogo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                logoEntreprise = uri.toString()

                prefs.edit()
                    .putString("entreprise_logo", logoEntreprise)
                    .apply()

                messageEntreprise = "Logo sélectionné avec succès."
            } catch (e: Exception) {
                messageEntreprise = "Impossible d'enregistrer ce logo."
            }
        }
    }


    var photoProfilUri by remember {
    mutableStateOf<Uri?>(null)
}

val choisirPhotoProfil = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    if (uri != null) {
        photoProfilUri = uri
    }
}
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var modeCreation by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(false) }
    var messageConnexion by remember { mutableStateOf("") }

    var messageProduit by remember { mutableStateOf("") }
    var messageVente by remember { mutableStateOf("") }
    var fichierFactureEnAttente by remember {
    mutableStateOf<File?>(null)
}
    var fichierRapportEnAttente by remember {
    mutableStateOf<File?>(null)
}

val enregistrerRapportLauncher =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val fichier = fichierRapportEnAttente

        if (uri != null && fichier != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { sortie ->
                    fichier.inputStream().use { entree ->
                        entree.copyTo(sortie)
                    }
                }
            } catch (e: Exception) {
                messageVente = "Erreur d'enregistrement du rapport : ${e.message}"
            }
        }

        fichierRapportEnAttente = null
    }

val enregistrerFactureLauncher =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val fichier = fichierFactureEnAttente

        if (uri != null && fichier != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { sortie ->
                    fichier.inputStream().use { entree ->
                        entree.copyTo(sortie)
                    }
                }
                messageVente = "Facture enregistrée avec succès."
            } catch (e: Exception) {
                messageVente = "Erreur d'enregistrement : ${e.message}"
            }
        }

        fichierFactureEnAttente = null
    }

    var messageDepense by remember { mutableStateOf("") }

    var chargementProduits by remember {
        mutableStateOf(false)
    }

    val estAdministrateur =
        roleUtilisateur == "administrateur" ||
        roleUtilisateur == "admin" ||
        roleUtilisateur == "manager"

    val produits = remember {
        mutableStateListOf<Produit>().apply {
            try {
                val json = JSONArray(
                    prefs.getString("produits", "[]")
                )

                for (i in 0 until json.length()) {
                    val p = json.getJSONObject(i)

                    add(
                        Produit(
                            id = p.getLong("id"),
                            nom = p.getString("nom"),
                            achat = p.getDouble("achat"),
                            vente = p.getDouble("vente"),
                            quantite = p.getInt("quantite"),
                            supabaseId = if (
                                p.isNull("supabaseId")
                            ) null else p.optString("supabaseId")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    val ventes = remember {
        mutableStateListOf<Vente>().apply {
            try {
                val json = JSONArray(
                    prefs.getString("ventes_nouvelles", "[]")
                )

                for (i in 0 until json.length()) {
                    val v = json.getJSONObject(i)
                    val lignesJson = v.getJSONArray("lignes")
                    val lignes = mutableListOf<LigneVente>()

                    for (j in 0 until lignesJson.length()) {
                        val l = lignesJson.getJSONObject(j)

                        lignes.add(
                            LigneVente(
                                l.getLong("produitId"),
                                l.getString("nom"),
                                l.getInt("quantite"),
                                l.getDouble("prixUnitaire"),
                                l.getDouble("prixAchat")
                            )
                        )
                    }

                    add(
                        Vente(
                            v.getLong("id"),
                            v.getString("client"),
                            lignes,
                            v.getDouble("paye"),
                            v.getLong("date")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    val depenses = remember {
        mutableStateListOf<Depense>().apply {
            try {
                val json = JSONArray(
                    prefs.getString("depenses", "[]")
                )

                for (i in 0 until json.length()) {
                    val d = json.getJSONObject(i)

                    add(
                        Depense(
                            d.getLong("id"),
                            d.getString("description"),
                            d.getDouble("montant"),
                            d.getLong("date")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun sauvegarderProduits() {
        val json = JSONArray()

        produits.forEach { p ->
            json.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("nom", p.nom)
                    put("achat", p.achat)
                    put("vente", p.vente)
                    put("quantite", p.quantite)
                    put("supabaseId", p.supabaseId)
                }
            )
        }

        prefs.edit()
            .putString("produits", json.toString())
            .apply()
    }

    fun sauvegarderVentes() {
        val json = JSONArray()

        ventes.forEach { v ->
            val lignes = JSONArray()

            v.lignes.forEach { l ->
                lignes.put(
                    JSONObject().apply {
                        put("produitId", l.produitId)
                        put("nom", l.nom)
                        put("quantite", l.quantite)
                        put("prixUnitaire", l.prixUnitaire)
                        put("prixAchat", l.prixAchat)
                    }
                )
            }

            json.put(
                JSONObject().apply {
                    put("id", v.id)
                    put("client", v.client)
                    put("lignes", lignes)
                    put("paye", v.paye)
                    put("date", v.date)
                }
            )
        }

        prefs.edit()
            .putString("ventes_nouvelles", json.toString())
            .apply()
    }

    fun sauvegarderDepenses() {
        val json = JSONArray()

        depenses.forEach { d ->
            json.put(
                JSONObject().apply {
                    put("id", d.id)
                    put("description", d.description)
                    put("montant", d.montant)
                    put("date", d.date)
                }
            )
        }

        prefs.edit()
            .putString("depenses", json.toString())
            .apply()
    }

    // Connexion et chargement des données du compte.
    LaunchedEffect(utilisateurConnecte) {
        if (utilisateurConnecte) {

            AuthService.obtenirRole(context) {
                    succes, message, role ->

                roleUtilisateur = if (succes) {
                    role?.lowercase(Locale.ROOT)
                } else {
                    null
                }

                if (!succes) {
                    messageConnexion = message
                }
            }

            val token = AuthService.obtenirAccessToken(context)

            if (!token.isNullOrBlank()) {
                chargementProduits = true

                AuthService.obtenirEntrepriseId(context) {
                        succes, message, entrepriseId ->

                    if (succes && entrepriseId != null) {
                        SupabaseService.chargerProduits(
                            token,
                            entrepriseId
                        ) { ok, resultat, liste ->

                            chargementProduits = false

                            if (ok && liste != null) {
                                produits.clear()

                                for (i in 0 until liste.length()) {
                                    val p = liste.getJSONObject(i)
                                    val uuid = p.getString("id")

                                    produits.add(
                                        Produit(
                                            id = uuid.hashCode().toLong(),
                                            nom = p.getString("nom"),
                                            achat = p.getDouble("prix_achat"),
                                            vente = p.getDouble("prix_vente"),
                                            quantite = p.getInt("quantite_stock"),
                                            supabaseId = uuid
                                        )
                                    )
                                }

                                sauvegarderProduits()
                                messageProduit =
                                    "Produits chargés depuis Supabase."
                            } else {
                                messageProduit = resultat
                            }
                        }
                    } else {
                        chargementProduits = false
                        messageProduit = message
                    }
                }
            }
        } else {
            roleUtilisateur = null
        }
    }

    // Champs de gestion des produits.
    var nomProduit by remember { mutableStateOf("") }
    var achatProduit by remember { mutableStateOf("") }
    var venteProduit by remember { mutableStateOf("") }
    var stockProduit by remember { mutableStateOf("") }
    var editionId by remember { mutableStateOf<Long?>(null) }
    var rechercheProduit by remember { mutableStateOf("") }
    var filtreProduit by remember { mutableStateOf("Tous") }

    // Champs de vente.
    var client by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }
    var produitChoisi by remember { mutableStateOf<Produit?>(null) }
    var quantiteSaisie by remember { mutableStateOf("1") }
    var panier by remember { mutableStateOf(listOf<LigneVente>()) }
    var derniereVenteValidee by remember {
    mutableStateOf<Vente?>(null)
    }
    var paiement by remember { mutableStateOf("") }
    var paiementId by remember { mutableStateOf<Long?>(null) }
    var paiementSupplementaire by remember { mutableStateOf("") }

    // Champs de dépenses.
    var descriptionDepense by remember { mutableStateOf("") }
    var montantDepense by remember { mutableStateOf("") }

    val chiffreAffaires = ventes.sumOf { it.total }
    val totalEncaisse = ventes.sumOf { it.paye }
    val totalDettes = ventes.sumOf { it.reste }
    val coutMarchandises = ventes.sumOf { it.cout }
    val totalDepenses = depenses.sumOf { it.montant }
    val benefice = chiffreAffaires - coutMarchandises - totalDepenses
    BackHandler(
    enabled = utilisateurConnecte && page != "Accueil"
) {
    page = when (page) {
        "Entreprise",
        "Paramètres",
        "Utilisateurs" -> "Plus"

        else -> "Accueil"
    }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF1565C0),
            secondary = androidx.compose.ui.graphics.Color(0xFF1976D2),
            background = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
            surface = androidx.compose.ui.graphics.Color.White
        )
    ) {

        if (!utilisateurConnecte) {

            EcranConnexion(
                email = email,
                onEmailChange = { email = it },
                motDePasse = motDePasse,
                onMotDePasseChange = { motDePasse = it },
                modeCreationCompte = modeCreation,
                chargement = chargement,
                message = messageConnexion,
                onChangerMode = {
                    modeCreation = !modeCreation
                    messageConnexion = ""
                },
                onValider = {
                    if (email.isBlank() || motDePasse.isBlank()) {
                        messageConnexion =
                            "Remplis l'e-mail et le mot de passe."
                    } else {
                        chargement = true

                        val callback: (Boolean, String) -> Unit =
                            { succes, message ->
                                chargement = false
                                messageConnexion = message

                                utilisateurConnecte =
                                    succes && AuthService.estConnecte(context)
                            }

                        if (modeCreation) {
                            AuthService.creerCompte(
                                context,
                                email.trim(),
                                motDePasse,
                                callback
                            )
                        } else {
                            AuthService.connexion(
                                context,
                                email.trim(),
                                motDePasse,
                                callback
                            )
                        }
                    }
                }
            )

        } else {

            Scaffold(
    topBar = {
        TopAppBar(
    title = {
    Image(
        painter = painterResource(id = R.drawable.melmatech_logo),
        contentDescription = "MelmaTech",
        modifier = Modifier
            .width(180.dp)
            .height(55.dp),
        contentScale = ContentScale.Fit
    )
},
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.White
    )
)
    },
                bottomBar = {
                    NavigationBar(
    containerColor = Color.White,
    contentColor = Color(0xFF6C3CE9)
) {
                        val onglets = if (estAdministrateur) {
                            listOf(
                                "Accueil",
                                "Produits",
                                "Ventes",
                                "Rapports",
                                "Plus"
                            )
                        } else {
                            listOf(
                                "Accueil",
                                "Ventes",
                                "Rapports",
                                "Plus"
                            )
                        }

                        onglets.forEach { onglet ->
    val icone = when (onglet) {
        "Accueil" -> Icons.Filled.Home
        "Produits" -> Icons.Filled.Inventory2
        "Ventes" -> Icons.Filled.ShoppingCart
        "Rapports" -> Icons.Filled.BarChart
        else -> Icons.Filled.MoreHoriz
    }

    NavigationBarItem(
        selected = page == onglet,
        colors = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF6C3CE9),
    selectedTextColor = Color(0xFF6C3CE9),
    indicatorColor = Color(0xFFEDE5FF),
    unselectedIconColor = Color(0xFF8A8A8A),
    unselectedTextColor = Color(0xFF8A8A8A)
),
        onClick = { page = onglet },
        icon = {
            Icon(
                imageVector = icone,
                contentDescription = onglet
            )
        },
        label = {
            Text(
                text = onglet,
                maxLines = 1
            )
        }
    )
                        }
                    }
                }
            ) { padding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp)
                ) {

                    // Empêche l'accès aux pages réservées.
                    if (
                        !estAdministrateur &&
                        (page == "Produits" || page == "Utilisateurs")
                    ) {
                        page = "Accueil"
                    }

                    when (page) {

                        "Accueil" -> {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF6C3CE9),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = "Bienvenue sur MelmaTech",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Gestion de votre commerce en $devise",
                    color = Color.White
                )
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                item {
                    DashboardCard(
                        "Produits enregistrés",
                        produits.size.toString()
                    )
                }

                item {
                    DashboardCard(
                        "Chiffre d'affaires",
                        argent(chiffreAffaires, devise)
                    )
                }

                item {
                    DashboardCard(
                        "Argent encaissé",
                        argent(totalEncaisse, devise)
                    )
                }

                item {
                    DashboardCard(
                        "Dettes clients",
                        argent(totalDettes, devise)
                    )
                }

                item {
                    DashboardCard(
                        "Dépenses",
                        argent(totalDepenses, devise)
                    )
                }

                item {
                    DashboardCard(
                        "Bénéfice estimé",
                        argent(benefice, devise)
                    )
                }
            }
        }

        item {
            if (chargementProduits) {
                CircularProgressIndicator()
            }

            Text(messageProduit)
        }

        item {
            Button(
                onClick = {
                    SupabaseService.testerConnexion {
                        succes, message ->
                        messageConnexion = message
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tester Supabase")
            }
        }
    }
                        }

                        
"Produits" -> {

    val totalProduits = produits.size

    val stockTotal = produits.sumOf { it.quantite }

    val produitsEnAlerte = produits.count {
        it.quantite in 1..5
    }

    val produitsEpuises = produits.count {
        it.quantite <= 0
    }

    val valeurStock = produits.sumOf {
        it.achat * it.quantite
    }

    val produitsFiltres = produits.filter { produit ->
        val rechercheOK = produit.nom.contains(
            rechercheProduit,
            ignoreCase = true
        )

        val filtreOK = when (filtreProduit) {
            "Stock faible" -> produit.quantite in 1..5
            "Épuisés" -> produit.quantite <= 0
            else -> true
        }

        rechercheOK && filtreOK
    }

    if (!estAdministrateur) {

        Text("Accès réservé à l'administrateur.")

    } else {

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF5426A8),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "MelmaTech",
                        color = Color.White
                    )

                    Text(
                        "Gestion des produits",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "Gérez votre catalogue et votre stock",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                Text(
                    "Vue d'ensemble",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Produits")
                            Text(
                                "$totalProduits",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF5426A8)
                            )
                        }
                    }

                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Unités en stock")
                            Text(
                                "$stockTotal",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Stock faible")
                            Text(
                                "$produitsEnAlerte",
                                color = Color(0xFFD97706),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    Card(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Épuisés")
                            Text(
                                "$produitsEpuises",
                                color = Color.Red,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1EBFC)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Valeur du stock")
                        Text(
                            argent(valeurStock, devise),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF5426A8)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = rechercheProduit,
                    onValueChange = { rechercheProduit = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Rechercher un produit") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Tous", "Stock faible", "Épuisés").forEach { filtre ->
                        FilterChip(
                            selected = filtreProduit == filtre,
                            onClick = { filtreProduit = filtre },
                            label = { Text(filtre) }
                        )
                    }
                }
            }

            item {
                Text(
                    if (editionId == null) "Ajouter un produit"
                    else "Modifier un produit",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = nomProduit,
                    onValueChange = { nomProduit = it },
                    label = { Text("Nom du produit") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = achatProduit,
                    onValueChange = { achatProduit = it },
                    label = { Text("Prix d'achat ($devise)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venteProduit,
                    onValueChange = { venteProduit = it },
                    label = { Text("Prix de vente ($devise)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockProduit,
                    onValueChange = { stockProduit = it },
                    label = { Text("Quantité en stock") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val a = achatProduit.replace(",", ".").toDoubleOrNull()
                        val v = venteProduit.replace(",", ".").toDoubleOrNull()
                        val q = stockProduit.toIntOrNull()

                        if (
                            nomProduit.isBlank() ||
                            a == null || a < 0 ||
                            v == null || v < 0 ||
                            q == null || q < 0
                        ) {
                            messageProduit =
                                "Vérifie les informations du produit."
                        } else {
                            val id = editionId

                            if (id == null) {
                                val nouvelId = System.currentTimeMillis()

                                produits.add(
                                    Produit(
                                        nouvelId,
                                        nomProduit.trim(),
                                        a,
                                        v,
                                        q
                                    )
                                )

                                sauvegarderProduits()
                                messageProduit =
                                    "Produit enregistré localement."

                                val token =
                                    AuthService.obtenirAccessToken(context)

                                if (!token.isNullOrBlank()) {
                                    AuthService.obtenirEntrepriseId(context) {
                                            ok, message, entrepriseId ->

                                        if (ok && entrepriseId != null) {
                                            SupabaseService.ajouterProduit(
                                                token,
                                                entrepriseId,
                                                nomProduit.trim(),
                                                a,
                                                v,
                                                q
                                            ) { enregistre, resultat, uuid ->

                                                if (enregistre && uuid != null) {
                                                    val index =
                                                        produits.indexOfFirst {
                                                            it.id == nouvelId
                                                        }

                                                    if (index >= 0) {
                                                        produits[index] =
                                                            produits[index].copy(
                                                                supabaseId = uuid
                                                            )
                                                        sauvegarderProduits()
                                                    }
                                                }

                                                messageProduit = resultat
                                            }
                                        } else {
                                            messageProduit = message
                                        }
                                    }
                                }

                            } else {
                                val index = produits.indexOfFirst {
                                    it.id == id
                                }

                                if (index >= 0) {
                                    produits[index] =
                                        produits[index].copy(
                                            nom = nomProduit.trim(),
                                            achat = a,
                                            vente = v,
                                            quantite = q
                                        )
                                }

                                sauvegarderProduits()
                                messageProduit =
                                    "Modification enregistrée localement. Synchronisation des modifications non activée."
                            }

                            nomProduit = ""
                            achatProduit = ""
                            venteProduit = ""
                            stockProduit = ""
                            editionId = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (editionId == null) "Ajouter le produit"
                        else "Enregistrer les modifications"
                    )
                }

                Text(messageProduit)
            }

            item {
                Text(
                    "Catalogue des produits (${produitsFiltres.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                produitsFiltres,
                key = { it.id }
            ) { p ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            p.nom,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Achat : ${argent(p.achat, devise)}")
                            Text("Vente : ${argent(p.vente, devise)}")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quantité : ${p.quantite}")

                            Text(
                                when {
                                    p.quantite <= 0 -> "Épuisé"
                                    p.quantite <= 5 -> "Stock faible"
                                    else -> "Disponible"
                                },
                                color = when {
                                    p.quantite <= 0 -> Color.Red
                                    p.quantite <= 5 -> Color(0xFFD97706)
                                    else -> Color(0xFF16834A)
                                }
                            )
                        }

                        Row {
                            TextButton(
                                onClick = {
                                    editionId = p.id
                                    nomProduit = p.nom
                                    achatProduit = p.achat.toString()
                                    venteProduit = p.vente.toString()
                                    stockProduit = p.quantite.toString()
                                }
                            ) {
                                Text("Modifier")
                            }

                            TextButton(
                                onClick = {
                                    produits.removeAll { it.id == p.id }
                                    sauvegarderProduits()
                                }
                            ) {
                                Text("Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }
}


                        "Depenses" -> {

    var messageDepense by remember {
        mutableStateOf("")
    }

    var depenseAModifier by remember {
        mutableStateOf<Depense?>(null)
    }

    var confirmerSuppression by remember {
        mutableStateOf<Depense?>(null)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Gestion des dépenses",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text("Enregistre et suis les dépenses de ton commerce.")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (depenseAModifier != null)
                            "Modifier une dépense"
                        else
                            "Nouvelle dépense",
                        style = MaterialTheme.typography.titleLarge
                    )

                    OutlinedTextField(
                        value = descriptionDepense,
                        onValueChange = {
                            descriptionDepense = it
                        },
                        label = {
                            Text("Description de la dépense")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = montantDepense,
                        onValueChange = {
                            montantDepense = it
                        },
                        label = {
                            Text("Montant ($devise)")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val montant = montantDepense
                                .replace(",", ".")
                                .toDoubleOrNull()

                            if (
                                descriptionDepense.isBlank() ||
                                montant == null ||
                                montant <= 0
                            ) {
                                messageDepense =
                                    "Vérifie la description et le montant."
                            } else {

                                if (depenseAModifier != null) {
                                    val ancienneDepense =
                                        depenseAModifier!!

                                    val index = depenses.indexOfFirst {
                                        it.id == ancienneDepense.id
                                    }

                                    if (index != -1) {
                                        depenses[index] =
                                            ancienneDepense.copy(
                                                description = descriptionDepense.trim(),
                                                montant = montant
                                            )
                                    }

                                    depenseAModifier = null

                                    messageDepense =
                                        "Dépense modifiée avec succès."

                                } else {
                                    val id = System.currentTimeMillis()

                                    depenses.add(
                                        Depense(
                                            id,
                                            descriptionDepense.trim(),
                                            montant,
                                            id
                                        )
                                    )

                                    messageDepense =
                                        "Dépense enregistrée avec succès."
                                }

                                sauvegarderDepenses()

                                descriptionDepense = ""
                                montantDepense = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (depenseAModifier != null)
                                "Enregistrer les modifications"
                            else
                                "Enregistrer la dépense"
                        )
                    }

                    if (depenseAModifier != null) {
                        OutlinedButton(
                            onClick = {
                                depenseAModifier = null
                                descriptionDepense = ""
                                montantDepense = ""
                                messageDepense = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Annuler la modification")
                        }
                    }

                    if (messageDepense.isNotBlank()) {
                        Text(messageDepense)
                    }
                }
            }
        }

        item {
            Text(
                "Historique des dépenses",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Total : ${argent(totalDepenses, devise)}",
                fontWeight = FontWeight.Bold
            )
        }

        if (depenses.isEmpty()) {
            item {
                Text("Aucune dépense enregistrée.")
            }
        }

        items(
            depenses.sortedByDescending { it.date },
            key = { it.id }
        ) { depense ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        depense.description,
                        fontWeight = FontWeight.Bold
                    )

                    Text(argent(depense.montant, devise))

                    Text(formaterDate(depense.date))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                depenseAModifier = depense
                                descriptionDepense = depense.description
                                montantDepense = depense.montant.toString()
                                messageDepense = ""
                            }
                        ) {
                            Text("Modifier")
                        }

                        TextButton(
                            onClick = {
                                confirmerSuppression = depense
                            }
                        ) {
                            Text(
                                "Supprimer",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmerSuppression != null) {
        AlertDialog(
            onDismissRequest = {
                confirmerSuppression = null
            },
            title = {
                Text("Supprimer cette dépense ?")
            },
            text = {
                Text(
                    "La dépense « ${confirmerSuppression!!.description} » sera supprimée définitivement."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val depense = confirmerSuppression!!

                        depenses.removeAll {
                            it.id == depense.id
                        }

                        sauvegarderDepenses()

                        if (depenseAModifier?.id == depense.id) {
                            depenseAModifier = null
                            descriptionDepense = ""
                            montantDepense = ""
                        }

                        confirmerSuppression = null
                        messageDepense = "Dépense supprimée."
                    }
                ) {
                    Text(
                        "Supprimer",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmerSuppression = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
                        }
                        
                        "Ventes" -> {

    var ongletVente by remember {
        mutableStateOf("Nouvelle vente")
    }

    val chiffreAffairesVentes = ventes.sumOf { it.total }
    val nombreVentes = ventes.size
    val articlesVendus = ventes.sumOf { vente ->
        vente.lignes.sumOf { it.quantite }
    }

    val suggestions = produits.filter { produit ->
        produitChoisi == null &&
        recherche.isNotBlank() &&
        produit.nom.contains(
            recherche.trim(),
            ignoreCase = true
        ) &&
        produit.quantite > 0
    }.take(8)

    val totalPanier = panier.sumOf { it.total }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {

        // EN-TÊTE
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF6C3CE9),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "MelmaTech",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "Gestion des ventes",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Enregistrez vos ventes et suivez vos paiements.",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // STATISTIQUES
        item {
            Text(
                "Vue d'ensemble",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            "Chiffre d'affaires",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            argent(chiffreAffairesVentes, devise),
                            color = Color(0xFF6C3CE9),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            "Ventes",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            nombreVentes.toString(),
                            color = Color(0xFF6C3CE9),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            "Articles vendus",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            articlesVendus.toString(),
                            color = Color(0xFF6C3CE9),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // NAVIGATION ENTRE LES DEUX ONGLETS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFF0EBFA),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                listOf(
                    "Nouvelle vente",
                    "Historique"
                ).forEach { onglet ->

                    val selectionne = ongletVente == onglet

                    Button(
                        onClick = {
                            ongletVente = onglet
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectionne)
                                Color(0xFF6C3CE9)
                            else
                                Color.Transparent,
                            contentColor = if (selectionne)
                                Color.White
                            else
                                Color(0xFF5426A8)
                        )
                    ) {
                        Text(
                            onglet,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ==========================================
        // ONGLET NOUVELLE VENTE
        // ==========================================

        if (ongletVente == "Nouvelle vente") {

            item {
                Text(
                    "Informations du client",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = client,
                    onValueChange = {
                        client = it
                    },
                    label = {
                        Text("Nom du client")
                    },
                    placeholder = {
                        Text("Entrez le nom du client")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    "Ajouter des produits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = recherche,
                    onValueChange = {
                        recherche = it
                        produitChoisi = null
                    },
                    label = {
                        Text("Rechercher un produit")
                    },
                    placeholder = {
                        Text("Tapez les premières lettres...")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Rechercher"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // SUGGESTIONS DE PRODUITS
            if (suggestions.isNotEmpty()) {

                items(
                    suggestions,
                    key = { it.id }
                ) { produit ->

                    Card(
                        onClick = {
                            produitChoisi = produit
                            recherche = produit.nom
                            quantiteSaisie = "1"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    produit.nom,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Prix : ${argent(produit.vente, devise)}",
                                    color = Color(0xFF6C3CE9)
                                )

                                Text(
                                    "Stock disponible : ${produit.quantite}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Sélectionner",
                                tint = Color(0xFF6C3CE9)
                            )
                        }
                    }
                }
            }

            // PRODUIT SÉLECTIONNÉ
            item {

                produitChoisi?.let { produit ->

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1EBFC)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                "Produit sélectionné",
                                color = Color(0xFF6C3CE9),
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                produit.nom,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Prix unitaire : ${argent(produit.vente, devise)}"
                            )

                            Text(
                                "Stock disponible : ${produit.quantite}"
                            )

                            OutlinedTextField(
                                value = quantiteSaisie,
                                onValueChange = {
                                    quantiteSaisie = it
                                },
                                label = {
                                    Text("Quantité")
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {

                                    val quantite =
                                        quantiteSaisie.toIntOrNull()

                                    val dejaAjoute = panier
                                        .filter {
                                            it.produitId == produit.id
                                        }
                                        .sumOf { it.quantite }

                                    if (
                                        quantite != null &&
                                        quantite > 0 &&
                                        quantite + dejaAjoute <= produit.quantite
                                    ) {

                                        val index = panier.indexOfFirst {
                                            it.produitId == produit.id
                                        }

                                        panier = if (index >= 0) {

                                            panier.mapIndexed { i, ligne ->

                                                if (i == index) {
                                                    ligne.copy(
                                                        quantite =
                                                            ligne.quantite + quantite
                                                    )
                                                } else {
                                                    ligne
                                                }
                                            }

                                        } else {

                                            panier + LigneVente(
                                                produit.id,
                                                produit.nom,
                                                quantite,
                                                produit.vente,
                                                produit.achat
                                            )
                                        }

                                        produitChoisi = null
                                        recherche = ""
                                        quantiteSaisie = "1"
                                        messageVente = ""

                                    } else {

                                        messageVente =
                                            "Quantité invalide ou stock insuffisant."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(6.dp))

                                Text("Ajouter au panier")
                            }
                        }
                    }
                }
            }

            // PANIER
            item {
                Text(
                    "Mon panier (${panier.size} lignes)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (panier.isEmpty()) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🛒")

                            Text(
                                "Votre panier est vide.",
                                color = Color.Gray
                            )
                        }
                    }
                }

            } else {

                items(
                    panier,
                    key = { it.produitId }
                ) { ligne ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    ligne.nom,
                                    fontWeight = FontWeight.Bold
                                )

                                Text("Quantité : ${ligne.quantite}")

                                Text(
                                    argent(ligne.total, devise),
                                    color = Color(0xFF6C3CE9),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    panier = panier.filter {
                                        it.produitId != ligne.produitId
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Retirer",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }

            // PAIEMENT ET VALIDATION
            item {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1EBFC)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            "Récapitulatif",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Total à payer : ${argent(totalPanier, devise)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF6C3CE9),
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = paiement,
                            onValueChange = {
                                paiement = it
                            },
                            label = {
                                Text("Montant payé maintenant")
                            },
                            placeholder = {
                                Text("Exemple : 20")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            "Le montant restant sera automatiquement enregistré comme dette du client.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Button(
                            onClick = {

                                val montantPaye = paiement
                                    .replace(",", ".")
                                    .toDoubleOrNull()

                                if (
                                    panier.isEmpty() ||
                                    client.isBlank() ||
                                    montantPaye == null ||
                                    montantPaye < 0 ||
                                    montantPaye > totalPanier
                                ) {

                                    messageVente =
                                        "Vérifiez le client, le panier et le paiement."

                                } else {

                                    val stockOK = panier.all { ligne ->

                                        val produit = produits.find {
                                            it.id == ligne.produitId
                                        }

                                        produit != null &&
                                        ligne.quantite <= produit.quantite
                                    }

                                    if (!stockOK) {

                                        messageVente = "Stock insuffisant."

                                    } else {

                                        val id = System.currentTimeMillis()

                                        val nouvelleVente = Vente(
                                            id,
                                            client.trim(),
                                            panier.toList(),
                                            montantPaye,
                                            id
                                        )

                                        // Mise à jour du stock
                                        panier.forEach { ligne ->

                                            val index = produits.indexOfFirst {
                                                it.id == ligne.produitId
                                            }

                                            if (index >= 0) {

                                                val produit = produits[index]

                                                produits[index] = produit.copy(
                                                    quantite =
                                                        produit.quantite - ligne.quantite
                                                )
                                            }
                                        }

                                        // Enregistrement de la vente
                                        ventes.add(nouvelleVente)

                                        derniereVenteValidee = nouvelleVente

                                        sauvegarderProduits()
                                        sauvegarderVentes()

                                        messageVente =
                                            "Vente enregistrée. Facture n° $id"

                                        // Réinitialisation du formulaire
                                        panier = emptyList()
                                        client = ""
                                        recherche = ""
                                        produitChoisi = null
                                        quantiteSaisie = "1"
                                        paiement = ""

                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = panier.isNotEmpty()
                        ) {
                            Text("Valider la vente")
                        }

                        if (messageVente.isNotBlank()) {
                            Text(
                                messageVente,
                                color = if (
                                    messageVente.startsWith("Vente enregistrée")
                                ) Color(0xFF16803C)
                                else Color.Red
                            )
                        }
                    }
                }
            }

            // FACTURE APRÈS VALIDATION
            item {

                derniereVenteValidee?.let { vente ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                "Vente enregistrée",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16803C)
                            )

                            Text("Facture n° ${vente.id}")

                            Button(
                                onClick = {

                                    val fichier = genererFacturePdf(
                                        context,
                                        vente,
                                        devise,
                                        nomEntreprise,
                                        adresseEntreprise,
                                        telephoneEntreprise,
                                        emailEntreprise,
                                        rccmEntreprise,
                                        idNatEntreprise,
                                        impotEntreprise,
                                        logoEntreprise
                                    )

                                    fichierFactureEnAttente = fichier

                                    enregistrerFactureLauncher.launch(
                                        "Facture-${vente.id}.pdf"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer la facture PDF")
                            }

                            OutlinedButton(
                                onClick = {

                                    val fichier = genererFacturePdf(
                                        context,
                                        vente,
                                        devise,
                                        nomEntreprise,
                                        adresseEntreprise,
                                        telephoneEntreprise,
                                        emailEntreprise,
                                        rccmEntreprise,
                                        idNatEntreprise,
                                        impotEntreprise,
                                        logoEntreprise
                                    )

                                    imprimerFacture(context, fichier)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Imprimer la facture")
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // ONGLET HISTORIQUE
        // ==========================================

        else {

            item {
                Text(
                    "Historique des ventes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Consultez les transactions et les paiements de vos clients.",
                    color = Color.Gray
                )
            }

            if (ventes.isEmpty()) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Aucune vente enregistrée.")
                        }
                    }
                }

            } else {

                items(
                    ventes.toList().reversed(),
                    key = { it.id }
                ) { vente ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {

                            Text(
                                "Facture n° ${vente.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6C3CE9)
                            )

                            Text("Client : ${vente.client}")

                            Text(
                                "Date : ${formaterDate(vente.date)}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            HorizontalDivider()

                            vente.lignes.forEach { ligne ->

                                Text(
                                    "${ligne.nom} × ${ligne.quantite}"
                                )

                                Text(
                                    argent(ligne.total, devise),
                                    color = Color.Gray
                                )
                            }

                            HorizontalDivider()

                            Text(
                                "Total : ${argent(vente.total, devise)}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Payé : ${argent(vente.paye, devise)}",
                                color = Color(0xFF16803C)
                            )

                            Text(
                                "Reste : ${argent(vente.reste, devise)}",
                                color = if (vente.reste > 0)
                                    Color(0xFFD97706)
                                else
                                    Color(0xFF16803C),
                                fontWeight = FontWeight.Bold
                            )

                            // PAIEMENT SUPPLÉMENTAIRE
                            if (vente.reste > 0) {

                                if (paiementId == vente.id) {

                                    OutlinedTextField(
                                        value = paiementSupplementaire,
                                        onValueChange = {
                                            paiementSupplementaire = it
                                        },
                                        label = {
                                            Text("Paiement supplémentaire")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {

                                            val montant =
                                                paiementSupplementaire
                                                    .replace(",", ".")
                                                    .toDoubleOrNull()

                                            if (
                                                montant != null &&
                                                montant > 0 &&
                                                montant <= vente.reste
                                            ) {

                                                val index = ventes.indexOfFirst {
                                                    it.id == vente.id
                                                }

                                                if (index >= 0) {

                                                    ventes[index] = vente.copy(
                                                        paye = vente.paye + montant
                                                    )

                                                    sauvegarderVentes()
                                                }

                                                paiementId = null
                                                paiementSupplementaire = ""

                                                messageVente =
                                                    "Paiement enregistré."

                                            } else {

                                                messageVente =
                                                    "Montant invalide."
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Confirmer le paiement")
                                    }

                                    TextButton(
                                        onClick = {
                                            paiementId = null
                                            paiementSupplementaire = ""
                                        }
                                    ) {
                                        Text("Annuler")
                                    }

                                } else {

                                    OutlinedButton(
                                        onClick = {
                                            paiementId = vente.id
                                            paiementSupplementaire = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Enregistrer un paiement")
                                    }
                                }
                            }

                            // FACTURE
                            OutlinedButton(
                                onClick = {

                                    val fichier = genererFacturePdf(
                                        context,
                                        vente,
                                        devise,
                                        nomEntreprise,
                                        adresseEntreprise,
                                        telephoneEntreprise,
                                        emailEntreprise,
                                        rccmEntreprise,
                                        idNatEntreprise,
                                        impotEntreprise,
                                        logoEntreprise
                                    )

                                    fichierFactureEnAttente = fichier

                                    enregistrerFactureLauncher.launch(
                                        "Facture-${vente.id}.pdf"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer la facture PDF")
                            }

                            OutlinedButton(
                                onClick = {

                                    val fichier = genererFacturePdf(
                                        context,
                                        vente,
                                        devise,
                                        nomEntreprise,
                                        adresseEntreprise,
                                        telephoneEntreprise,
                                        emailEntreprise,
                                        rccmEntreprise,
                                        idNatEntreprise,
                                        impotEntreprise,
                                        logoEntreprise
                                    )

                                    imprimerFacture(context, fichier)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Imprimer la facture")
                            }

                            if (messageVente.isNotBlank()) {
                                Text(
                                    messageVente,
                                    color = Color(0xFF16803C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
                        }
                                    
                                            
            
                        "Rapports" -> {

    var periode by remember { mutableStateOf("Tout") }

    val maintenant = System.currentTimeMillis()

    fun debutPeriode(type: String): Long {
        val cal = java.util.Calendar.getInstance()

        when (type) {
            "Aujourd'hui" -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
            }

            "Cette semaine" -> {
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
            }

            "Ce mois" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
            }

            else -> return 0L
        }

        return cal.timeInMillis
    }

    val debut = debutPeriode(periode)

    val ventesFiltrees = ventes.filter {
        it.date >= debut && it.date <= maintenant
    }

    val depensesFiltrees = depenses.filter {
        it.date >= debut && it.date <= maintenant
    }

    val caPeriode = ventesFiltrees.sumOf { it.total }
    val encaissePeriode = ventesFiltrees.sumOf { it.paye }
    val dettesPeriode = ventesFiltrees.sumOf { it.reste }
    val coutPeriode = ventesFiltrees.sumOf { it.cout }
    val rapportProduits = ventesFiltrees
    .flatMap { it.lignes }
    .groupBy { it.produitId }
    .map { (_, lignes) ->
        val premiereLigne = lignes.first()

        val quantiteVendue = lignes.sumOf { it.quantite }
        val chiffreAffaires = lignes.sumOf { it.total }
        val coutMarchandises = lignes.sumOf {
            it.prixAchat * it.quantite
        }

        Triple(
            premiereLigne.nom,
            quantiteVendue,
            chiffreAffaires
        )
    }
    .sortedByDescending { it.second }
    val contenuRapportVentes = buildList {
    add("Période : $periode")
    add("")
    add("RÉSUMÉ DES VENTES")
    add("Chiffre d'affaires : ${argent(caPeriode, devise)}")
    add("Montant encaissé : ${argent(encaissePeriode, devise)}")
    add("Dettes clients : ${argent(dettesPeriode, devise)}")
    add("Coût des marchandises : ${argent(coutPeriode, devise)}")
    add("")
    add("PRODUITS VENDUS")

    rapportProduits.forEach { produit ->
        add("${produit.first} | Quantité : ${produit.second} | Total : ${argent(produit.third, devise)}")
    }

    add("")
    add("HISTORIQUE DES VENTES")

    ventesFiltrees.forEach { vente ->
        add("Client : ${vente.client}")
        add("Date : ${java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(vente.date))}")
        add("Montant : ${argent(vente.total, devise)}")
        add("Payé : ${argent(vente.paye, devise)}")
        add("Reste : ${argent(vente.reste, devise)}")
        add("------------------------------")
    }
    }
    val depensesPeriode = depensesFiltrees.sumOf { it.montant }
        val contenuRapportDepenses = buildList {
        add("Période : $periode")
        add("")
        add("RÉSUMÉ DES DÉPENSES")
        add("Total des dépenses : ${argent(depensesPeriode, devise)}")
        add("")
        add("HISTORIQUE DES DÉPENSES")

        depensesFiltrees.forEach { depense ->
            add("Description : ${depense.description}")
            add(
                "Date : ${
                    java.text.SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(Date(depense.date))
                }"
            )
            add("Montant : ${argent(depense.montant, devise)}")
            add("------------------------------")
        }
        }
    val beneficeBrut = caPeriode - coutPeriode
                                val beneficeNet = beneficeBrut - depensesPeriode

    val contenuRapportBenefices = buildList {
        add("Période : $periode")
        add("")
        add("RÉSUMÉ DES BÉNÉFICES")
        add("Chiffre d'affaires : ${argent(caPeriode, devise)}")
        add("Coût des marchandises : ${argent(coutPeriode, devise)}")
        add("Bénéfice brut : ${argent(beneficeBrut, devise)}")
        add("Dépenses : ${argent(depensesPeriode, devise)}")
        add("")
        add("BÉNÉFICE NET ESTIMÉ")
        add(argent(beneficeNet, devise))
    }

    val produitsFaibles = produits.filter {
        it.quantite in 1..5
    }

    val produitsEpuises = produits.filter {
        it.quantite <= 0
    }
        val contenuRapportStock = buildList {
        add("Date du rapport : ${
            java.text.SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())
        }")
        add("")
        add("RÉSUMÉ DU STOCK")
        add("Nombre total de produits : ${produits.size}")
        add("Produits en stock faible : ${produitsFaibles.size}")
        add("Produits épuisés : ${produitsEpuises.size}")
        add("")
        add("ÉTAT DÉTAILLÉ DU STOCK")

        produits.forEach { produit ->
            add("Produit : ${produit.nom}")
            add("Quantité disponible : ${produit.quantite}")
            add("Prix de vente : ${argent(produit.vente, devise)}")

            when {
                produit.quantite <= 0 -> add("État : ÉPUISÉ")
                produit.quantite <= 5 -> add("État : STOCK FAIBLE")
                else -> add("État : DISPONIBLE")
            }

            add("------------------------------")
        }
            }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Rapports du commerce",
                style = MaterialTheme.typography.headlineSmall
            )
            Text("Analyse détaillée de votre activité.")
        }
                item {
            HorizontalDivider()

            Text(
                "Rapport des ventes",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES VENTES",
                        periode = periode,
                        contenu = contenuRapportVentes,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    fichierRapportEnAttente = fichier
                    enregistrerRapportLauncher.launch(
                        "Rapport-Ventes.pdf"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer les ventes en PDF")
            }

            OutlinedButton(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES VENTES",
                        periode = periode,
                        contenu = contenuRapportVentes,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    imprimerRapport(context, fichier)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimer le rapport des ventes")
            }
                }

                item {
            HorizontalDivider()

            Text(
                "Rapport des dépenses",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES DÉPENSES",
                        periode = periode,
                        contenu = contenuRapportDepenses,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    fichierRapportEnAttente = fichier
                    enregistrerRapportLauncher.launch(
                        "Rapport-Depenses.pdf"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer les dépenses en PDF")
            }

            OutlinedButton(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES DÉPENSES",
                        periode = periode,
                        contenu = contenuRapportDepenses,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    imprimerRapport(context, fichier)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimer le rapport des dépenses")
            }
                }
                        item {
            HorizontalDivider()

            Text(
                "Rapport des bénéfices",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES BÉNÉFICES",
                        periode = periode,
                        contenu = contenuRapportBenefices,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    fichierRapportEnAttente = fichier
                    enregistrerRapportLauncher.launch(
                        "Rapport-Benefices.pdf"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer les bénéfices en PDF")
            }

            OutlinedButton(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DES BÉNÉFICES",
                        periode = periode,
                        contenu = contenuRapportBenefices,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    imprimerRapport(context, fichier)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimer le rapport des bénéfices")
            }
                        }
                                item {
            HorizontalDivider()

            Text(
                "Rapport du stock",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DU STOCK",
                        periode = "Situation actuelle du stock",
                        contenu = contenuRapportStock,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    fichierRapportEnAttente = fichier
                    enregistrerRapportLauncher.launch("Rapport-Stock.pdf")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer le stock en PDF")
            }

            OutlinedButton(
                onClick = {
                    val fichier = genererRapportDocumentPdf(
                        context = context,
                        titreRapport = "RAPPORT DU STOCK",
                        periode = "Situation actuelle du stock",
                        contenu = contenuRapportStock,
                        nomEntreprise = nomEntreprise,
                        adresseEntreprise = adresseEntreprise,
                        telephoneEntreprise = telephoneEntreprise,
                        emailEntreprise = emailEntreprise,
                        rccmEntreprise = rccmEntreprise,
                        idNatEntreprise = idNatEntreprise,
                        impotEntreprise = impotEntreprise,
                        logoEntreprise = logoEntreprise,
                        lieuEntreprise = lieuEntreprise,
                        nomGerant = nomGerant,
                        fonctionGerant = fonctionGerant
                    )

                    imprimerRapport(context, fichier)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Imprimer le rapport du stock")
            }
                                }
        item {
            Text(
                "Choisir une période",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Aujourd'hui",
                    "Cette semaine",
                    "Ce mois",
                    "Tout"
                ).forEach { choix ->
                    FilterChip(
                        selected = periode == choix,
                        onClick = { periode = choix },
                        label = { Text(choix) }
                    )
                }
            }
        }

        item {
            Text(
                "1. Rapport des ventes",
                style = MaterialTheme.typography.titleLarge
            )
            DashboardCard("Chiffre d'affaires", argent(caPeriode, devise))
DashboardCard("Montant encaissé", argent(encaissePeriode, devise))
DashboardCard("Dettes clients", argent(dettesPeriode, devise))
            Text("Nombre de ventes : ${ventesFiltrees.size}")
        }

        item {
            HorizontalDivider()
            Text(
                "2. Rapport des bénéfices",
                style = MaterialTheme.typography.titleLarge
            )
            DashboardCard(
    "Coût des marchandises vendues",
    argent(coutPeriode, devise)
)
DashboardCard("Bénéfice brut", argent(beneficeBrut, devise))
DashboardCard("Dépenses déduites", argent(depensesPeriode, devise))
DashboardCard("Bénéfice net estimé", argent(beneficeNet, devise))
        }

        item {
            HorizontalDivider()
            Text(
                "3. Historique des ventes",
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
    HorizontalDivider()

    Text(
        "3. Rapport des produits vendus",
        style = MaterialTheme.typography.titleLarge
    )

    if (rapportProduits.isEmpty()) {
        Text("Aucun produit vendu pour cette période.")
    }
}

items(
    rapportProduits,
    key = { it.first }
) { produit ->
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                produit.first,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Quantité vendue : ${produit.second}")

            Text(
                "Chiffre d'affaires : " +
                    argent(produit.third, devise)
            )
        }
    }
}

        if (ventesFiltrees.isEmpty()) {
            item {
                Text("Aucune vente pour cette période.")
            }
        }

        items(
            ventesFiltrees.sortedByDescending { it.date },
            key = { it.id }
        ) { v ->
            Card {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Facture n° ${v.id}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Client : ${v.client}")
                    Text(
                        "Date : " +
                            java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(java.util.Date(v.date))
                    )
                    Text("Total : ${argent(v.total, devise)}")
Text("Payé : ${argent(v.paye, devise)}")
Text("Reste : ${argent(v.reste, devise)}")
Text(
    "Bénéfice sur cette vente : " +
        argent(v.total - v.cout, devise)
)
                }
            }
        }

        item {
            HorizontalDivider()
            Text(
                "4. Rapport des dépenses",
                style = MaterialTheme.typography.titleLarge
            )
            DashboardCard("Total des dépenses", argent(depensesPeriode, devise))
            Text("Nombre de dépenses : ${depensesFiltrees.size}")
        }

        if (depensesFiltrees.isEmpty()) {
            item {
                Text("Aucune dépense pour cette période.")
            }
        }

        items(
            depensesFiltrees.sortedByDescending { it.date },
            key = { it.id }
        ) { d ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        d.description,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(argent(d.montant, devise))
                    Text(
                        java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        ).format(java.util.Date(d.date))
                    )
                }
            }
        }

        item {
            HorizontalDivider()
            Text(
                "5. État du stock",
                style = MaterialTheme.typography.titleLarge
            )
            DashboardCard(
                "Produits disponibles",
                produits.count { it.quantite > 5 }.toString()
            )
            DashboardCard(
                "Stocks faibles (5 ou moins)",
                produitsFaibles.size.toString()
            )
            DashboardCard(
                "Produits épuisés",
                produitsEpuises.size.toString()
            )
            Text("Produits à surveiller")
        }

        items(
            produitsFaibles + produitsEpuises,
            key = { it.id }
        ) { p ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(p.nom)
                    Text("Stock restant : ${p.quantite}")
                }
            }
        }
    }
}

                        "Plus" -> {

    var confirmerDeconnexion by remember {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Text(
                    "Plus",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Gérez votre compte et configurez MelmaTech.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "MelmaTech",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Text(
                            if (estAdministrateur)
                                "Espace administrateur"
                            else
                                "Espace vendeur",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Text(
                        "⚙️",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }

        item {
            Text(
                "GESTION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                onClick = { page = "Depenses" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💸", style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dépenses et rapports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Suivre les dépenses et les résultats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            Card(
                onClick = { page = "Ventes" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧾", style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Factures et ventes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Consulter les ventes et les paiements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        if (estAdministrateur) {
            item {
                Text(
                    "ADMINISTRATION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    onClick = { page = "Utilisateurs" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👥", style = MaterialTheme.typography.headlineMedium)

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Gestion des utilisateurs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Gérer les comptes des vendeurs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text("›", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }

        item {
            Text(
                "CONFIGURATION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                onClick = { page = "Entreprise" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏢", style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Informations de l'entreprise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Logo, adresse, contacts, RCCM et ID NAT.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            Card(
                onClick = { page = "Paramètres" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙️", style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Paramètres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Devise et préférences de l'application.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))

            Card(
                onClick = { confirmerDeconnexion = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚪", style = MaterialTheme.typography.headlineMedium)

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Déconnexion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Fermer votre session en toute sécurité.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmerDeconnexion) {
        AlertDialog(
            onDismissRequest = {
                confirmerDeconnexion = false
            },
            title = {
                Text("Confirmer la déconnexion")
            },
            text = {
                Text("Voulez-vous vraiment vous déconnecter de MelmaTech ?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmerDeconnexion = false
                        AuthService.deconnecter(context)
                        utilisateurConnecte = false
                        page = "Accueil"
                        email = ""
                        motDePasse = ""
                        messageConnexion = ""
                    }
                ) {
                    Text("Se déconnecter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmerDeconnexion = false
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
                        }


                        "Entreprise" -> {

    if (!estAdministrateur) {
        Text("Accès réservé à l'administrateur.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Informations de l'entreprise",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Ces informations apparaîtront sur les documents officiels.")
            }

            item {
                OutlinedTextField(
                    value = nomEntreprise,
                    onValueChange = { nomEntreprise = it },
                    label = { Text("Nom de l'entreprise") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = adresseEntreprise,
                    onValueChange = { adresseEntreprise = it },
                    label = { Text("Adresse physique") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
    OutlinedTextField(
        value = lieuEntreprise,
        onValueChange = { lieuEntreprise = it },
        label = { Text("Lieu d'établissement des documents") },
        modifier = Modifier.fillMaxWidth()
    )
}

item {
    OutlinedTextField(
        value = nomGerant,
        onValueChange = { nomGerant = it },
        label = { Text("Nom du gérant") },
        modifier = Modifier.fillMaxWidth()
    )
}

item {
    OutlinedTextField(
        value = fonctionGerant,
        onValueChange = { fonctionGerant = it },
        label = { Text("Fonction du gérant") },
        modifier = Modifier.fillMaxWidth()
    )
}

            item {
                OutlinedTextField(
                    value = telephoneEntreprise,
                    onValueChange = { telephoneEntreprise = it },
                    label = { Text("Numéros de téléphone") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = emailEntreprise,
                    onValueChange = { emailEntreprise = it },
                    label = { Text("Adresse e-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = rccmEntreprise,
                    onValueChange = { rccmEntreprise = it },
                    label = { Text("Numéro RCCM") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = idNatEntreprise,
                    onValueChange = { idNatEntreprise = it },
                    label = { Text("Numéro ID NAT") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = impotEntreprise,
                    onValueChange = { impotEntreprise = it },
                    label = { Text("Numéro d'impôt") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Logo de l'entreprise",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        choisirLogo.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choisir le logo dans le téléphone")
                }

                if (logoEntreprise.isNotBlank()) {
    Image(
        painter = rememberAsyncImagePainter(model = logoEntreprise),
        contentDescription = "Logo de l'entreprise",
        modifier = Modifier.size(
            width = 160.dp * tailleLogoEntreprise,
            height = 120.dp * tailleLogoEntreprise
        ),
        contentScale = ContentScale.Fit
    )
                }
            }

            item {
                // Réglage de la taille du logo
Text("Taille du logo")

Text(
    when {
        tailleLogoEntreprise < 0.9f -> "Petit"
        tailleLogoEntreprise < 1.3f -> "Moyen"
        else -> "Grand"
    }
)

Slider(
    value = tailleLogoEntreprise,
    onValueChange = { tailleLogoEntreprise = it },
    valueRange = 0.7f..1.5f,
    steps = 1
)
            }
                item {
                    Button(
                    onClick = {
                        prefs.edit()
                            .putString("entreprise_nom", nomEntreprise)
                            .putString("entreprise_adresse", adresseEntreprise)
                            .putString("entreprise_telephone", telephoneEntreprise)
                            .putString("entreprise_email", emailEntreprise)
                            .putString("entreprise_rccm", rccmEntreprise)
                            .putString("entreprise_idnat", idNatEntreprise)
                            .putString("entreprise_impot", impotEntreprise)
                            .putString("entreprise_logo", logoEntreprise)
                            .putString("entreprise_lieu", lieuEntreprise)
                            .putString("entreprise_gerant", nomGerant)
                            .putString("entreprise_fonction_gerant", fonctionGerant)
                            .putFloat("entreprise_logo_taille", tailleLogoEntreprise)
.apply()
                        messageEntreprise = "Informations enregistrées avec succès."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer les informations")
                }

                Text(
                    messageEntreprise,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                OutlinedButton(
                    onClick = { page = "Plus" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retour")
                }
            }
        }
    }
}

                        "Paramètres" -> {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Paramètres",
            style = MaterialTheme.typography.headlineSmall
        )

        Text("Configuration de MelmaTech")

        Text("Choisir la devise principale")

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    devise = "USD"
                    prefs.edit()
                        .putString("devise", "USD")
                        .apply()
                }
            ) {
                Text(if (devise == "USD") "✓ USD ($)" else "USD ($)")
            }

            OutlinedButton(
                onClick = {
                    devise = "CDF"
                    prefs.edit()
                        .putString("devise", "CDF")
                        .apply()
                }
            ) {
                Text(if (devise == "CDF") "✓ CDF (FC)" else "CDF (FC)")
            }
        }

        Text("Devise actuelle : $devise")

        Text("Version : 1.0")

        OutlinedButton(
            onClick = { page = "Plus" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Retour")
        }
    }
}

                        
"Utilisateurs" -> {

    if (!estAdministrateur) {
        Text("Accès réservé à l'administrateur.")
    } else {

        var vendeurs by remember {
            mutableStateOf(emptyList<AuthService.UtilisateurEntreprise>())
        }

        var profilAdmin by remember {
            mutableStateOf<ProfilUtilisateur?>(null)
        }

        var rechercheUtilisateur by remember {
            mutableStateOf("")
        }

        var filtreUtilisateur by remember {
            mutableStateOf("Tous")
        }

        var chargementUtilisateurs by remember {
            mutableStateOf(false)
        }

        var messageUtilisateurs by remember {
            mutableStateOf("")
        }

        var afficherFormulaire by remember {
            mutableStateOf(false)
        }

        var vendeurModifie by remember {
            mutableStateOf<AuthService.UtilisateurEntreprise?>(null)
        }

        var nomSaisi by remember {
            mutableStateOf("")
        }

        var emailSaisi by remember {
            mutableStateOf("")
        }

        var motDePasseSaisi by remember {
            mutableStateOf("")
        }

        var afficherMotDePasse by remember {
            mutableStateOf(false)
        }

        var vendeurMotDePasse by remember {
            mutableStateOf<AuthService.UtilisateurEntreprise?>(null)
        }

        var nouveauMotDePasse by remember {
            mutableStateOf("")
        }

        var vendeurAction by remember {
            mutableStateOf<AuthService.UtilisateurEntreprise?>(null)
        }

        var typeAction by remember {
            mutableStateOf("")
        }

        val violet = Color(0xFF6C3CE9)
        val fond = Color(0xFFF6F4FB)
        val vert = Color(0xFF159B68)
        val rouge = Color(0xFFD94B55)
        // Gestion des photos de profil
var utilisateurPhotoId by remember {
    mutableStateOf<String?>(null)
}
var photoARecadrer by remember {
    mutableStateOf<Bitmap?>(null)
}

var chargementPhoto by remember {
    mutableStateOf(false)
}


val choisirPhotoUtilisateur =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        if (uri != null && utilisateurPhotoId != null) {

            chargementPhoto = true

            try {
                val typeMime =
                    context.contentResolver.getType(uri)
                        ?: "image/jpeg"

                if (typeMime !in listOf(
                        "image/jpeg",
                        "image/png",
                        "image/webp"
                    )
                ) {
                    chargementPhoto = false
                    messageUtilisateurs =
                        "Choisis une photo JPG, PNG ou WEBP."
                } else {

                    val image = context.contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }

                    if (image == null || image.isEmpty()) {
                        chargementPhoto = false
                        messageUtilisateurs =
                            "Impossible de lire cette photo."
                    } else {

                        val bitmap = BitmapFactory.decodeByteArray(
                            image,
                            0,
                            image.size
                        )

                        chargementPhoto = false

                        if (bitmap == null) {
                            messageUtilisateurs =
                                "Impossible de décoder cette photo."
                        } else {
                            photoARecadrer = bitmap
                        }
                    }
                }

            } catch (e: Exception) {
                chargementPhoto = false
                messageUtilisateurs =
                    "Erreur lors de la lecture de la photo."
            }
        }
    }
    
val validerPhotoRecadree: (Bitmap) -> Unit = { bitmap ->

    val cibleId = utilisateurPhotoId

    if (!cibleId.isNullOrBlank()) {

        chargementPhoto = true

        val sortie = java.io.ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            90,
            sortie
        )

        val imageFinale = sortie.toByteArray()

        AuthService.envoyerPhotoProfil(
            context = context,
            utilisateurCibleId = cibleId,
            image = imageFinale,
            typeMime = "image/jpeg"
        ) { succes, message, photoUrl ->

            chargementPhoto = false
            messageUtilisateurs = message

            if (succes && photoUrl != null) {

                if (cibleId == profilAdmin?.id) {

                    profilAdmin = profilAdmin?.copy(
                        photoUrl = photoUrl
                    )

                } else {

                    vendeurs = vendeurs.map { vendeur ->

                        if (vendeur.id == cibleId) {
                            vendeur.copy(photoUrl = photoUrl)
                        } else {
                            vendeur
                        }
                    }
                }

                photoARecadrer = null
                utilisateurPhotoId = null
            }
        }
    }
}



        val chargerVendeurs: () -> Unit = {
            chargementUtilisateurs = true

            AuthService.listerVendeurs(context) {
                    succes, message, liste ->

                chargementUtilisateurs = false

                if (succes) {
                    vendeurs = liste.filter {
                        it.role.equals("vendeur", ignoreCase = true)
                    }
                    messageUtilisateurs = ""
                } else {
                    messageUtilisateurs = message
                }
            }
        }

        LaunchedEffect(Unit) {
            AuthService.obtenirProfil(context) {
                    succes, _, profil ->
                if (succes) {
                    profilAdmin = profil
                }
            }

            chargerVendeurs()
        }

        val vendeursFiltres = vendeurs.filter { vendeur ->

            val correspondRecherche =
                vendeur.nomComplet.contains(
                    rechercheUtilisateur,
                    ignoreCase = true
                ) ||
                vendeur.email.contains(
                    rechercheUtilisateur,
                    ignoreCase = true
                )

            val correspondFiltre = when (filtreUtilisateur) {
                "Actifs" -> vendeur.actif
                "Inactifs" -> !vendeur.actif
                else -> true
            }

            correspondRecherche && correspondFiltre
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(fond)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            
            // EN-TÊTE DE GESTION DES UTILISATEURS

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "Gestion des utilisateurs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF252238)
                    )

                    Text(
                        "Gérez les accès et les membres de votre équipe.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // CARTE ADMINISTRATEUR

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = violet
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        if (!profilAdmin?.photoUrl.isNullOrBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    profilAdmin?.photoUrl
                                ),
                                contentDescription = "Photo administrateur",
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Color.White.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "A",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                profilAdmin?.nomComplet
                                    ?: "Administrateur MelmaTech",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "Administrateur",
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Text(
                                "Espace de gestion",
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall
                            )

                            TextButton(
                                onClick = {
                                    utilisateurPhotoId = profilAdmin?.id
                                    if (utilisateurPhotoId != null) {
                                        choisirPhotoUtilisateur.launch("image/*")
                                    }
                                },
                                enabled = !chargementPhoto,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    if (chargementPhoto)
                                        "Envoi en cours..."
                                    else
                                        "Changer la photo",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // BOUTON AJOUTER UN VENDEUR

            item {
                Button(
                    onClick = {
                        vendeurModifie = null
                        nomSaisi = ""
                        emailSaisi = ""
                        motDePasseSaisi = ""
                        messageUtilisateurs = ""
                        afficherFormulaire = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = violet
                    )
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Ajouter un vendeur",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // STATISTIQUES

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                vendeurs.size.toString(),
                                color = violet,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Total",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                vendeurs.count { it.actif }.toString(),
                                color = vert,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Actifs",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                vendeurs.count { !it.actif }.toString(),
                                color = rouge,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Inactifs",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // RECHERCHE

            item {
                OutlinedTextField(
                    value = rechercheUtilisateur,
                    onValueChange = {
                        rechercheUtilisateur = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = {
                        Text("Rechercher un vendeur")
                    },
                    placeholder = {
                        Text("Nom ou adresse e-mail")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Rechercher",
                            tint = violet
                        )
                    }
                )
            }

            // FILTRES

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tous", "Actifs", "Inactifs").forEach { filtre ->

                        FilterChip(
                            selected = filtreUtilisateur == filtre,
                            onClick = {
                                filtreUtilisateur = filtre
                            },
                            label = {
                                Text(
                                    filtre,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = violet.copy(
                                    alpha = 0.13f
                                ),
                                selectedLabelColor = violet
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    "Équipe commerciale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (chargementUtilisateurs) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = violet)
                    }
                }
            }

            if (messageUtilisateurs.isNotBlank()) {
                item {
                    Text(
                        messageUtilisateurs,
                        color = rouge
                    )
                }
            }

            if (!chargementUtilisateurs && vendeursFiltres.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Aucun vendeur trouvé",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Ajoutez un vendeur ou modifiez votre recherche.",
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            items(
                vendeursFiltres,
                key = { it.id }
            ) { vendeur ->

                
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color.White
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 3.dp
    )
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // INFORMATIONS DU VENDEUR

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (!vendeur.photoUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        vendeur.photoUrl
                    ),
                    contentDescription = "Photo vendeur",
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(violet.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        vendeur.nomComplet.firstOrNull()
                            ?.uppercase() ?: "V",
                        color = violet,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    vendeur.nomComplet,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF252238)
                )

                Text(
                    vendeur.email,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "Vendeur",
                    color = violet,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (vendeur.actif)
                    Color(0xFFE4F5EF)
                else
                    Color(0xFFFCE8EA)
            ) {
                Text(
                    if (vendeur.actif) "Actif" else "Inactif",
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 7.dp
                    ),
                    color = if (vendeur.actif)
                        Color(0xFF15966B)
                    else
                        Color(0xFFD04452),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // CHANGER LA PHOTO

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    utilisateurPhotoId = vendeur.id
                    choisirPhotoUtilisateur.launch("image/*")
                },
                enabled = !chargementPhoto
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Changer la photo",
                    tint = violet,
                    modifier = Modifier.size(19.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    "Changer la photo",
                    color = violet,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(
            color = Color(0xFFF0EDF5)
        )

        // ACTIONS DU VENDEUR

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    vendeurModifie = vendeur
                    nomSaisi = vendeur.nomComplet
                    emailSaisi = vendeur.email
                    messageUtilisateurs = ""
                    afficherFormulaire = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFEAF1FF),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Modifier",
                    tint = Color(0xFF2563EB)
                )
            }

            IconButton(
                onClick = {
                    vendeurMotDePasse = vendeur
                    nouveauMotDePasse = ""
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFF1EAFE),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    Icons.Filled.LockReset,
                    contentDescription = "Mot de passe",
                    tint = Color(0xFF8B5CF6)
                )
            }

            IconButton(
                onClick = {
                    vendeurAction = vendeur
                    typeAction = if (vendeur.actif)
                        "desactiver"
                    else
                        "reactiver"
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (vendeur.actif)
                            Color(0xFFFFF2E5)
                        else
                            Color(0xFFE4F5EF),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    if (vendeur.actif)
                        Icons.Filled.Block
                    else
                        Icons.Filled.CheckCircle,
                    contentDescription = if (vendeur.actif)
                        "Désactiver"
                    else
                        "Réactiver",
                    tint = if (vendeur.actif)
                        Color(0xFFD97706)
                    else
                        Color(0xFF15966B)
                )
            }

            IconButton(
                onClick = {
                    vendeurAction = vendeur
                    typeAction = "supprimer"
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFFFCE8EA),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Supprimer",
                    tint = Color(0xFFD04452)
                )
            }
        }
    }
}

            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }

                        if (afficherFormulaire) {
            AlertDialog(
                onDismissRequest = {
                    if (!chargementUtilisateurs) {
                        afficherFormulaire = false
                    }
                },
                containerColor = Color(0xFFF3F0FA),
                titleContentColor = Color(0xFF30145F),
                textContentColor = Color(0xFF655D70),
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Color(0xFF4B258D),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (vendeurModifie == null)
                                    Icons.Filled.Person
                                else
                                    Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Text(
                            text = if (vendeurModifie == null)
                                "Ajouter un vendeur"
                            else
                                "Modifier le vendeur",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF30145F)
                        )

                        Text(
                            text = if (vendeurModifie == null)
                                "Créez un accès pour un nouveau membre de votre équipe."
                            else
                                "Modifiez les informations de ce membre de votre équipe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF655D70)
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = nomSaisi,
                            onValueChange = {
                                nomSaisi = it
                            },
                            label = {
                                Text("Nom complet")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            enabled = !chargementUtilisateurs,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6540A5),
                                unfocusedBorderColor = Color(0xFFD8D0E3),
                                focusedLeadingIconColor = Color(0xFF6540A5),
                                unfocusedLeadingIconColor = Color(0xFF81778F),
                                focusedLabelColor = Color(0xFF6540A5)
                            )
                        )

                        OutlinedTextField(
                            value = emailSaisi,
                            onValueChange = {
                                emailSaisi = it
                            },
                            label = {
                                Text("Adresse e-mail")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Email,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            enabled = !chargementUtilisateurs,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6540A5),
                                unfocusedBorderColor = Color(0xFFD8D0E3),
                                focusedLeadingIconColor = Color(0xFF6540A5),
                                unfocusedLeadingIconColor = Color(0xFF81778F),
                                focusedLabelColor = Color(0xFF6540A5)
                            )
                        )

                        if (vendeurModifie == null) {
                            OutlinedTextField(
                                value = motDePasseSaisi,
                                onValueChange = {
                                    motDePasseSaisi = it
                                },
                                label = {
                                    Text("Mot de passe temporaire")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.LockReset,
                                        contentDescription = null
                                    )
                                },
                                visualTransformation =
                                    PasswordVisualTransformation(),
                                singleLine = true,
                                enabled = !chargementUtilisateurs,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6540A5),
                                    unfocusedBorderColor = Color(0xFFD8D0E3),
                                    focusedLeadingIconColor = Color(0xFF6540A5),
                                    unfocusedLeadingIconColor = Color(0xFF81778F),
                                    focusedLabelColor = Color(0xFF6540A5)
                                )
                            )

                            Text(
                                text = "Le vendeur pourra utiliser ces identifiants pour accéder à son compte.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF655D70)
                            )
                        }

                        if (messageUtilisateurs.isNotBlank()) {
                            Text(
                                text = messageUtilisateurs,
                                color = rouge,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !chargementUtilisateurs,
                        onClick = {
                            val nom = nomSaisi.trim()
                            val email = emailSaisi.trim()

                            if (
                                nom.isBlank() ||
                                !email.contains("@")
                            ) {
                                messageUtilisateurs =
                                    "Vérifie le nom et l'adresse e-mail."
                            } else if (
                                vendeurModifie == null &&
                                motDePasseSaisi.length < 8
                            ) {
                                messageUtilisateurs =
                                    "Le mot de passe doit contenir au moins 8 caractères."
                            } else {
                                chargementUtilisateurs = true

                                if (vendeurModifie == null) {
                                    AuthService.creerVendeur(
                                        context,
                                        nom,
                                        email,
                                        motDePasseSaisi
                                    ) { succes, message ->

                                        chargementUtilisateurs = false
                                        messageUtilisateurs = message

                                        if (succes) {
                                            afficherFormulaire = false
                                            motDePasseSaisi = ""
                                            chargerVendeurs()
                                        }
                                    }
                                } else {
                                    AuthService.modifierVendeur(
                                        context,
                                        vendeurModifie!!.id,
                                        nom,
                                        email
                                    ) { succes, message ->

                                        chargementUtilisateurs = false
                                        messageUtilisateurs = message

                                        if (succes) {
                                            afficherFormulaire = false
                                            chargerVendeurs()
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4B258D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (chargementUtilisateurs)
                                "Patiente..."
                            else if (vendeurModifie == null)
                                "Créer le vendeur"
                            else
                                "Enregistrer"
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !chargementUtilisateurs,
                        onClick = {
                            afficherFormulaire = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF4B258D)
                        )
                    ) {
                        Text("Annuler")
                    }
                }
            )
                        }

        vendeurMotDePasse?.let { vendeur ->

            AlertDialog(
                onDismissRequest = {
                    vendeurMotDePasse = null
                },
                title = {
                    Text("Changer le mot de passe")
                },
                text = {
                    Column {
                        Text(vendeur.nomComplet)

                        OutlinedTextField(
                            value = nouveauMotDePasse,
                            onValueChange = {
                                nouveauMotDePasse = it
                            },
                            label = {
                                Text("Nouveau mot de passe")
                            },
                            visualTransformation =
                                PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (nouveauMotDePasse.length < 8) {
                                messageUtilisateurs =
                                    "Minimum 8 caractères."
                            } else {
                                chargementUtilisateurs = true

                                AuthService.changerMotDePasseVendeur(
                                    context,
                                    vendeur.id,
                                    nouveauMotDePasse
                                ) { succes, message ->

                                    chargementUtilisateurs = false
                                    messageUtilisateurs = message

                                    if (succes) {
                                        vendeurMotDePasse = null
                                        nouveauMotDePasse = ""
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Confirmer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vendeurMotDePasse = null
                        }
                    ) {
                        Text("Annuler")
                    }
                }
            )
        }

        vendeurAction?.let { vendeur ->

            val titreAction = when (typeAction) {
                "desactiver" -> "Désactiver ce vendeur ?"
                "reactiver" -> "Réactiver ce vendeur ?"
                else -> "Supprimer ce vendeur ?"
            }

            AlertDialog(
                onDismissRequest = {
                    vendeurAction = null
                },
                title = {
                    Text(titreAction)
                },
                text = {
                    Text(
                        when (typeAction) {
                            "desactiver" ->
                                "Le vendeur ne pourra plus accéder à son compte."
                            "reactiver" ->
                                "Le vendeur pourra de nouveau accéder à son compte."
                            else ->
                                "Le compte sera supprimé de la gestion. Vérifie avant de continuer."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            chargementUtilisateurs = true

                            val action: (
                                (Boolean, String) -> Unit
                            ) -> Unit = { callback ->

                                when (typeAction) {
                                    "desactiver" ->
                                        AuthService.desactiverVendeur(
                                            context,
                                            vendeur.id,
                                            callback
                                        )

                                    "reactiver" ->
                                        AuthService.reactiverVendeur(
                                            context,
                                            vendeur.id,
                                            callback
                                        )

                                    else ->
                                        AuthService.supprimerVendeur(
                                            context,
                                            vendeur.id,
                                            callback
                                        )
                                }
                            }

                            action { succes, message ->

                                chargementUtilisateurs = false
                                messageUtilisateurs = message
                                vendeurAction = null

                                if (succes) {
                                    chargerVendeurs()
                                }
                            }
                        }
                    ) {
                        Text("Confirmer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vendeurAction = null
                        }
                    ) {
                        Text("Annuler")
                    }
                }
            )
        }
        photoARecadrer?.let { bitmap ->
    EditeurPhotoProfil(
        bitmap = bitmap,
        onAnnuler = {
            photoARecadrer = null
            utilisateurPhotoId = null
        },
        onValider = { photoRecadree ->
            validerPhotoRecadree(photoRecadree)
        }
    )
        }
    }
}
 

                        else -> {
                            Text("Page introuvable.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(titre: String, montant: String) {

    val couleur = when (titre) {
        "Produits enregistrés" -> Color(0xFF6C3CE9)
        "Chiffre d'affaires" -> Color(0xFF2878D0)
        "Argent encaissé" -> Color(0xFF239B68)
        "Dettes clients" -> Color(0xFFE58B25)
        "Dépenses" -> Color(0xFFD95757)
        "Bénéfice estimé" -> Color(0xFF159EAD)
        else -> Color(0xFF6C3CE9)
    }

    val icone = when (titre) {
        "Produits enregistrés" -> Icons.Filled.Inventory2
        "Chiffre d'affaires" -> Icons.Filled.BarChart
        "Argent encaissé" -> Icons.Filled.Payments
        "Dettes clients" -> Icons.Filled.AccountBalanceWallet
        "Dépenses" -> Icons.Filled.MoneyOff
        "Bénéfice estimé" -> Icons.Filled.TrendingUp
        else -> Icons.Filled.BarChart
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = couleur
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                imageVector = icone,
                contentDescription = titre,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = titre,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = montant,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun EcranConnexion(
    email: String,
    onEmailChange: (String) -> Unit,
    motDePasse: String,
    onMotDePasseChange: (String) -> Unit,
    modeCreationCompte: Boolean,
    chargement: Boolean,
    message: String,
    onChangerMode: () -> Unit,
    onValider: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
    painter = painterResource(id = R.drawable.melmatech_logo),
    contentDescription = "Logo MelmaTech",
    modifier = Modifier
        .fillMaxWidth()
        .height(150.dp),
    contentScale = ContentScale.Fit
)

        Spacer(Modifier.height(12.dp))

        Text(
            if (modeCreationCompte) "Créer un compte" else "Connexion",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Adresse e-mail") },
            singleLine = true,
            enabled = !chargement,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = motDePasse,
            onValueChange = onMotDePasseChange,
            label = { Text("Mot de passe") },
            singleLine = true,
            enabled = !chargement,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onValider,
            enabled = !chargement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (chargement) "Patiente..."
                else if (modeCreationCompte) "Créer mon compte"
                else "Se connecter"
            )
        }

        TextButton(
            onClick = onChangerMode,
            enabled = !chargement
        ) {
            Text(
                if (modeCreationCompte)
                    "J'ai déjà un compte"
                else
                    "Créer un nouveau compte"
            )
        }

        if (message.isNotBlank()) {
            Text(message)
        }
    }
}
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MelmaTechApp(this)
        }
    }
}
private fun genererFacturePdf(
    context: Context,
    vente: Vente,
    devise: String,
    nomEntreprise: String,
    adresseEntreprise: String,
    telephoneEntreprise: String,
    emailEntreprise: String,
    rccmEntreprise: String,
    idNatEntreprise: String,
        impotEntreprise: String,
    logoEntreprise: String
): File {

    val document = PdfDocument()
    val largeur = 595
    val hauteur = 842

    val pageInfo = PdfDocument.PageInfo.Builder(
        largeur, hauteur, 1
    ).create()

    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titre = Paint().apply {
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val texte = Paint().apply {
        textSize = 12f
    }

    val gras = Paint().apply {
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var y = 50f
    if (logoEntreprise.isNotBlank()) {
    try {
        val uri = Uri.parse(logoEntreprise)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)

            if (bitmap != null) {
                val logo = Bitmap.createScaledBitmap(
                    bitmap, 90, 90, true
                )
                canvas.drawBitmap(logo, 455f, 25f, null)
            }
        }
    } catch (_: Exception) {
    }
    }

    canvas.drawText(
        nomEntreprise.ifBlank { "Mon entreprise" },
        40f, y, titre
    )
    y += 30f
        listOf(
        adresseEntreprise,
        telephoneEntreprise,
        emailEntreprise,
        "RCCM : $rccmEntreprise",
        "ID NAT : $idNatEntreprise",
        "N° Impôt : $impotEntreprise"
    ).filter { it.isNotBlank() && !it.endsWith(": ") }
        .forEach {
            canvas.drawText(it, 40f, y, Paint().apply {
                textSize = 12f
            })
            y += 17f
        }

    y += 25f

    canvas.drawText("FACTURE", 40f, y, titre)
    y += 30f

    canvas.drawText("N° facture : ${vente.id}", 40f, y, texte)
    y += 20f

    val date = SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(vente.date))

    canvas.drawText("Date : $date", 40f, y, texte)
    y += 20f

    canvas.drawText(
        "Client : ${vente.client.ifBlank { "Client comptant" }}",
        40f, y, texte
    )
    y += 35f
        canvas.drawText("Article", 40f, y, gras)
    canvas.drawText("Qté", 300f, y, gras)
    canvas.drawText("Prix", 370f, y, gras)
    canvas.drawText("Total", 470f, y, gras)

    y += 10f
    canvas.drawLine(40f, y, 555f, y, texte)
    y += 22f

    vente.lignes.forEach { ligne ->
        canvas.drawText(ligne.nom.take(32), 40f, y, texte)
        canvas.drawText("${ligne.quantite}", 300f, y, texte)

        canvas.drawText(
            String.format(Locale.US, "%.2f", ligne.prixUnitaire),
            370f, y, texte
        )

        canvas.drawText(
            String.format(Locale.US, "%.2f", ligne.total),
            470f, y, texte
        )

        y += 22f
    }
        y += 15f
    canvas.drawLine(40f, y, 555f, y, texte)
    y += 30f

    canvas.drawText(
        "TOTAL : ${argent(vente.total, devise)}",
        350f, y, gras
    )
    y += 22f

    canvas.drawText(
        "Payé : ${argent(vente.paye, devise)}",
        350f, y, texte
    )
    y += 22f

    canvas.drawText(
        "Reste à payer : ${argent(vente.reste, devise)}",
        350f, y, gras
    )

    y += 55f
    canvas.drawText(
        "Merci pour votre confiance !",
        40f, y, texte
    )

    document.finishPage(page)

    val dossier = File(
        context.getExternalFilesDir(null),
        "Factures"
    )
    if (!dossier.exists()) dossier.mkdirs()

    val fichier = File(dossier, "Facture-${vente.id}.pdf")

    try {
        FileOutputStream(fichier).use {
            document.writeTo(it)
        }
    } finally {
        document.close()
    }

    return fichier
}
private fun imprimerFacture(
    context: Context,
    fichier: File
) {
    val printManager = context.getSystemService(
        Context.PRINT_SERVICE
    ) as PrintManager

    val adapter = object : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) return

            val info = PrintDocumentInfo.Builder(fichier.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                FileInputStream(fichier).use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(
                        destination
                    ).use { output ->
                        input.copyTo(output)
                    }
                }

                callback.onWriteFinished(
                    arrayOf(PageRange.ALL_PAGES)
                )
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    printManager.print(
        "Facture-${fichier.name}",
        adapter,
        PrintAttributes.Builder().build()
    )
}
private fun imprimerRapport(context: Context, fichier: File) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager

    val adapter = object : android.print.PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: android.print.PrintAttributes?,
            newAttributes: android.print.PrintAttributes,
            cancellationSignal: android.os.CancellationSignal,
            callback: LayoutResultCallback,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal.isCanceled) return

            callback.onLayoutFinished(
                android.print.PrintDocumentInfo.Builder(fichier.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<out android.print.PageRange>,
            destination: android.os.ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                java.io.FileInputStream(fichier).use { input ->
                    java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }

                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    printManager.print(
        "Rapport-${fichier.name}",
        adapter,
        android.print.PrintAttributes.Builder().build()
    )
}

private fun genererRapportDocumentPdf(
    context: Context,
    titreRapport: String,
    periode: String,
    contenu: List<String>,
    nomEntreprise: String,
    adresseEntreprise: String,
    telephoneEntreprise: String,
    emailEntreprise: String,
    rccmEntreprise: String,
    idNatEntreprise: String,
    impotEntreprise: String,
    logoEntreprise: String,
    lieuEntreprise: String,
    nomGerant: String,
    fonctionGerant: String
): File {

    val lignesParPage = 30
    val groupes = contenu.chunked(lignesParPage)
        .ifEmpty { listOf(emptyList()) }

    val document = PdfDocument()
    val largeur = 595
    val hauteur = 842

    val titre = Paint().apply {
        textSize = 19f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val gras = Paint().apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val texte = Paint().apply {
        textSize = 10f
    }

    try {
        groupes.forEachIndexed { index, lignes ->

            val pageInfo = PdfDocument.PageInfo.Builder(
                largeur, hauteur, index + 1
            ).create()

            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = 35f

            if (index == 0 && logoEntreprise.isNotBlank()) {
                try {
                    val uri = Uri.parse(logoEntreprise)
                    context.contentResolver
                        .openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input)?.let { bitmap ->
                                val ratio = minOf(
                                    90f / bitmap.width,
                                    65f / bitmap.height
                                )
                                val logo = Bitmap.createScaledBitmap(
                                    bitmap,
                                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                                    true
                                )
                                canvas.drawBitmap(logo, 40f, 25f, null)
                            }
                        }
                } catch (_: Exception) {
                }
            }

            val entreprise = Paint().apply {
                textSize = 13f
                typeface = Typeface.create(
                    Typeface.DEFAULT, Typeface.BOLD
                )
            }

            canvas.drawText(
                nomEntreprise.ifBlank { "Mon entreprise" },
                150f, y, entreprise
            )
            y += 17f

            listOf(
                adresseEntreprise,
                telephoneEntreprise,
                emailEntreprise,
                "RCCM : $rccmEntreprise",
                "ID NAT : $idNatEntreprise",
                "N° Impôt : $impotEntreprise"
            ).filter { it.isNotBlank() && !it.endsWith(": ") }
                .forEach {
                    canvas.drawText(it, 150f, y, texte)
                    y += 14f
                }

            y = maxOf(y + 12f, 110f)

            canvas.drawLine(40f, y, 555f, y, texte)
            y += 30f

            canvas.drawText(titreRapport, largeur / 2f, y, titre)
            y += 24f

            val paintCentre = Paint(texte).apply {
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                periode,
                largeur / 2f,
                y,
                paintCentre
            )
            y += 28f

            canvas.drawLine(40f, y, 555f, y, texte)
            y += 25f

            lignes.forEach { ligne ->
                canvas.drawText(ligne.take(85), 40f, y, texte)
                y += 20f
            }

            if (index == groupes.lastIndex) {
                y = maxOf(y + 25f, 730f)

                canvas.drawText(
                    "Fait à ${lieuEntreprise.ifBlank { "________________" }}, le " +
                        java.text.SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(Date()),
                    40f, y, texte
                )
                y += 28f

                canvas.drawText("Par : $nomGerant", 40f, y, gras)
                y += 20f

                canvas.drawText(
                    "Fonction : $fonctionGerant",
                    40f, y, texte
                )
                y += 45f

                canvas.drawLine(40f, y, 220f, y, texte)
                canvas.drawText(
                    "Signature et cachet",
                    40f, y + 16f, texte
                )
            }

            document.finishPage(page)
        }

        val dossier = File(
            context.getExternalFilesDir(null),
            "Rapports"
        )

        if (!dossier.exists()) dossier.mkdirs()

        val fichier = File(
            dossier,
            "Rapport-${System.currentTimeMillis()}.pdf"
        )

        FileOutputStream(fichier).use {
            document.writeTo(it)
        }

        return fichier

    } finally {
        document.close()
    }
}

@Composable
fun EditeurPhotoProfil(
    bitmap: Bitmap,
    onAnnuler: () -> Unit,
    onValider: (Bitmap) -> Unit
) {
    var zoom by remember { mutableStateOf(1f) }
    var decalage by remember { mutableStateOf(Offset.Zero) }
    var tailleZone by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onAnnuler) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recadrer la photo",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { tailleZone = it }
                        .pointerInput(bitmap) {
                            detectTransformGestures {
                                    _, pan, zoomChange, _ ->

                                zoom = (zoom * zoomChange)
                                    .coerceIn(1f, 5f)

                                decalage += pan
                            }
                        }
                ) {
                    val cote = minOf(size.width, size.height)

                    val echelleBase = maxOf(
                        cote / bitmap.width,
                        cote / bitmap.height
                    )

                    val echelle = echelleBase * zoom

                    val largeur = bitmap.width * echelle
                    val hauteur = bitmap.height * echelle

                    val gauche =
                        (size.width - largeur) / 2f + decalage.x

                    val haut =
                        (size.height - hauteur) / 2f + decalage.y

                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(
                            gauche.roundToInt(),
                            haut.roundToInt()
                        ),
                        dstSize = IntSize(
                            largeur.roundToInt(),
                            hauteur.roundToInt()
                        )
                    )

                    drawCircle(
                        color = Color.White,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Déplace et agrandis la photo avec tes doigts.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onAnnuler) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        val cote = tailleZone.width
                            .coerceAtMost(tailleZone.height)
                            .toFloat()

                        if (cote > 0f) {
                            val echelleBase = maxOf(
                                cote / bitmap.width,
                                cote / bitmap.height
                            )

                            val echelle = echelleBase * zoom

                            val largeur = bitmap.width * echelle
                            val hauteur = bitmap.height * echelle

                            val gauche =
                                (cote - largeur) / 2f + decalage.x

                            val haut =
                                (cote - hauteur) / 2f + decalage.y

                            val centreX =
                                (cote / 2f - gauche) / echelle

                            val centreY =
                                (cote / 2f - haut) / echelle

                            val tailleSource = cote / echelle

                            val x = (centreX - tailleSource / 2f)
                                .roundToInt()
                                .coerceIn(
                                    0,
                                    bitmap.width - tailleSource.roundToInt()
                                )

                            val y = (centreY - tailleSource / 2f)
                                .roundToInt()
                                .coerceIn(
                                    0,
                                    bitmap.height - tailleSource.roundToInt()
                                )

                            val coteSource = tailleSource
                                .roundToInt()
                                .coerceAtLeast(1)
                                .coerceAtMost(
                                    minOf(bitmap.width - x, bitmap.height - y)
                                )

                            val photoRecadree = Bitmap.createBitmap(
                                bitmap,
                                x,
                                y,
                                coteSource,
                                coteSource
                            )

                            onValider(photoRecadree)
                        }
                    }
                ) {
                    Text("Valider")
                }
            }
        }
    }
}


