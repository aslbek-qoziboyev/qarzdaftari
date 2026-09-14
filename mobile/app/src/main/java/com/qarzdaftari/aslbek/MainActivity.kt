package com.qarzdaftari.aslbek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable

private const val SUPABASE_URL = "https://dbhbdhub.supabase.co"
private const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"

@Serializable
data class Debt(
    val id: String,
    val user_id: String,
    val name: String,
    val amount: Double,
    val returned: Double,
    val direction: String,
    val created_at: String? = null
)

class MainActivity : ComponentActivity() {
    private val supabase: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.data != null) handleDeepLink(intent)
        setContent { QarzDaftariApp(supabase) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        // OAuth callback: qarzdaftari://auth/callback
        // Supabase Auth processes the returned session/deep link.
    }
}

@Composable
fun QarzDaftariApp(sb: SupabaseClient) {
    var loggedIn by remember { mutableStateOf(sb.auth.currentSessionOrNull() != null) }

    MaterialTheme {
        if (!loggedIn) {
            LoginScreen {
                // Google OAuth is handled by Supabase Auth.
                sb.auth.signInWith(Google) {
                    scopes.add("email")
                    queryParams["redirect_to"] = "qarzdaftari://auth/callback"
                }
                loggedIn = true
            }
        } else {
            HomeScreen(sb) {
                sb.auth.signOut()
                loggedIn = false
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Q", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))
            Text("Qarz Daftari", style = MaterialTheme.typography.headlineLarge)
            Text("Qarzlaringizni tartibli va xavfsiz boshqaring.")
            Spacer(Modifier.height(28.dp))
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Qarz Daftariga kirish")
            }
            Spacer(Modifier.height(10.dp))
            Text("Google orqali xavfsiz kirish")
        }
    }
}

@Composable
fun HomeScreen(sb: SupabaseClient, onLogout: () -> Unit) {
    var debts by remember { mutableStateOf<List<Debt>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching {
            debts = sb.from("debts").select().decodeList<Debt>()
        }
    }

    val incoming = debts.filter { it.direction == "received" }
    val outgoing = debts.filter { it.direction == "given" }
    val visible = when (tab) {
        1 -> outgoing
        2 -> incoming
        else -> debts
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, label = { Text("Asosiy") }, icon = {})
                NavigationBarItem(tab == 1, { tab = 1 }, label = { Text("Qarzdorman") }, icon = {})
                NavigationBarItem(tab == 2, { tab = 2 }, label = { Text("Qarzdorlar") }, icon = {})
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(18.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("QARZ DAFTARI")
                        Text(
                            if (tab == 0) "Boshqaruv paneli"
                            else if (tab == 1) "Qarzdorman"
                            else "Qarzdorlar",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    TextButton(onClick = onLogout) { Text("Chiqish") }
                }

                Spacer(Modifier.height(14.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Umumiy qarzlar")
                        Text(formatMoney(debts.sumOf { it.amount }))
                        Text(debts.size.toString() + " ta yozuv")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Summary("Olgan qarzlar", formatMoney(incoming.sumOf { it.amount }), incoming.size)
                Summary("Bergan qarzlar", formatMoney(outgoing.sumOf { it.amount }), outgoing.size)
                Spacer(Modifier.height(12.dp))
            }

            items(visible) { debt -> DebtRow(debt) }
        }
    }
}

@Composable
fun Summary(title: String, value: String, count: Int) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(count.toString() + " ta")
        }
    }
}

@Composable
fun DebtRow(debt: Debt) {
    val remaining = (debt.amount - debt.returned).coerceAtLeast(0.0)
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(debt.name.take(1).uppercase())
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(debt.name)
                Text(if (debt.direction == "received") "Men qarzdorman" else "Menga qarzdor")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(remaining))
                Text("qoldiq")
            }
        }
    }
}

fun formatMoney(value: Double): String {
    return String.format("%,.0f", value).replace(",", " ") + " so'm"
}
