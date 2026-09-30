package com.spendtrack.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendtrack.app.domain.model.BudgetProgress
import com.spendtrack.app.domain.model.CategorySpend
import com.spendtrack.app.domain.model.Money
import com.spendtrack.app.domain.model.MonthlySummary
import com.spendtrack.app.domain.model.PayeeSpend
import com.spendtrack.app.ui.components.MonthSelector
import com.spendtrack.app.ui.components.TabContentInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSetup: () -> Unit,
    onOpenPending: () -> Unit,
    onOpenBudgets: () -> Unit,
    onAddCash: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory),
) {
    val range by viewModel.month.range.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = TabContentInsets,
        topBar = {
            TopAppBar(
                title = { Text("SpendTrack") },
                actions = {
                    IconButton(onClick = onOpenSetup) { Icon(Icons.Filled.Settings, contentDescription = "Setup") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCash,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Cash") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSelector(
                    range = range,
                    canGoNext = viewModel.month.canGoNext(range),
                    onPrevious = viewModel.month::previous,
                    onNext = viewModel.month::next,
                )
            }
            val s = summary
            if (s == null || s.range != range) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item { TotalCard(s, onOpenPending) }
                if (s.paymentCount > 0) {
                    item { CategoryCard(s.categories) }
                    item { PayeeCard(s.topPayees) }
                }
                item { BudgetCard(s.budgets, onOpenBudgets) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun TotalCard(summary: MonthlySummary, onOpenPending: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Spent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Money.format(summary.totalPaise), style = MaterialTheme.typography.displaySmall)
            Text(
                if (summary.paymentCount == 0) "No spending recorded this month."
                else "${summary.paymentCount} payment${if (summary.paymentCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.pendingCount > 0) {
                TextButton(onClick = onOpenPending, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        "${Money.format(summary.pendingPaise)} in ${summary.pendingCount} payment" +
                            "${if (summary.pendingCount == 1) "" else "s"} still needs a category",
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(categories: List<CategorySpend>) {
    SectionCard("By category") {
        val max = categories.maxOfOrNull { it.totalPaise }?.coerceAtLeast(1) ?: 1
        val barColor = MaterialTheme.colorScheme.primary
        val pendingColor = MaterialTheme.colorScheme.outline
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        categories.forEach { spend ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        spend.category ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(Money.format(spend.totalPaise), style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalBar(
                    fraction = spend.totalPaise.toFloat() / max,
                    color = if (spend.category == null) pendingColor else barColor,
                    trackColor = trackColor,
                )
            }
        }
    }
}

/** A single horizontal bar of the category chart. */
@Composable
private fun HorizontalBar(fraction: Float, color: Color, trackColor: Color) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(
                color = color,
                size = Size(width.coerceAtLeast(size.height), size.height),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun PayeeCard(payees: List<PayeeSpend>) {
    SectionCard("Top payees") {
        payees.forEachIndexed { index, payee ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(payee.payee, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${payee.count} payment${if (payee.count == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(Money.format(payee.totalPaise), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BudgetCard(budgets: List<BudgetProgress>, onOpenBudgets: () -> Unit) {
    SectionCard("Budgets") {
        if (budgets.isEmpty()) {
            Text("No monthly limits set.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenBudgets, contentPadding = PaddingValues(0.dp)) { Text("Set budgets") }
        }
        budgets.forEach { BudgetRow(it) }
    }
}

@Composable
private fun BudgetRow(budget: BudgetProgress) {
    val color = if (budget.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(budget.category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${Money.format(budget.spentPaise)} of ${Money.format(budget.limitPaise)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LinearProgressIndicator(
            progress = { budget.fraction.coerceIn(0f, 1f) },
            color = color,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Text(
            if (budget.isOver) "${Money.format(-budget.remainingPaise)} over (${(budget.fraction * 100).toInt()}%)"
            else "${Money.format(budget.remainingPaise)} left",
            style = MaterialTheme.typography.bodySmall,
            color = if (budget.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
