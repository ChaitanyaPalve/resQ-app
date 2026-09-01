package com.phoenix.phoenixnet.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoenix.phoenixnet.ui.theme.LocalPhoenixThemeState

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val themeState = LocalPhoenixThemeState.current
    var isLoginMode by remember { mutableStateOf(true) }
    val authResult by viewModel.authState.collectAsState()

    LaunchedEffect(authResult) {
        if (authResult is AuthResult.Authenticated || authResult is AuthResult.Success) {
            onAuthSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHOENIXNET",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = { themeState.isDarkMode = !themeState.isDarkMode }) {
                    Text(if (themeState.isDarkMode) "🌙" else "☀️", fontSize = 24.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Mode Toggle
            TabRow(selectedTabIndex = if (isLoginMode) 0 else 1) {
                Tab(
                    selected = isLoginMode,
                    onClick = { isLoginMode = true },
                    text = { Text("Login") }
                )
                Tab(
                    selected = !isLoginMode,
                    onClick = { isLoginMode = false },
                    text = { Text("Register") }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Inputs
            OutlinedTextField(
                value = viewModel.firstName,
                onValueChange = { viewModel.firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.middleName,
                onValueChange = { viewModel.middleName = it },
                label = { Text("Middle Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.lastName,
                onValueChange = { viewModel.lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = viewModel.triggerWord,
                onValueChange = { viewModel.triggerWord = it },
                label = { Text("Trigger Word") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            if (authResult is AuthResult.Error) {
                Text(
                    text = (authResult as AuthResult.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    if (isLoginMode) viewModel.onLogin(onAuthSuccess)
                    else viewModel.onRegister(onAuthSuccess)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (isLoginMode) "ACCESS MESH" else "CREATE ACCOUNT")
            }
        }
    }
}
