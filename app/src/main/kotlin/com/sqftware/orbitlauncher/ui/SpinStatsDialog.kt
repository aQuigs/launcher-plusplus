package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.SpinStats

/** Shows [stats], the tally of every spin of the ring and its folders. Done, Back and a tap outside call [onDismiss]. */
@Composable
fun SpinStatsDialog(stats: SpinStats, onDismiss: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    fun decimal(n: Double, places: Int) = String.format(locale, "%,.${places}f", n)
    fun turns(n: Double) = "${decimal(n, 1)} turns"

    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PopupButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Spin stats") },
        text = {
            if (stats.spins == 0) {
                Text("No spins yet. Drag up or down round the ring's side and let go.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatRow("Spins", String.format(locale, "%,d", stats.spins))
                    StatRow("Clockwise", turns(stats.clockwise))
                    StatRow("Anticlockwise", turns(stats.anticlockwise))
                    StatRow("Most in one spin", turns(stats.mostTurns))
                    StatRow("Top speed", "${decimal(stats.topRpm, 0)} rpm")
                    StatRow("Longest coast", "${decimal(stats.longestCoast, 1)} s")
                }
            }
        },
        modifier = Modifier.testTag(LauncherMenuTags.SPIN_STATS_DIALOG),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
