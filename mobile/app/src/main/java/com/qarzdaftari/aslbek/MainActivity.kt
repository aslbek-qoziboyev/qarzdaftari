package com.qarzdaftari.aslbek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.qarzdaftari.aslbek.data.db.DebtDatabaseHelper
import com.qarzdaftari.aslbek.data.model.User
import com.qarzdaftari.aslbek.data.session.SessionManager
import com.qarzdaftari.aslbek.ui.screens.AuthScreen
import com.qarzdaftari.aslbek.ui.screens.HomeScreen
import com.qarzdaftari.aslbek.ui.theme.QarzDaftariTheme

class MainActivity : ComponentActivity() {
    private val dbHelper by lazy { DebtDatabaseHelper(applicationContext) }
    private val sessionManager by lazy { SessionManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QarzDaftariTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentUser by remember { mutableStateOf<User?>(sessionManager.getUser()) }

                    val activeUser = currentUser
                    if (activeUser == null) {
                        AuthScreen(
                            dbHelper = dbHelper,
                            sessionManager = sessionManager,
                            onAuthSuccess = { user ->
                                currentUser = user
                            }
                        )
                    } else {
                        HomeScreen(
                            currentUser = activeUser,
                            dbHelper = dbHelper,
                            onLogout = {
                                sessionManager.logout()
                                currentUser = null
                            }
                        )
                    }
                }
            }
        }
    }
}
