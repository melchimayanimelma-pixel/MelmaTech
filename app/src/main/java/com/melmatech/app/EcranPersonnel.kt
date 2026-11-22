package com.melmatech.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

private data class EmployeListe(
    val id: String,
    val matricule: String,
    val nom: String,
    val poste: String,
    val departement: String,
    val statut: String
)

@Composable
fun EcranPersonnel(onRetour: () -> Unit) {
    val context = LocalContext.current
    var employes by remember { mutableStateOf(emptyList<EmployeListe>()) }
    var chargement by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    fun chargerEmployes() {
        chargement = true
        message = ""
        AuthService.obtenirProfil(context) { okProfil, texteProfil, profil ->
            if (!okProfil || profil == null) {
                chargement = false
                message = texteProfil
                return@obtenirProfil
            }
            val token = AuthService.obtenirAccessToken(context)
            if (token.isNullOrBlank()) {
                chargement = false
                message = "Session expirée. Reconnecte-toi."
                return@obtenirProfil
            }
            val url = "${SupabaseConfig.url}/rest/v1/employes" +
                "?entreprise_id=eq.${profil.entrepriseId}" +
                "&select=id,matricule,nom,postnom,prenom,poste,departement,statut" +
                "&order=nom.asc"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.key)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        chargement = false
                        message = "Erreur réseau : ${e.message ?: "connexion impossible"}"
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            chargement = false
                            if (!it.isSuccessful) {
                                message = "Impossible de charger le personnel (${it.code})."
                            } else {
                                try {
                                    val array = JSONArray(body)
                                    employes = (0 until array.length()).map { index ->
                                        val item = array.getJSONObject(index)
                                        EmployeListe(
                                            id = item.optString("id"),
                                            matricule = item.optString("matricule"),
                                            nom = listOf(
                                                item.optString("prenom"),
                                                item.optString("nom"),
                                                item.optString("postnom")
                                            ).filter { part -> part.isNotBlank() && part != "null" }
                                                .joinToString(" "),
                                            poste = item.optString("poste").takeUnless { value -> value == "null" }.orEmpty(),
                                            departement = item.optString("departement").takeUnless { value -> value == "null" }.orEmpty(),
                                            statut = item.optString("statut", "actif")
                                        )
                                    }
                                    message = if (employes.isEmpty()) "Aucun employé enregistré pour cette entreprise." else ""
                                } catch (_: Exception) {
                                    message = "La réponse du serveur est illisible."
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    LaunchedEffect(Unit) { chargerEmployes() }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F3FA)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ressources humaines", color = Color(0xFF6C3CE9), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Personnel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onRetour) { Text("Retour") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Employés : ${employes.size}", fontWeight = FontWeight.SemiBold)
            Button(onClick = { chargerEmployes() }, enabled = !chargement) { Text("Actualiser") }
        }
        if (chargement) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chargement du personnel…")
            }
        } else if (employes.isEmpty()) {
            Text(message.ifBlank { "Aucun employé à afficher." }, color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(employes, key = { it.id }) { employe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(employe.nom.ifBlank { "Employé sans nom" }, fontWeight = FontWeight.Bold)
                            Text("Matricule : ${employe.matricule.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("Poste : ${employe.poste.ifBlank { "Non défini" }}", style = MaterialTheme.typography.bodyMedium)
                            if (employe.departement.isNotBlank()) Text("Département : ${employe.departement}", style = MaterialTheme.typography.bodySmall)
                            Text("Statut : ${employe.statut}", color = if (employe.statut == "actif") Color(0xFF218653) else Color(0xFFB45309), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        if (message.isNotBlank() && employes.isNotEmpty()) Text(message, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}
