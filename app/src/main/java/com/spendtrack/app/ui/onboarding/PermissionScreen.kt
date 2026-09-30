package com.spendtrack.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendtrack.app.system.PaymentListenerService

private enum class ChecklistItem(val title: String, val detail: String) {
    RESTRICTED(
        "Allow restricted settings",
        "Sideloaded apps can't get notification access until you allow it. App info → ⋮ (top right) → " +
            "\"Allow restricted settings\", then enable notification access again.",
    ),
    AUTOSTART(
        "Autostart",
        "Lets HyperOS restart the payment detector after a reboot or when it is cleared.",
    ),
    BATTERY(
        "Battery saver: No restrictions",
        "App info → Battery saver → No restrictions. Otherwise HyperOS kills the detector in the background.",
    ),
    LOCK_RECENTS(
        "Lock in recents",
        "Open recents, long-press the SpendTrack card and tap the lock icon.",
    ),
    FLOATING(
        "Floating notifications",
        "App notifications → allow floating notifications so payment prompts pop up.",
    ),
    POPUP_BACKGROUND(
        "Display pop-up windows while running in background",
        "Other permissions → allow it so the category picker can open over other apps.",
    ),
}

private const val PREFS = "setup_checklist"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(onOpenDebug: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(SetupStatus.read(context)) }
    LifecycleResumeEffect(Unit) {
        status = SetupStatus.read(context)
        onPauseOrDispose { }
    }
    val connected by PaymentListenerService.connected.collectAsStateWithLifecycle()

    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val checked = remember {
        mutableStateMapOf<ChecklistItem, Boolean>().apply {
            ChecklistItem.entries.forEach { put(it, prefs.getBoolean(it.name, false)) }
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { status = SetupStatus.read(context) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("SpendTrack setup") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "SpendTrack reads payment notifications from GPay, PhonePe and Paytm so it can ask " +
                        "what each payment was for. It has no internet permission: everything stays on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                SetupCard(title = "Notification access") {
                    StatusRow("Access granted", status.listenerEnabled)
                    StatusRow("Detector connected", status.listenerEnabled && connected)
                    Text(
                        "Needed to see UPI payment notifications. Only GPay, PhonePe and Paytm " +
                            "notifications are read.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            SetupIntents.launchFirst(context, SetupIntents.notificationListenerSettings())
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (status.listenerEnabled) "Notification access settings" else "Grant notification access") }
                    if (status.listenerEnabled && !connected) {
                        OutlinedButton(
                            onClick = { PaymentListenerService.requestRebind(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Reconnect detector") }
                        Text(
                            "Access is on but the detector isn't running. Tap Reconnect; if it stays " +
                                "disconnected, turn access off and on again and check the HyperOS list below.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    SetupCard(title = "Payment prompts") {
                        StatusRow("Notifications allowed", status.notificationsAllowed)
                        Text(
                            "Needed to ask you for a category right after each payment.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!status.notificationsAllowed) {
                            Button(
                                onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Allow notifications") }
                            TextButton(onClick = {
                                SetupIntents.launchFirst(context, SetupIntents.appNotificationSettings(context))
                            }) { Text("Open notification settings instead") }
                        }
                    }
                }
            }

            item {
                Text(
                    "HyperOS checklist",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "HyperOS aggressively stops background apps. Tick each item once you've done it.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            items(ChecklistItem.entries) { item ->
                val autoDone = item == ChecklistItem.BATTERY && status.batteryUnrestricted
                ChecklistRow(
                    item = item,
                    checked = autoDone || checked[item] == true,
                    onCheckedChange = { value ->
                        checked[item] = value
                        prefs.edit { putBoolean(item.name, value) }
                    },
                    onOpen = { openChecklistTarget(context, item) },
                )
            }

            item {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Continue to app") }
                OutlinedButton(
                    onClick = onOpenDebug,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Captured notifications (debug)") }
            }
        }
    }
}

private fun openChecklistTarget(context: Context, item: ChecklistItem) {
    val appDetails = SetupIntents.appDetails(context)
    when (item) {
        ChecklistItem.RESTRICTED, ChecklistItem.BATTERY -> SetupIntents.launchFirst(context, appDetails)
        ChecklistItem.AUTOSTART -> SetupIntents.launchFirst(context, SetupIntents.miuiAutostart(), appDetails)
        ChecklistItem.FLOATING -> SetupIntents.launchFirst(context, SetupIntents.appNotificationSettings(context), appDetails)
        ChecklistItem.POPUP_BACKGROUND -> SetupIntents.launchFirst(context, SetupIntents.miuiPermissionEditor(context), appDetails)
        ChecklistItem.LOCK_RECENTS -> Unit
    }
}

@Composable
private fun SetupCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            if (ok) "Yes" else "No",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                Text(item.detail, style = MaterialTheme.typography.bodySmall)
            }
            if (item != ChecklistItem.LOCK_RECENTS) {
                TextButton(onClick = onOpen) { Text("Open") }
            }
        }
    }
}
