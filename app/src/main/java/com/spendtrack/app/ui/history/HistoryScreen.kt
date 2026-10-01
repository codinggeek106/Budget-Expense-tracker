package com.spendtrack.app.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.model.TxnStatus
import com.spendtrack.app.ui.CategorizeActivity
import com.spendtrack.app.ui.components.MonthSelector
import com.spendtrack.app.ui.components.TabContentInsets
import com.spendtrack.app.ui.format.formatDateTime
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onAddCash: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val context = LocalContext.current
    val range by viewModel.month.range.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var exportAll by rememberSaveable { mutableStateOf(false) }
    // Storage Access Framework: the user picks where the file goes; no storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.export(uri, if (exportAll) null else range) { message ->
                scope.launch { snackbar.showSnackbar(message) }
            }
        }
    }

    fun ignoreWithUndo(txn: TransactionEntity) {
        viewModel.ignore(txn)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar("Payment ignored", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoIgnore(txn)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TabContentInsets,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export this month (CSV)") },
                            onClick = {
                                menuOpen = false
                                exportAll = false
                                exportLauncher.launch("spendtrack-${range.month}.csv")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export everything (CSV)") },
                            onClick = {
                                menuOpen = false
                                exportAll = true
                                exportLauncher.launch("spendtrack-all-${LocalDate.now()}.csv")
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCash,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Cash") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthSelector(
                range = range,
                canGoNext = viewModel.month.canGoNext(range),
                onPrevious = viewModel.month::previous,
                onNext = viewModel.month::next,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            val s = state
            when {
                s == null || s.range != range -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                s.transactions.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No payments this month.\n\nUPI payments appear here automatically. Tap Cash to add one you paid in cash.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    item {
                        Text(
                            "Tap to change the category. Swipe left to ignore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(s.transactions, key = { "${it.id}-${it.status}" }) { txn ->
                        val open = { context.startActivity(CategorizeActivity.intent(context, txn.id)) }
                        if (txn.status == TxnStatus.IGNORED) {
                            HistoryRow(txn, onClick = open)
                        } else {
                            SwipeToIgnore(onIgnore = { ignoreWithUndo(txn) }) { HistoryRow(txn, onClick = open) }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToIgnore(onIgnore: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value -> if (value == SwipeToDismissBoxValue.EndToStart) onIgnore() },
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Ignore", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

@Composable
private fun HistoryRow(txn: TransactionEntity, onClick: () -> Unit) {
    val ignored = txn.status == TxnStatus.IGNORED
    val dim = if (ignored) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(txn.payee, style = MaterialTheme.typography.bodyLarge, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${UpiApps.label(txn.appPkg)} · ${formatDateTime(txn.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            txn.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
            Text(
                Money.format(txn.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                color = dim,
                textDecoration = if (ignored) TextDecoration.LineThrough else null,
            )
            Text(
                when (txn.status) {
                    TxnStatus.PENDING -> "Needs category"
                    TxnStatus.IGNORED -> "Ignored"
                    TxnStatus.CATEGORIZED -> txn.category.orEmpty()
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (txn.status) {
                    TxnStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                    TxnStatus.IGNORED -> MaterialTheme.colorScheme.onSurfaceVariant
                    TxnStatus.CATEGORIZED -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}
