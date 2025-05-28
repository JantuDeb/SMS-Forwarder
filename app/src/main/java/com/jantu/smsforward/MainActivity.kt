package com.jantu.smsforward

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jantu.smsforward.model.ForwardingMethod
import com.jantu.smsforward.model.UserSettings
import com.jantu.smsforward.ui.theme.SmsWhatsappForwarderTheme // Your app's theme
import com.jantu.smsforward.util.PreferencesHelper

class MainActivity : ComponentActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS // Request upfront if SMS forwarding is an option
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsWhatsappForwarderTheme {
                MainScreen(requiredPermissions)
            }
        }
    }


}

@Composable
fun MainScreen(permissions: Array<String>) {
    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        hasPermissions = permissionsMap.values.all { it }
        if (!hasPermissions) {
            // Optionally show a message if permissions are crucial and denied
            // For now, the UI will just reflect the permission status
        }
    }

    LaunchedEffect(key1 = true) { // Use Unit or a constant key
        if (!hasPermissions) {
            permissionLauncher.launch(permissions)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (hasPermissions) {
            SettingsScreen()
        } else {
            PermissionRequestScreen {
                permissionLauncher.launch(permissions)
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Permissions Required", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "This app needs SMS permissions to read incoming messages and " +
                    "optionally send SMS for forwarding. Please grant these permissions to continue.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val initialSettings = remember { PreferencesHelper.loadSettings(context) }

    var triggerTextsString by remember { mutableStateOf(initialSettings.triggerTexts.joinToString(",")) }
    var forwardingPhoneNumber by remember { mutableStateOf(initialSettings.forwardingPhoneNumber) }
    var selectedMethod by remember { mutableStateOf(initialSettings.forwardingMethod) }

    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("SMS Forwarder Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Configure SMS forwarding rules.",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = triggerTextsString,
                onValueChange = { triggerTextsString = it },
                label = { Text("Trigger Texts (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = forwardingPhoneNumber,
                onValueChange = { forwardingPhoneNumber = it },
                label = { Text("Forwarding Phone Number (with country code)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Forwarding Method:", style = MaterialTheme.typography.titleSmall)
            ForwardingMethodRadioGroup(
                selectedMethod = selectedMethod,
                onMethodSelected = { selectedMethod = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val triggers = triggerTextsString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val settings = UserSettings(
                        triggerTexts = triggers,
                        forwardingPhoneNumber = forwardingPhoneNumber.trim(),
                        forwardingMethod = selectedMethod
                    )
                    PreferencesHelper.saveSettings(context, settings)
                    // Show confirmation
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Settings Saved") },
                    text = { Text("Your SMS forwarding settings have been updated.") },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ForwardingMethodRadioGroup(
    selectedMethod: ForwardingMethod,
    onMethodSelected: (ForwardingMethod) -> Unit
) {
    val methods = ForwardingMethod.values()

    Column {
        methods.forEach { method ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (method == selectedMethod),
                    onClick = { onMethodSelected(method) }
                )
                Text(
                    text = method.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // Nicer display name
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}