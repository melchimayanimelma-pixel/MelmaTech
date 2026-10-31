package com.melmatech.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.BackHandler

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


    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var modeCreation by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(false) }
    var messageConnexion by remember { mutableStateOf("") }

    var messageProduit by remember { mutableStateOf("") }
    var messageVente by remember { mutableStateOf("") }
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

    // Champs de vente.
    var client by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }
    var produitChoisi by remember { mutableStateOf<Produit?>(null) }
    var quantiteSaisie by remember { mutableStateOf("1") }
    var panier by remember { mutableStateOf(listOf<LigneVente>()) }
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
                        title = { Text("MelmaTech") },
                        actions = {
                            TextButton(
                                onClick = {
                                    AuthService.deconnecter(context)
                                    utilisateurConnecte = false
                                    page = "Accueil"
                                    email = ""
                                    motDePasse = ""
                                    messageConnexion = ""
                                }
                            ) {
                                Text("Déconnexion")
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
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
                            NavigationBarItem(
                                selected = page == onglet,
                                onClick = { page = onglet },
                                icon = {},
                                label = { Text(onglet) }
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
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text(
                                        "Bienvenue sur MelmaTech",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text("Gestion de votre commerce en $devise")

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

                            if (!estAdministrateur) {
                                Text("Accès réservé à l'administrateur.")
                            } else {

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            "Gestion des produits",
                                            style = MaterialTheme.typography.headlineSmall
                                        )

                                        OutlinedTextField(
                                            value = nomProduit,
                                            onValueChange = {
                                                nomProduit = it
                                            },
                                            label = {
                                                Text("Nom du produit")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = achatProduit,
                                            onValueChange = {
                                                achatProduit = it
                                            },
                                            label = {
                                                Text("Prix d'achat (USD)")
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = venteProduit,
                                            onValueChange = {
                                                venteProduit = it
                                            },
                                            label = {
                                                Text("Prix de vente (USD)")
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = stockProduit,
                                            onValueChange = {
                                                stockProduit = it
                                            },
                                            label = {
                                                Text("Quantité en stock")
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                val a = achatProduit
                                                    .replace(",", ".")
                                                    .toDoubleOrNull()

                                                val v = venteProduit
                                                    .replace(",", ".")
                                                    .toDoubleOrNull()

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
                                                        val nouvelId =
                                                            System.currentTimeMillis()

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
                                                            AuthService.obtenirEntrepriseId(
                                                                context
                                                            ) { ok, message, entrepriseId ->

                                                                if (
                                                                    ok &&
                                                                    entrepriseId != null
                                                                ) {
                                                                    SupabaseService.ajouterProduit(
                                                                        token,
                                                                        entrepriseId,
                                                                        nomProduit.trim(),
                                                                        a,
                                                                        v,
                                                                        q
                                                                    ) { enregistre, resultat, uuid ->

                                                                        if (
                                                                            enregistre &&
                                                                            uuid != null
                                                                        ) {
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
                                                        val index =
                                                            produits.indexOfFirst {
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
                                                if (editionId == null)
                                                    "Ajouter le produit"
                                                else
                                                    "Enregistrer les modifications"
                                            )
                                        }

                                        Text(messageProduit)
                                        HorizontalDivider()
                                        Text("Liste des produits (${produits.size})")
                                    }

                                    items(
                                        produits.toList(),
                                        key = { it.id }
                                    ) { p ->

                                        Card {
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    p.nom,
                                                    style = MaterialTheme.typography.titleMedium
                                                )

                                                Text("Achat : ${argent(p.achat, devise)}")
Text("Vente : ${argent(p.vente, devise)}")
                                                Text("Stock : ${p.quantite}")

                                                if (estAdministrateur) {
                                                    Row {
                                                        TextButton(
                                                            onClick = {
                                                                editionId = p.id
                                                                nomProduit = p.nom
                                                                achatProduit =
                                                                    p.achat.toString()
                                                                venteProduit =
                                                                    p.vente.toString()
                                                                stockProduit =
                                                                    p.quantite.toString()
                                                            }
                                                        ) {
                                                            Text("Modifier")
                                                        }

                                                        TextButton(
                                                            onClick = {
                                                                produits.removeAll {
                                                                    it.id == p.id
                                                                }
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
                        }
                        "Ventes" -> {

                            val suggestions = produits.filter {
                                produitChoisi == null &&
                                recherche.isNotBlank() &&
                                it.nom.contains(
                                    recherche.trim(),
                                    ignoreCase = true
                                ) &&
                                it.quantite > 0
                            }.take(6)

                            val totalPanier = panier.sumOf { it.total }

                            LazyColumn(
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text(
                                        "Vente rapide",
                                        style = MaterialTheme.typography.headlineSmall
                                    )

                                    OutlinedTextField(
                                        value = client,
                                        onValueChange = { client = it },
                                        label = { Text("Nom du client") },
                                        modifier = Modifier.fillMaxWidth()
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
                                            Text("Tape les premières lettres...")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                items(suggestions, key = { it.id }) { p ->
                                    Card(
                                        onClick = {
                                            produitChoisi = p
                                            recherche = p.nom
                                            quantiteSaisie = "1"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            Modifier.padding(12.dp)
                                        ) {
                                            Text(p.nom)
                                            Text(
    "${argent(p.vente, devise)} — Stock : ${p.quantite}"
)
                                        }
                                    }
                                }

                                item {
                                    produitChoisi?.let { p ->

                                        Text("Produit choisi : ${p.nom}")

                                        OutlinedTextField(
                                            value = quantiteSaisie,
                                            onValueChange = {
                                                quantiteSaisie = it
                                            },
                                            label = { Text("Quantité") },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                val q =
                                                    quantiteSaisie.toIntOrNull()

                                                val dejaAjoute = panier
                                                    .filter {
                                                        it.produitId == p.id
                                                    }
                                                    .sumOf { it.quantite }

                                                if (
                                                    q != null &&
                                                    q > 0 &&
                                                    q + dejaAjoute <= p.quantite
                                                ) {
                                                    val index = panier.indexOfFirst {
                                                        it.produitId == p.id
                                                    }

                                                    panier = if (index >= 0) {
                                                        panier.mapIndexed { i, ligne ->
                                                            if (i == index) {
                                                                ligne.copy(
                                                                    quantite =
                                                                        ligne.quantite + q
                                                                )
                                                            } else ligne
                                                        }
                                                    } else {
                                                        panier + LigneVente(
                                                            p.id,
                                                            p.nom,
                                                            q,
                                                            p.vente,
                                                            p.achat
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
                                            Text("Ajouter au panier")
                                        }
                                    }

                                    HorizontalDivider()

                                    Text(
                                        "Panier (${panier.size} lignes)",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                items(panier, key = { it.produitId }) { ligne ->
                                    Card {
                                        Column(
                                            Modifier.padding(12.dp)
                                        ) {
                                            Text(ligne.nom)
                                            Text("Quantité : ${ligne.quantite}")
                                            Text("Total : ${argent(ligne.total, devise)}")

                                            TextButton(
                                                onClick = {
                                                    panier = panier.filter {
                                                        it.produitId != ligne.produitId
                                                    }
                                                }
                                            ) {
                                                Text("Retirer")
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        "TOTAL : ${argent(totalPanier, devise)}",
                                        style = MaterialTheme.typography.headlineSmall
                                    )

                                    OutlinedTextField(
                                        value = paiement,
                                        onValueChange = { paiement = it },
                                        label = {
                                            Text("Montant payé maintenant (USD)")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.fillMaxWidth()
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
                                                    "Vérifie le client, le panier et le paiement."
                                            } else {

                                                val stockOK = panier.all { ligne ->
                                                    val p = produits.find {
                                                        it.id == ligne.produitId
                                                    }

                                                    p != null &&
                                                        ligne.quantite <= p.quantite
                                                }

                                                if (!stockOK) {
                                                    messageVente =
                                                        "Stock insuffisant."
                                                } else {

                                                    val id =
                                                        System.currentTimeMillis()

                                                    val nouvelleVente = Vente(
                                                        id,
                                                        client.trim(),
                                                        panier.toList(),
                                                        montantPaye,
                                                        id
                                                    )

                                                    panier.forEach { ligne ->
                                                        val index =
                                                            produits.indexOfFirst {
                                                                it.id == ligne.produitId
                                                            }

                                                        if (index >= 0) {
                                                            val p = produits[index]

                                                            produits[index] =
                                                                p.copy(
                                                                    quantite =
                                                                        p.quantite - ligne.quantite
                                                                )
                                                        }
                                                    }

                                                    ventes.add(nouvelleVente)

                                                    sauvegarderProduits()
                                                    sauvegarderVentes()

                                                    messageVente =
                                                        "Vente enregistrée. Facture n° $id"

                                                    panier = emptyList()
                                                    client = ""
                                                    recherche = ""
                                                    produitChoisi = null
                                                    paiement = ""
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Valider la vente")
                                    }

                                    Text(messageVente)

                                    HorizontalDivider()

                                    Text(
                                        "Historique des ventes",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                items(
                                    ventes.toList().reversed(),
                                    key = { it.id }
                                ) { v ->

                                    Card {
                                        Column(
                                            Modifier.padding(12.dp),
                                            verticalArrangement =
                                                Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "Facture n° ${v.id}",
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            Text("Client : ${v.client}")

                                            v.lignes.forEach { ligne ->
                                                Text(
    "${ligne.nom} × ${ligne.quantite} = ${argent(ligne.total, devise)}"
)
                                                )
                                            }

                                            Text("Total : ${argent(v.total, devise)}")
Text("Payé : ${argent(v.paye, devise)}")
Text("Reste : ${argent(v.reste, devise)}")
                                            if (v.reste > 0) {
                                                if (paiementId == v.id) {

                                                    OutlinedTextField(
                                                        value = paiementSupplementaire,
                                                        onValueChange = {
                                                            paiementSupplementaire = it
                                                        },
                                                        label = {
                                                            Text("Paiement supplémentaire")
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Button(
                                                        onClick = {
                                                            val m =
                                                                paiementSupplementaire
                                                                    .replace(",", ".")
                                                                    .toDoubleOrNull()

                                                            if (
                                                                m != null &&
                                                                m > 0 &&
                                                                m <= v.reste
                                                            ) {
                                                                val index =
                                                                    ventes.indexOfFirst {
                                                                        it.id == v.id
                                                                    }

                                                                if (index >= 0) {
                                                                    ventes[index] =
                                                                        v.copy(
                                                                            paye = v.paye + m
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
                                                        }
                                                    ) {
                                                        Text("Enregistrer le paiement")
                                                    }
                                                } else {
                                                    TextButton(
                                                        onClick = {
                                                            paiementId = v.id
                                                            paiementSupplementaire = ""
                                                        }
                                                    ) {
                                                        Text("Ajouter un paiement")
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
    val depensesPeriode = depensesFiltrees.sumOf { it.montant }
    val beneficeBrut = caPeriode - coutPeriode
    val beneficeNet = beneficeBrut - depensesPeriode

    val produitsFaibles = produits.filter {
        it.quantite in 1..5
    }

    val produitsEpuises = produits.filter {
        it.quantite <= 0
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
            DashboardCard("Total des dépenses", argent(depensesPeriode))
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
                    Text(argent(d.montant))
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Plus",
                style = MaterialTheme.typography.headlineMedium
            )
            Text("Services et configuration de MelmaTech")
        }

        item {
            Card(
                onClick = { page = "Rapports" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "💸 Dépenses et rapports",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Consulter les résultats et les dépenses.")
                }
            }
        }

        item {
            Card(
                onClick = { page = "Ventes" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🧾 Factures et ventes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Consulter les ventes et les paiements.")
                }
            }
        }

        if (estAdministrateur) {
            item {
                Card(
                    onClick = { page = "Utilisateurs" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "👥 Gestion des utilisateurs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Gérer les comptes des vendeurs.")
                    }
                }
            }
        }

        item {
            Card(
                onClick = { page = "Entreprise" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🏢 Informations de l'entreprise",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Logo, adresse, contacts, RCCM et ID NAT.")
                }
            }
        }

        item {
            Card(
                onClick = { page = "Paramètres" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "⚙️ Paramètres",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Configuration de l'application.")
                }
            }
        }

        item {
            HorizontalDivider()

            Card(
                onClick = { confirmerDeconnexion = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🚪 Déconnexion",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Fermer la session en toute sécurité.")
                }
            }
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
.putFloat("entreprise_logo_taille", tailleLogoEntreprise)

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

                                var nomVendeur by remember {
                                    mutableStateOf("")
                                }

                                var emailVendeur by remember {
                                    mutableStateOf("")
                                }

                                var motDePasseVendeur by remember {
                                    mutableStateOf("")
                                }

                                var messageVendeur by remember {
                                    mutableStateOf("")
                                }

                                var chargementVendeur by remember {
                                    mutableStateOf(false)
                                }

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text(
                                            "Gestion des utilisateurs",
                                            style = MaterialTheme.typography.headlineSmall
                                        )

                                        Text("Créer les comptes des vendeurs.")
                                    }

                                    item {
                                        OutlinedTextField(
                                            value = nomVendeur,
                                            onValueChange = {
                                                nomVendeur = it
                                            },
                                            label = {
                                                Text("Nom complet du vendeur")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !chargementVendeur
                                        )

                                        OutlinedTextField(
                                            value = emailVendeur,
                                            onValueChange = {
                                                emailVendeur = it
                                            },
                                            label = {
                                                Text("Adresse e-mail")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !chargementVendeur
                                        )

                                        OutlinedTextField(
                                            value = motDePasseVendeur,
                                            onValueChange = {
                                                motDePasseVendeur = it
                                            },
                                            label = {
                                                Text("Mot de passe temporaire")
                                            },
                                            visualTransformation =
                                                PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !chargementVendeur
                                        )

                                        Button(
                                            onClick = {
                                                val n = nomVendeur.trim()
                                                val e = emailVendeur.trim()

                                                if (
                                                    n.isBlank() ||
                                                    !e.contains("@") ||
                                                    motDePasseVendeur.length < 8
                                                ) {
                                                    messageVendeur =
                                                        "Vérifie le nom, l'e-mail et le mot de passe."
                                                } else {
                                                    chargementVendeur = true

                                                    AuthService.creerVendeur(
                                                        context,
                                                        n,
                                                        e,
                                                        motDePasseVendeur
                                                    ) { succes, message ->
                                                        chargementVendeur = false
                                                        messageVendeur = message

                                                        if (succes) {
                                                            nomVendeur = ""
                                                            emailVendeur = ""
                                                            motDePasseVendeur = ""
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !chargementVendeur
                                        ) {
                                            if (chargementVendeur) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text("Créer le compte vendeur")
                                            }
                                        }

                                        Text(messageVendeur)
                                    }
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp)
        ) {
            Text(
                titre,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                montant,
                style = MaterialTheme.typography.headlineMedium
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
        Text(
            "MelmaTech",
            style = MaterialTheme.typography.headlineLarge
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
