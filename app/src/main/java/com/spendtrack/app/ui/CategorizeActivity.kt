package com.spendtrack.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.Category
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.model.TxnStatus
import com.spendtrack.app.ui.format.formatDateTime
import com.spendtrack.app.ui.theme.SpendTrackTheme

/** Small floating picker: full category list, custom category, and note for one transaction. */
class CategorizeActivity : ComponentActivity() {

    private var txnId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        txnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)
        if (txnId < 0) {
            finish()
            return
        }
        setContent {
            SpendTrackTheme {
                val id = txnId
                val vm: CategorizeViewModel = viewModel(key = "txn-$id", factory = CategorizeViewModel.factory(id))
                CategorizeCard(vm, onClose = ::finish)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra(EXTRA_TXN_ID, -1L).takeIf { it >= 0 }?.let { txnId = it }
    }

    companion object {
        const val EXTRA_TXN_ID = "txnId"

        fun intent(context: Context, txnId: Long): Intent =
            Intent(context, CategorizeActivity::class.java)
                .setData(Uri.parse("spendtrack://txn/$txnId"))
                .putExtra(EXTRA_TXN_ID, txnId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun CategorizeCard(vm: CategorizeViewModel, onClose: () -> Unit) {
    val load by vm.txn.collectAsStateWithLifecycle()
    val choices by vm.choices.collectAsStateWithLifecycle()

    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().imePadding(),
    ) {
        when (val state = load) {
            TxnLoad.Loading -> Row(Modifier.fillMaxWidth().padding(48.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            TxnLoad.Missing -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("This payment no longer exists.", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
            is TxnLoad.Loaded -> CategorizeForm(
                txn = state.txn,
                choices = choices,
                onSave = { category, note -> vm.save(category, note, onClose) },
                onIgnore = { vm.ignore(onClose) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorizeForm(
    txn: TransactionEntity,
    choices: List<String>,
    onSave: (category: String, note: String) -> Unit,
    onIgnore: () -> Unit,
) {
    var selected by rememberSaveable(txn.id) { mutableStateOf(txn.category) }
    var custom by rememberSaveable(txn.id) { mutableStateOf("") }
    var note by rememberSaveable(txn.id) { mutableStateOf(txn.note.orEmpty()) }
    var saving by rememberSaveable(txn.id) { mutableStateOf(false) }

    val isOther = selected == Category.OTHER
    val effectiveCategory = if (isOther) custom.trim().ifEmpty { Category.OTHER } else selected

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Money.format(txn.amountPaise), style = MaterialTheme.typography.headlineMedium)
        Text("to ${txn.payee}", style = MaterialTheme.typography.titleMedium)
        Text(
            "${UpiApps.label(txn.appPkg)} · ${formatDateTime(txn.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (txn.status != TxnStatus.PENDING) {
            Text(
                if (txn.status == TxnStatus.IGNORED) "Currently ignored" else "Currently: ${txn.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text("What was this for?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { selected = category },
                    label = { Text(if (category == Category.OTHER) "Other…" else category) },
                )
            }
        }
        if (isOther) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.take(40) },
                label = { Text("Category name (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(200) },
            label = { Text("Note (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !saving, onClick = {
                saving = true
                onIgnore()
            }) { Text("Ignore") }
            Spacer(Modifier.weight(1f))
            Button(
                enabled = !saving && effectiveCategory != null,
                onClick = {
                    saving = true
                    onSave(effectiveCategory!!, note)
                },
            ) { Text("Save") }
        }
    }
}
