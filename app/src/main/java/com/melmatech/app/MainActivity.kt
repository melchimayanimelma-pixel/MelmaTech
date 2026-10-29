package com.melmatech.app

import android.os.Bundle
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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

fun argent(montant: Double): String =
    "$" + String.format(Locale.US, "%.2f", montant)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MelmaTechApp(this)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelmaTechApp(context: Context) {
    var statutConnexion by remember {
        mutableStateOf("Connexion non testée")
    }

    var utilisateurConnecte by remember {
        mutableStateOf(AuthService.estConnecte(context))
    }

    var emailConnexion by remember {
        mutableStateOf("")
    }

    var motDePasseConnexion by remember {
        mutableStateOf("")
    }

    var messageConnexion by remember {
        mutableStateOf("")
    }

    var chargementConnexion by remember {
        mutableStateOf(false)
    }

    var modeCreationCompte by remember {
        mutableStateOf(false)
    }
    val prefs = remember {
        context.getSharedPreferences("melmatech", Context.MODE_PRIVATE)
    }

    val produits = remember {
        mutableStateListOf<Produit>().apply {
            try {
                val json = JSONArray(prefs.getString("produits", "[]"))
                for (i in 0 until json.length()) {
                    val p = json.getJSONObject(i)
                    add(
                        Produit(
                            p.getLong("id"),
                            p.getString("nom"),
                            p.getDouble("achat"),
                            p.getDouble("vente"),
                            p.getInt("quantite")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    val ventes = remember {
        mutableStateListOf<Vente>().apply {
            try {
                val json = JSONArray(prefs.getString("ventes_nouvelles", "[]"))
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
            } catch (_: Exception) {}
        }
    }

    val depenses = remember {
        mutableStateListOf<Depense>().apply {
            try {
                val json = JSONArray(prefs.getString("depenses", "[]"))
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
            } catch (_: Exception) {}
        }
    }

    fun sauvegarderProduits() {
        val json = JSONArray()
        produits.forEach { p ->
            json.put(JSONObject().apply {
                put("id", p.id)
                put("nom", p.nom)
                put("achat", p.achat)
                put("vente", p.vente)
                put("quantite", p.quantite)
            })
        }
        prefs.edit().putString("produits", json.toString()).apply()
    }

    fun sauvegarderVentes() {
        val json = JSONArray()
        ventes.forEach { v ->
            val lignes = JSONArray()
            v.lignes.forEach { l ->
                lignes.put(JSONObject().apply {
                    put("produitId", l.produitId)
                    put("nom", l.nom)
                    put("quantite", l.quantite)
                    put("prixUnitaire", l.prixUnitaire)
                    put("prixAchat", l.prixAchat)
                })
            }

            json.put(JSONObject().apply {
                put("id", v.id)
                put("client", v.client)
                put("lignes", lignes)
                put("paye", v.paye)
                put("date", v.date)
            })
        }
        prefs.edit().putString("ventes_nouvelles", json.toString()).apply()
    }

    fun sauvegarderDepenses() {
        val json = JSONArray()
        depenses.forEach { d ->
            json.put(JSONObject().apply {
                put("id", d.id)
                put("description", d.description)
                put("montant", d.montant)
                put("date", d.date)
            })
        }
        prefs.edit().putString("depenses", json.toString()).apply()
    }

    var page by remember { mutableStateOf("Accueil") }

    var nom by remember { mutableStateOf("") }
    var achat by remember { mutableStateOf("") }
    var prixVente by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var editionId by remember { mutableStateOf<Long?>(null) }
    var messageProduit by remember { mutableStateOf("") }
    var chargementProduits by remember {
    mutableStateOf(false)
    }
    LaunchedEffect(utilisateurConnecte) {
        if (utilisateurConnecte) {
            val accessToken = AuthService.obtenirAccessToken(context)

            if (!accessToken.isNullOrBlank()) {
                chargementProduits = true

                AuthService.obtenirEntrepriseId(context) { succes, message, entrepriseId ->
                    if (succes && entrepriseId != null) {
                        SupabaseService.chargerProduits(
                            accessToken,
                            entrepriseId
                        ) { ok, resultat, produitsEnLigne ->
                            chargementProduits = false

                            if (ok && produitsEnLigne != null) {
                                messageProduit =
                                    "${produitsEnLigne.length()} produit(s) trouvé(s) sur Supabase."
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
        }
    }

    var client by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }
    var produitChoisi by remember { mutableStateOf<Produit?>(null) }
    var quantiteSaisie by remember { mutableStateOf("1") }
    var panier by remember { mutableStateOf(listOf<LigneVente>()) }
    var paiement by remember { mutableStateOf("") }
    var messageVente by remember { mutableStateOf("") }
    var paiementId by remember { mutableStateOf<Long?>(null) }
    var paiementSupplementaire by remember { mutableStateOf("") }

    var descriptionDepense by remember { mutableStateOf("") }
    var montantDepense by remember { mutableStateOf("") }
    var messageDepense by remember { mutableStateOf("") }

    val chiffreAffaires = ventes.sumOf { it.total }
    val coutMarchandises = ventes.sumOf { it.cout }
    val totalEncaisse = ventes.sumOf { it.paye }
    val totalDettes = ventes.sumOf { it.reste }
    val totalDepenses = depenses.sumOf { it.montant }
    val benefice = chiffreAffaires - coutMarchandises - totalDepenses

    MaterialTheme(
colorScheme = lightColorScheme(
primary = androidx.compose.ui.graphics.Color(0xFF1565C0),
onPrimary = androidx.compose.ui.graphics.Color.White,
secondary = androidx.compose.ui.graphics.Color(0xFF1976D2),
background = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
onBackground = androidx.compose.ui.graphics.Color(0xFF1B1B1F),
surface = androidx.compose.ui.graphics.Color.White,
onSurface = androidx.compose.ui.graphics.Color(0xFF1B1B1F)
)
) {
    if (!utilisateurConnecte) {
        EcranConnexion(
            email = emailConnexion,
            onEmailChange = { emailConnexion = it },
            motDePasse = motDePasseConnexion,
            onMotDePasseChange = { motDePasseConnexion = it },
            modeCreationCompte = modeCreationCompte,
            chargement = chargementConnexion,
            message = messageConnexion,
            onChangerMode = {
                modeCreationCompte = !modeCreationCompte
                messageConnexion = ""
            },
            onValider = {
                if (emailConnexion.isBlank() || motDePasseConnexion.isBlank()) {
                    messageConnexion = "Remplis l'e-mail et le mot de passe."
                } else {
                    chargementConnexion = true
                    messageConnexion = "Connexion en cours..."

                    if (modeCreationCompte) {
                        AuthService.creerCompte(
                            context,
                            emailConnexion.trim(),
                            motDePasseConnexion
                        ) { succes, message ->
                            chargementConnexion = false
                            messageConnexion = message
                            utilisateurConnecte = succes &&
                                AuthService.estConnecte(context)
                        }
                    } else {
                        AuthService.connexion(
                            context,
                            emailConnexion.trim(),
                            motDePasseConnexion
                        ) { succes, message ->
                            chargementConnexion = false
                            messageConnexion = message
                            utilisateurConnecte = succes &&
                                AuthService.estConnecte(context)
                        }
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
                messageConnexion = ""
                emailConnexion = ""
                motDePasseConnexion = ""
                modeCreationCompte = false
            }
        ) {
            Text("Déconnexion")
        }
    }
)
            },
            bottomBar = {
                NavigationBar {
                    listOf("Accueil", "Produits", "Ventes", "Rapports", "Plus")
                        .forEach { onglet ->
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
                when (page) {
                    "Accueil" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    "Bienvenue sur MelmaTech",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text("Gestion de votre commerce en USD")
                            }
                            item {
                                DashboardCard(
                                    "Produits enregistrés",
                                    produits.size.toString()
                                )
                            }
                            item {
                                DashboardCard(
                                    "Chiffre d'affaires",
                                    argent(chiffreAffaires)
                                )
                            }
                            item {
                                DashboardCard("Argent encaissé", argent(totalEncaisse))
                            }
                            item {
                                DashboardCard("Dettes clients", argent(totalDettes))
                            }
                            item {
                                DashboardCard("Dépenses", argent(totalDepenses))
                            }
                            item {
                                DashboardCard("Bénéfice estimé", argent(benefice))
                            }
                            item {
                                Button(
                                    onClick = {
                                        statutConnexion = "Connexion en cours..."

                                        SupabaseService.testerConnexion { succes, message ->
                                            Handler(Looper.getMainLooper()).post {
                                                statutConnexion = message
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tester la connexion Supabase")
                                }
                            }

                            item {
                                Text(
                                    text = statutConnexion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    "Produits" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "Gestion des produits",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                OutlinedTextField(
                                    value = nom,
                                    onValueChange = { nom = it },
                                    label = { Text("Nom du produit") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = achat,
                                    onValueChange = { achat = it },
                                    label = { Text("Prix d'achat (USD)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = prixVente,
                                    onValueChange = { prixVente = it },
                                    label = { Text("Prix de vente (USD)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = quantite,
                                    onValueChange = { quantite = it },
                                    label = { Text("Quantité en stock") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Button(
    onClick = {
        val a = achat.replace(",", ".").toDoubleOrNull()
        val v = prixVente.replace(",", ".").toDoubleOrNull()
        val q = quantite.toIntOrNull()

        if (
            nom.isNotBlank() &&
            a != null && a >= 0 &&
            v != null && v >= 0 &&
            q != null && q >= 0
        ) {
            val nomProduit = nom.trim()
            val id = editionId

            if (id == null) {
                produits.add(
                    Produit(
                        System.currentTimeMillis(),
                        nomProduit, a, v, q
                    )
                )

                sauvegarderProduits()
                messageProduit = "Produit enregistré sur le téléphone. Synchronisation en cours..."

                val accessToken = AuthService.obtenirAccessToken(context)

                if (accessToken.isNullOrBlank()) {
                    messageProduit =
                        "Produit enregistré sur le téléphone. Reconnecte-toi pour synchroniser."
                } else {
                    AuthService.obtenirEntrepriseId(context) { succes, message, entrepriseId ->
                        if (succes && entrepriseId != null) {
                            SupabaseService.ajouterProduit(
                                accessToken,
                                entrepriseId,
                                nomProduit,
                                a,
                                v,
                                q
                            ) { enregistre, resultat ->
                                messageProduit = if (enregistre) {
                                    "Produit enregistré et synchronisé avec Supabase."
                                } else {
                                    "Produit enregistré sur le téléphone, mais échec Supabase : $resultat"
                                }
                            }
                        } else {
                            messageProduit =
                                "Produit enregistré sur le téléphone, mais synchronisation impossible : $message"
                        }
                    }
                }
            } else {
                val index = produits.indexOfFirst {
                    it.id == id
                }

                if (index >= 0) {
                    produits[index] = Produit(
                        id, nomProduit, a, v, q
                    )
                }

                sauvegarderProduits()
                messageProduit =
                    "Modification enregistrée sur le téléphone. La synchronisation des modifications n'est pas encore activée."
            }

            nom = ""
            achat = ""
            prixVente = ""
            quantite = ""
            editionId = null
        } else {
            messageProduit =
                "Vérifie le nom, les prix et la quantité."
        }
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text(
        if (editionId == null) "Ajouter le produit"
        else "Enregistrer les modifications"
    )
}
                                if (messageProduit.isNotBlank()) {
                                    Text(messageProduit)
                                }

                                if (editionId != null) {
                                    TextButton(onClick = {
                                        editionId = null
                                        nom = ""
                                        achat = ""
                                        prixVente = ""
                                        quantite = ""
                                    }) {
                                        Text("Annuler la modification")
                                    }
                                }

                                HorizontalDivider()
                                Text("Liste des produits (${produits.size})")
                            }

                            items(produits.toList(), key = { it.id }) { p ->
                                Card {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            p.nom,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text("Achat : ${argent(p.achat)}")
                                        Text("Vente : ${argent(p.vente)}")
                                        Text("Stock : ${p.quantite}")
                                        Text(
                                            "Bénéfice unitaire : " +
                                                argent(p.vente - p.achat)
                                        )
                                        Row {
                                            TextButton(onClick = {
                                                editionId = p.id
                                                nom = p.nom
                                                achat = p.achat.toString()
                                                prixVente = p.vente.toString()
                                                quantite = p.quantite.toString()
                                            }) {
                                                Text("Modifier")
                                            }
                                            TextButton(onClick = {
                                                produits.removeAll { it.id == p.id }
                                                sauvegarderProduits()
                                                if (editionId == p.id) {
                                                    editionId = null
                                                    nom = ""
                                                    achat = ""
                                                    prixVente = ""
                                                    quantite = ""
                                                }
                                            }) {
                                                Text("Supprimer")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Ventes" -> {
                        val suggestions = produits.filter {
                            recherche.isNotBlank() &&
                                it.nom.contains(recherche.trim(), ignoreCase = true) &&
                                it.quantite > 0
                        }.take(6)

                        val totalPanier = panier.sumOf { it.total }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = recherche,
                                    onValueChange = {
                                        recherche = it
                                        produitChoisi = null
                                    },
                                    label = { Text("Rechercher un produit") },
                                    placeholder = {
                                        Text("Tape les premières lettres...")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                if (recherche.isNotBlank() && suggestions.isEmpty()) {
                                    Text("Aucun produit disponible trouvé.")
                                }
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
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            p.nom,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "${argent(p.vente)} par unité — Stock : ${p.quantite}"
                                        )
                                    }
                                }
                            }

                            item {
                                produitChoisi?.let { p ->
                                    Text("Produit choisi : ${p.nom}")
                                    OutlinedTextField(
                                        value = quantiteSaisie,
                                        onValueChange = { quantiteSaisie = it },
                                        label = { Text("Quantité") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val q = quantiteSaisie.toIntOrNull()
                                            val dansPanier = panier
                                                .filter { it.produitId == p.id }
                                                .sumOf { it.quantite }

                                            if (
                                                q != null && q > 0 &&
                                                q + dansPanier <= p.quantite
                                            ) {
                                                val existant = panier.indexOfFirst {
                                                    it.produitId == p.id
                                                }
                                                panier = if (existant >= 0) {
                                                    panier.mapIndexed { index, ligne ->
                                                        if (index == existant) {
                                                            ligne.copy(
                                                                quantite = ligne.quantite + q
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

                                                recherche = ""
                                                produitChoisi = null
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
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            ligne.nom,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text("Quantité : ${ligne.quantite}")
                                        Text("Sous-total : ${argent(ligne.total)}")
                                        TextButton(onClick = {
                                            panier = panier.filter {
                                                it.produitId != ligne.produitId
                                            }
                                        }) {
                                            Text("Retirer du panier")
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "TOTAL : ${argent(totalPanier)}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                OutlinedTextField(
                                    value = paiement,
                                    onValueChange = { paiement = it },
                                    label = { Text("Montant payé maintenant (USD)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
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
                                                val actuel = produits.find {
                                                    it.id == ligne.produitId
                                                }
                                                actuel != null &&
                                                    ligne.quantite <= actuel.quantite
                                            }

                                            if (!stockOK) {
                                                messageVente =
                                                    "Stock insuffisant. Vérifie les produits."
                                            } else {
                                                val date = System.currentTimeMillis()
                                                val nouvelleVente = Vente(
                                                    date,
                                                    client.trim(),
                                                    panier.toList(),
                                                    montantPaye,
                                                    date
                                                )

                                                panier.forEach { ligne ->
                                                    val index = produits.indexOfFirst {
                                                        it.id == ligne.produitId
                                                    }
                                                    if (index >= 0) {
                                                        val p = produits[index]
                                                        produits[index] = p.copy(
                                                            quantite = p.quantite - ligne.quantite
                                                        )
                                                    }
                                                }

                                                ventes.add(nouvelleVente)
                                                sauvegarderProduits()
                                                sauvegarderVentes()

                                                messageVente =
                                                    "Vente enregistrée. Facture n° $date"
                                                panier = emptyList()
                                                client = ""
                                                recherche = ""
                                                produitChoisi = null
                                                paiement = ""
                                                quantiteSaisie = "1"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Valider la vente")
                                }

                                if (messageVente.isNotBlank()) {
                                    Text(messageVente)
                                }

                                HorizontalDivider()
                                Text(
                                    "Historique des ventes",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            items(ventes.toList().reversed(), key = { it.id }) { v ->
                                Card {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "Facture n° ${v.id}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text("Client : ${v.client}")
                                        v.lignes.forEach { ligne ->
                                            Text(
                                                "${ligne.nom} × ${ligne.quantite} = " +
                                                    argent(ligne.total)
                                            )
                                        }
                                        Text("Total : ${argent(v.total)}")
                                        Text("Payé : ${argent(v.paye)}")
                                        Text("Reste à payer : ${argent(v.reste)}")

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
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Decimal
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Button(onClick = {
                                                    val m = paiementSupplementaire
                                                        .replace(",", ".")
                                                        .toDoubleOrNull()

                                                    if (m != null && m > 0 && m <= v.reste) {
                                                        val index = ventes.indexOfFirst {
                                                            it.id == v.id
                                                        }
                                                        if (index >= 0) {
                                                            ventes[index] = v.copy(
                                                                paye = v.paye + m
                                                            )
                                                            sauvegarderVentes()
                                                        }
                                                        paiementId = null
                                                        paiementSupplementaire = ""
                                                        messageVente =
                                                            "Paiement supplémentaire enregistré."
                                                    } else {
                                                        messageVente =
                                                            "Montant invalide."
                                                    }
                                                }) {
                                                    Text("Enregistrer le paiement")
                                                }
                                            } else {
                                                TextButton(onClick = {
                                                    paiementId = v.id
                                                    paiementSupplementaire = ""
                                                }) {
                                                    Text("Ajouter un paiement")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Plus" -> {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Plus",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Autres fonctions de MelmaTech",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Card(
                onClick = { page = "Rapports" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "💸 Dépenses",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Consulter et enregistrer les dépenses")
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
                    Text("Accéder aux ventes et à leur historique")
                }
            }
        }

        item {
    Card(
        onClick = { page = "Utilisateurs" },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "👥 Gestion des utilisateurs",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Créer et gérer les comptes des vendeurs."
            )

            Text(
                "Accès réservé à l'administrateur",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "⚙️ Paramètres",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Configuration de l'entreprise : à développer")
                }
            }
        }
    }
                    }
                    "Utilisateurs" -> {
    var nomVendeur by remember { mutableStateOf("") }
    var emailVendeur by remember { mutableStateOf("") }
    var motDePasseVendeur by remember { mutableStateOf("") }
    var confirmationVendeur by remember { mutableStateOf("") }
    var chargementVendeur by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Gestion des utilisateurs",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Créez les comptes des vendeurs de votre entreprise.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Créer un compte vendeur",
                        style = MaterialTheme.typography.titleLarge
                    )

                    OutlinedTextField(
                        value = nomVendeur,
                        onValueChange = { nomVendeur = it },
                        label = { Text("Nom complet du vendeur") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !chargementVendeur
                    )

                    OutlinedTextField(
                        value = emailVendeur,
                        onValueChange = { emailVendeur = it },
                        label = { Text("Adresse e-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !chargementVendeur
                    )

                    OutlinedTextField(
                        value = motDePasseVendeur,
                        onValueChange = { motDePasseVendeur = it },
                        label = { Text("Mot de passe temporaire") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        enabled = !chargementVendeur
                    )

                    Button(
                        onClick = {
                            val nom = nomVendeur.trim()
                            val email = emailVendeur.trim()

                            if (
                                nom.isBlank() ||
                                email.isBlank() ||
                                !email.contains("@") ||
                                motDePasseVendeur.length < 8
                            ) {
                                confirmationVendeur =
                                    "Saisis un nom, un e-mail valide et un mot de passe d'au moins 8 caractères."
                            } else {
                                chargementVendeur = true
                                confirmationVendeur = ""

                                AuthService.creerVendeur(
                                    context = context,
                                    nomComplet = nom,
                                    email = email,
                                    motDePasse = motDePasseVendeur
                                ) { succes, message ->
                                    chargementVendeur = false
                                    confirmationVendeur = message

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

                    if (confirmationVendeur.isNotBlank()) {
                        Text(
                            text = confirmationVendeur,
                            color = if (
                                confirmationVendeur ==
                                "Compte vendeur créé avec succès."
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Administrateur",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "L'administrateur actif peut créer les comptes vendeurs."
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Vendeur",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Le vendeur ne peut pas créer d'autres comptes ni gérer les produits sans autorisation."
                    )
                }
            }
        }
    }
}
                    "Rapports" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    "Rapports du commerce",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                DashboardCard(
                                    "Chiffre d'affaires",
                                    argent(chiffreAffaires)
                                )
                                DashboardCard("Montant encaissé", argent(totalEncaisse))
                                DashboardCard("Dettes clients", argent(totalDettes))
                                DashboardCard("Coût des marchandises vendues", argent(coutMarchandises))
                                DashboardCard("Dépenses", argent(totalDepenses))
                                DashboardCard("Bénéfice estimé", argent(benefice))
                                HorizontalDivider()
                                Text(
                                    "Nouvelle dépense",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                OutlinedTextField(
                                    value = descriptionDepense,
                                    onValueChange = { descriptionDepense = it },
                                    label = { Text("Description de la dépense") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = montantDepense,
                                    onValueChange = { montantDepense = it },
                                    label = { Text("Montant (USD)") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val m = montantDepense
                                            .replace(",", ".")
                                            .toDoubleOrNull()
                                        if (
                                            descriptionDepense.isNotBlank() &&
                                            m != null && m > 0
                                        ) {
                                            depenses.add(
                                                Depense(
                                                    System.currentTimeMillis(),
                                                    descriptionDepense.trim(),
                                                    m,
                                                    System.currentTimeMillis()
                                                )
                                            )
                                            sauvegarderDepenses()
                                            descriptionDepense = ""
                                            montantDepense = ""
                                            messageDepense = "Dépense enregistrée."
                                        } else {
                                            messageDepense =
                                                "Vérifie la description et le montant."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Enregistrer la dépense")
                                }
                                if (messageDepense.isNotBlank()) {
                                    Text(messageDepense)
                                }
                                HorizontalDivider()
                                Text(
                                    "Historique des dépenses",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            items(depenses.toList().reversed(), key = { it.id }) { d ->
                                Card {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(d.description)
                                        Text(argent(d.montant))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Cette fonction doit être déclarée en dehors de MelmaTechApp.
@Composable
fun DashboardCard(titre: String, montant: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(montant, style = MaterialTheme.typography.headlineMedium)
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
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = "MelmaTech",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = if (modeCreationCompte) "Créer un compte" else "Connexion",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Adresse e-mail") },
            singleLine = true,
            enabled = !chargement
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = motDePasse,
            onValueChange = onMotDePasseChange,
            label = { Text("Mot de passe") },
            singleLine = true,
            enabled = !chargement,
            visualTransformation =
                androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message)
        }
    }
}
