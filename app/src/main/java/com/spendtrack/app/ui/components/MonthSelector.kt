package com.spendtrack.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.spendtrack.app.domain.model.MonthRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** The month a screen is showing. Can't move past the current month. */
class MonthState(private val clock: Clock = Clock.systemDefaultZone()) {

    private val _range = MutableStateFlow(MonthRange.current(clock))
    val range: StateFlow<MonthRange> = _range.asStateFlow()

    fun previous() {
        _range.value = _range.value.previous()
    }

    fun next() {
        if (canGoNext(_range.value)) _range.value = _range.value.next()
    }

    fun canGoNext(range: MonthRange): Boolean = range.month < YearMonth.now(clock)
}

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun MonthSelector(
    range: MonthRange,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            range.month.format(monthFormat),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}
