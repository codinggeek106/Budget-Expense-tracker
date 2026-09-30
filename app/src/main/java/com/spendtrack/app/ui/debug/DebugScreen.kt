package com.spendtrack.app.ui.debug

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.data.db.RawNotificationEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.system.PendingReminderWorker
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = viewModel(factory = DebugViewModel.Factory),
) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Captured (${rows.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = rows.isNotEmpty(),
                        onClick = { copyToClipboard(context, SampleExport.format(rows.reversed())) },
                    ) { Text("Copy all") }
                    IconButton(
                        enabled = rows.isNotEmpty(),
                        onClick = { share(context, SampleExport.format(rows.reversed())) },
                    ) { Icon(Icons.Filled.Share, contentDescription = "Share all") }
                    IconButton(
                        enabled = rows.isNotEmpty(),
                        onClick = { confirmClear = true },
                    ) { Icon(Icons.Filled.Delete, contentDescription = "Clear") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = viewModel::createTestPayment, modifier = Modifier.weight(1f)) {
                    Text("Test prompt")
                }
                OutlinedButton(onClick = { PendingReminderWorker.runNow(context) }, modifier = Modifier.weight(1f)) {
                    Text("Run reminder now")
                }
            }
            CapturedList(rows, Modifier.weight(1f))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear captured notifications?") },
            text = { Text("This only deletes the debug log. Copy anything you still need first.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CapturedList(rows: List<RawNotificationEntity>, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing captured yet.\n\nMake a UPI payment with GPay, PhonePe or Paytm and its " +
                        "notification will show up here. Then tap \"Copy all\" and paste the result into " +
                        "the sample files.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.id }) { RawNotificationCard(it) }
            }
        }
    }
}

@Composable
private fun RawNotificationCard(row: RawNotificationEntity) {
    val time = remember(row.postedAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(row.postedAt))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${UpiApps.label(row.pkg)} · $time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                row.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                row.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                row.bigText?.takeIf { it != row.text }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clip = ClipData.newPlainText("SpendTrack samples", text)
    // Payment details: keep them out of the clipboard preview on Android 13+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Share samples"))
}
