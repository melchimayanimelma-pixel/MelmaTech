package com.melmatech.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContent {
MelmaTechApp()
}
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelmaTechApp() {
var page by remember { mutableStateOf("Accueil") }

MaterialTheme {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MelmaTech") }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Accueil", "Produits", "Ventes", "Rapports").forEach {
                    NavigationBarItem(
                        selected = page == it,
                        onClick = { page = it },
                        icon = {},
                        label = { Text(it) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Bienvenue sur MelmaTech",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Gestion de votre commerce en dollars USD")

            when (page) {
                "Accueil" -> {
                    DashboardCard("Ventes du jour", "$0.00")
                    DashboardCard("Dépenses", "$0.00")
                    DashboardCard("Bénéfice estimé", "$0.00")
                }

                "Produits" -> Text("Gestion des produits")
                "Ventes" -> Text("Enregistrement des ventes")
                "Rapports" -> Text("Rapports du commerce")
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