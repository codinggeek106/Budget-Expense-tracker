package com.spendtrack.app.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.parser.AmountParser
import com.spendtrack.app.ui.components.TabContentInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TabContentInsets,
        topBar = { TopAppBar(title = { Text("Monthly budgets") }) },
    ) { padding ->
        val list = rows
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    Text(
                        "Tap a category to set how much you want to spend on it each month.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(list, key = { it.category }) { row ->
                    ListItem(
                        headlineContent = { Text(row.category) },
                        supportingContent = {
                            Text(
                                (row.limitPaise?.let { "${Money.format(it)} a month" } ?: "No limit") +
                                    " · ${Money.format(row.spentThisMonthPaise)} spent this month",
                            )
                        },
                        trailingContent = { Icon(Icons.Filled.Edit, contentDescription = "Edit ${row.category} budget") },
                        modifier = Modifier.clickable { editing = row.category },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val current = editing?.let { name -> rows?.firstOrNull { it.category == name } }
    if (current != null) {
        BudgetDialog(
            row = current,
            onDismiss = { editing = null },
            onSave = { limit ->
                viewModel.save(current.category, limit)
                editing = null
            },
        )
    }
}

@Composable
private fun BudgetDialog(row: BudgetRowState, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    var text by remember(row.category) { mutableStateOf(row.limitPaise?.let(::editableRupees).orEmpty()) }
    val parsed = AmountParser.parsePaise(text)
    val invalid = text.isNotBlank() && (parsed == null || parsed <= 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${row.category} budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(15) },
                    label = { Text("Monthly limit") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = { if (invalid) Text("Enter an amount like 5000 or 2500.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Spent this month: ${Money.format(row.spentThisMonthPaise)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !invalid, onClick = { onSave(if (text.isBlank()) null else parsed) }) {
                Text(if (text.isBlank()) "Remove limit" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Paise -> "5000" or "2500.5" for editing (no ₹, no grouping). */
private fun editableRupees(paise: Long): String {
    val rupees = paise / 100
    val fraction = paise % 100
    return if (fraction == 0L) rupees.toString() else "$rupees.${fraction.toString().padStart(2, '0')}"
}
