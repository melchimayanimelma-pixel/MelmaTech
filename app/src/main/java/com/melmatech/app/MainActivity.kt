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

data class Produit(
    val id: Long,
    val nom: String,
    val achat: Double,
    val vente: Double,
    val quantite: Int
)

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
    val preferences = remember {
        context.getSharedPreferences("melmatech", Context.MODE_PRIVATE)
    }

    val produits = remember {
        mutableStateListOf<Produit>().apply {
            try {
                val json = JSONArray(
                    preferences.getString("produits", "[]")
                )
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
            } catch (_: Exception) {
            }
        }
    }

    fun sauvegarder() {
        val json = JSONArray()
        produits.forEach { p ->
            json.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("nom", p.nom)
                    put("achat", p.achat)
                    put("vente", p.vente)
                    put("quantite", p.quantite)
                }
            )
        }
        preferences.edit().putString("produits", json.toString()).apply()
    }

    var page by remember { mutableStateOf("Accueil") }
    var nom by remember { mutableStateOf("") }
    var achat by remember { mutableStateOf("") }
    var vente by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var editionId by remember { mutableStateOf<Long?>(null) }

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (page) {
                    "Accueil" -> {
                        Text(
                            "Bienvenue sur MelmaTech",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text("Gestion de votre commerce en USD")
                        DashboardCard(
                            "Produits enregistrés",
                            produits.size.toString()
                        )
                        DashboardCard(
                            "Valeur du stock au prix d'achat",
                            "$" + "%.2f".format(
                                produits.sumOf {
                                    it.achat * it.quantite
                                }
                            )
                        )
                    }

                    "Produits" -> {
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
                            value = vente,
                            onValueChange = { vente = it },
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
                                val v = vente.replace(",", ".").toDoubleOrNull()
                                val q = quantite.toIntOrNull()

                                if (
                                    nom.isNotBlank() &&
                                    a != null && a >= 0 &&
                                    v != null && v >= 0 &&
                                    q != null && q >= 0
                                ) {
                                    val id = editionId
                                    if (id == null) {
                                        produits.add(
                                            Produit(
                                                System.currentTimeMillis(),
                                                nom.trim(), a, v, q
                                            )
                                        )
                                    } else {
                                        val index = produits.indexOfFirst {
                                            it.id == id
                                        }
                                        if (index >= 0) {
                                            produits[index] = Produit(
                                                id, nom.trim(), a, v, q
                                            )
                                        }
                                    }
                                    sauvegarder()
                                    nom = ""
                                    achat = ""
                                    vente = ""
                                    quantite = ""
                                    editionId = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (editionId == null)
                                    "Ajouter le produit"
                                else "Enregistrer les modifications"
                            )
                        }

                        if (editionId != null) {
                            TextButton(
                                onClick = {
                                    editionId = null
                                    nom = ""
                                    achat = ""
                                    vente = ""
                                    quantite = ""
                                }
                            ) {
                                Text("Annuler la modification")
                            }
                        }

                        HorizontalDivider()

                        Text(
                            "Liste des produits (${produits.size})",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (produits.isEmpty()) {
                            Text("Aucun produit enregistré.")
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = produits.toList(),
                                key = { it.id }
                            ) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement =
                                            Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            p.nom,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text("Achat : $${"%.2f".format(p.achat)}")
                                        Text("Vente : $${"%.2f".format(p.vente)}")
                                        Text("Stock : ${p.quantite}")
                                        Text(
                                            "Bénéfice unitaire : " +
                                                "$${"%.2f".format(p.vente - p.achat)}"
                                        )

                                        Row {
                                            TextButton(
                                                onClick = {
                                                    editionId = p.id
                                                    nom = p.nom
                                                    achat = p.achat.toString()
                                                    vente = p.vente.toString()
                                                    quantite = p.quantite.toString()
                                                }
                                            ) {
                                                Text("Modifier")
                                            }

                                            TextButton(
                                                onClick = {
                                                    produits.removeAll {
                                                        it.id == p.id
                                                    }
                                                    sauvegarder()
                                                    if (editionId == p.id) {
                                                        editionId = null
                                                        nom = ""
                                                        achat = ""
                                                        vente = ""
                                                        quantite = ""
                                                    }
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

                    "Ventes" -> {
                        Text(
                            "Ventes",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Cette rubrique sera développée après la gestion des produits."
                        )
                    }

                    "Rapports" -> {
                        Text(
                            "Rapports",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text("Nombre de produits : ${produits.size}")
                        Text(
                            "Unités en stock : " +
                                produits.sumOf { it.quantite }
                        )
                        Text(
                            "Valeur du stock : " +
                                "$${"%.2f".format(
                                    produits.sumOf {
                                        it.achat * it.quantite
                                    }
                                )}"
                        )
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