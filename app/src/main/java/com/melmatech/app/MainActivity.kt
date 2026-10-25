package com.melmatech.app

import android.os.Bundle
import android.content.Context
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
    val quantite: Int
)

data class Vente(
    val id: Long,
    val client: String,
    val produitId: Long,
    val produitNom: String,
    val quantite: Int,
    val total: Double,
    val paye: Double,
    val date: Long
) {
    val reste: Double get() = (total - paye).coerceAtLeast(0.0)
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
        setContent { MelmaTechApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelmaTechApp(context: Context) {
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
                val json = JSONArray(prefs.getString("ventes", "[]"))
                for (i in 0 until json.length()) {
                    val v = json.getJSONObject(i)
                    add(
                        Vente(
                            v.getLong("id"),
                            v.getString("client"),
                            v.getLong("produitId"),
                            v.getString("produitNom"),
                            v.getInt("quantite"),
                            v.getDouble("total"),
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
            json.put(JSONObject().apply {
                put("id", v.id)
                put("client", v.client)
                put("produitId", v.produitId)
                put("produitNom", v.produitNom)
                put("quantite", v.quantite)
                put("total", v.total)
                put("paye", v.paye)
                put("date", v.date)
            })
        }
        prefs.edit().putString("ventes", json.toString()).apply()
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

    // Formulaire produit
    var nom by remember { mutableStateOf("") }
    var achat by remember { mutableStateOf("") }
    var ventePrix by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var editionId by remember { mutableStateOf<Long?>(null) }

    // Formulaire vente
    var client by remember { mutableStateOf("") }
    var produitChoisi by remember { mutableStateOf<Long?>(null) }
    var quantiteVente by remember { mutableStateOf("1") }
    var montantPaye by remember { mutableStateOf("") }
    var messageVente by remember { mutableStateOf("") }

    // Paiement d'une dette
    var ventePaiementId by remember { mutableStateOf<Long?>(null) }
    var paiementSupplementaire by remember { mutableStateOf("") }

    // Formulaire dépense
    var descriptionDepense by remember { mutableStateOf("") }
    var montantDepense by remember { mutableStateOf("") }
    var messageDepense by remember { mutableStateOf("") }

    val chiffreAffaires = ventes.sumOf { it.total }
    val coutMarchandises = ventes.sumOf { v ->
        val p = produits.find { it.id == v.produitId }
        (p?.achat ?: 0.0) * v.quantite
    }
    val totalDepenses = depenses.sumOf { it.montant }
    val totalDettes = ventes.sumOf { it.reste }
    val totalEncaisse = ventes.sumOf { it.paye }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("MelmaTech") })
            },
            bottomBar = {
                NavigationBar {
                    listOf("Accueil", "Produits", "Ventes", "Rapports")
                        .forEach { onglet ->
                            NavigationBarItem(
                                selected = page == onglet,
                                onClick = {
                                    page = onglet
                                    messageVente = ""
                                    messageDepense = ""
                                },
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
                            item { DashboardCard("Produits enregistrés", produits.size.toString()) }
                            item { DashboardCard("Ventes totales", argent(chiffreAffaires)) }
                            item { DashboardCard("Argent encaissé", argent(totalEncaisse)) }
                            item { DashboardCard("Dettes clients", argent(totalDettes)) }
                            item { DashboardCard("Dépenses", argent(totalDepenses)) }
                            item {
                                DashboardCard(
                                    "Bénéfice estimé",
                                    argent(chiffreAffaires - coutMarchandises - totalDepenses)
                                )
                            }
                        }
                    }

                    "Produits" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text("Gestion des produits", style = MaterialTheme.typography.headlineSmall)
                                OutlinedTextField(
                                    value = nom,
                                    onValueChange = { nom = it },
                                    label = { Text("Nom du produit") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = achat,
                                    onValueChange = { achat = it },
                                    label = { Text("Prix d'achat (USD)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = ventePrix,
                                    onValueChange = { ventePrix = it },
                                    label = { Text("Prix de vente (USD)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = quantite,
                                    onValueChange = { quantite = it },
                                    label = { Text("Quantité en stock") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val a = achat.replace(",", ".").toDoubleOrNull()
                                        val v = ventePrix.replace(",", ".").toDoubleOrNull()
                                        val q = quantite.toIntOrNull()
                                        if (nom.isNotBlank() && a != null && a >= 0 &&
                                            v != null && v >= 0 && q != null && q >= 0
                                        ) {
                                            val id = editionId
                                            if (id == null) {
                                                produits.add(
                                                    Produit(System.currentTimeMillis(), nom.trim(), a, v, q)
                                                )
                                            } else {
                                                val index = produits.indexOfFirst { it.id == id }
                                                if (index >= 0) {
                                                    produits[index] = Produit(id, nom.trim(), a, v, q)
                                                }
                                            }
                                            sauvegarderProduits()
                                            nom = ""
                                            achat = ""
                                            ventePrix = ""
                                            quantite = ""
                                            editionId = null
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (editionId == null) "Ajouter le produit" else "Enregistrer les modifications")
                                }
                                if (editionId != null) {
                                    TextButton(onClick = {
                                        editionId = null
                                        nom = ""; achat = ""; ventePrix = ""; quantite = ""
                                    }) { Text("Annuler") }
                                }
                                HorizontalDivider()
                                Text("Liste des produits (${produits.size})")
                            }

                            items(produits.toList(), key = { it.id }) { p ->
                                Card {
                                    Column(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(p.nom, style = MaterialTheme.typography.titleMedium)
                                        Text("Achat : ${argent(p.achat)} | Vente : ${argent(p.vente)}")
                                        Text("Stock : ${p.quantite}")
                                        Row {
                                            TextButton(onClick = {
                                                editionId = p.id
                                                nom = p.nom
                                                achat = p.achat.toString()
                                                ventePrix = p.vente.toString()
                                                quantite = p.quantite.toString()
                                            }) { Text("Modifier") }

                                            TextButton(onClick = {
                                                produits.removeAll { it.id == p.id }
                                                sauvegarderProduits()
                                            }) { Text("Supprimer") }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Ventes" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text("Nouvelle vente", style = MaterialTheme.typography.headlineSmall)
                                OutlinedTextField(
                                    value = client,
                                    onValueChange = { client = it },
                                    label = { Text("Nom du client") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Choisir un produit :")
                            }

                            items(produits.toList(), key = { it.id }) { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.nom)
                                        Text("${argent(p.vente)} — Stock : ${p.quantite}")
                                    }
                                    RadioButton(
                                        selected = produitChoisi == p.id,
                                        onClick = { produitChoisi = p.id }
                                    )
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = quantiteVente,
                                    onValueChange = { quantiteVente = it },
                                    label = { Text("Quantité vendue") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val p = produits.find { it.id == produitChoisi }
                                val q = quantiteVente.toIntOrNull() ?: 0
                                val total = (p?.vente ?: 0.0) * q

                                Text("Montant total : ${argent(total)}")
                                OutlinedTextField(
                                    value = montantPaye,
                                    onValueChange = { montantPaye = it },
                                    label = { Text("Montant payé maintenant (USD)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val produit = produits.find { it.id == produitChoisi }
                                        val qte = quantiteVente.toIntOrNull()
                                        val paye = montantPaye.replace(",", ".").toDoubleOrNull()

                                        if (produit != null && qte != null && qte > 0 &&
                                            qte <= produit.quantite && client.isNotBlank() &&
                                            paye != null && paye >= 0 &&
                                            paye <= produit.vente * qte
                                        ) {
                                            val montantTotal = produit.vente * qte
                                            ventes.add(
                                                Vente(
                                                    System.currentTimeMillis(),
                                                    client.trim(),
                                                    produit.id,
                                                    produit.nom,
                                                    qte,
                                                    montantTotal,
                                                    paye,
                                                    System.currentTimeMillis()
                                                )
                                            )
                                            val index = produits.indexOfFirst { it.id == produit.id }
                                            produits[index] = produit.copy(quantite = produit.quantite - qte)
                                            sauvegarderProduits()
                                            sauvegarderVentes()
                                            messageVente = "Vente enregistrée. Facture n° ${ventes.last().id}"
                                            client = ""
                                            quantiteVente = "1"
                                            montantPaye = ""
                                            produitChoisi = null
                                        } else {
                                            messageVente = "Vérifie le client, la quantité, le stock et le paiement."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Valider la vente") }

                                if (messageVente.isNotBlank()) Text(messageVente)

                                HorizontalDivider()
                                Text("Historique des ventes", style = MaterialTheme.typography.titleMedium)
                            }

                            items(ventes.toList().reversed(), key = { it.id }) { v ->
                                Card {
                                    Column(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Facture n° ${v.id}")
                                        Text("Client : ${v.client}")
                                        Text("${v.produitNom} × ${v.quantite}")
                                        Text("Total : ${argent(v.total)}")
                                        Text("Payé : ${argent(v.paye)}")
                                        Text("Reste : ${argent(v.reste)}")

                                        if (v.reste > 0) {
                                            if (ventePaiementId == v.id) {
                                                OutlinedTextField(
                                                    value = paiementSupplementaire,
                                                    onValueChange = { paiementSupplementaire = it },
                                                    label = { Text("Paiement supplémentaire") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Button(onClick = {
                                                    val montant = paiementSupplementaire.replace(",", ".").toDoubleOrNull()
                                                    if (montant != null && montant > 0 && montant <= v.reste) {
                                                        val index = ventes.indexOfFirst { it.id == v.id }
                                                        ventes[index] = v.copy(paye = v.paye + montant)
                                                        sauvegarderVentes()
                                                        ventePaiementId = null
                                                        paiementSupplementaire = ""
                                                        messageVente = "Paiement enregistré."
                                                    } else {
                                                        messageVente = "Montant invalide."
                                                    }
                                                }) { Text("Enregistrer le paiement") }
                                            } else {
                                                TextButton(onClick = {
                                                    ventePaiementId = v.id
                                                    paiementSupplementaire = ""
                                                }) { Text("Ajouter un paiement") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Rapports" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Text("Rapports du commerce", style = MaterialTheme.typography.headlineSmall)
                                DashboardCard("Nombre de produits", produits.size.toString())
                                DashboardCard("Unités en stock", produits.sumOf { it.quantite }.toString())
                                DashboardCard("Ventes totales", argent(chiffreAffaires))
                                DashboardCard("Montant encaissé", argent(totalEncaisse))
                                DashboardCard("Dettes clients", argent(totalDettes))
                                DashboardCard("Dépenses", argent(totalDepenses))
                                DashboardCard(
                                    "Bénéfice estimé",
                                    argent(chiffreAffaires - coutMarchandises - totalDepenses)
                                )
                                HorizontalDivider()
                                Text("Nouvelle dépense", style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = descriptionDepense,
                                    onValueChange = { descriptionDepense = it },
                                    label = { Text("Description (loyer, transport...)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = montantDepense,
                                    onValueChange = { montantDepense = it },
                                    label = { Text("Montant (USD)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val m = montantDepense.replace(",", ".").toDoubleOrNull()
                                        if (descriptionDepense.isNotBlank() && m != null && m > 0) {
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
                                            messageDepense = "Vérifie la description et le montant."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Enregistrer la dépense") }
                                if (messageDepense.isNotBlank()) Text(messageDepense)
                                HorizontalDivider()
                                Text("Historique des dépenses", style = MaterialTheme.typography.titleMedium)
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

@Composable
fun DashboardCard(titre: String, montant: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(montant, style = MaterialTheme.typography.headlineMedium)
        }
    }
}