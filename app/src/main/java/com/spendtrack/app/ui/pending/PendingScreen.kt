package com.spendtrack.app.ui.pending

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.ui.CategorizeActivity
import com.spendtrack.app.ui.format.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScreen(
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PendingViewModel = viewModel(factory = PendingViewModel.Factory),
) {
    val context = LocalContext.current
    val pending by viewModel.pending.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Pending") },
                actions = {
                    IconButton(onClick = onOpenSetup) { Icon(Icons.Filled.Settings, contentDescription = "Setup") }
                },
            )
        },
    ) { padding ->
        val rows = pending
        when {
            rows == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            rows.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "All caught up.\n\nNew UPI payments wait here until you pick a category.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.id }) { txn ->
                    PendingRow(
                        txn = txn,
                        onOpen = { context.startActivity(CategorizeActivity.intent(context, txn.id)) },
                        onIgnore = { viewModel.ignore(txn.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRow(txn: TransactionEntity, onOpen: () -> Unit, onIgnore: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(Money.format(txn.amountPaise), style = MaterialTheme.typography.titleMedium)
                Text(txn.payee, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${UpiApps.label(txn.appPkg)} · ${formatDateTime(txn.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onIgnore) { Icon(Icons.Filled.Close, contentDescription = "Ignore") }
        }
    }
}
