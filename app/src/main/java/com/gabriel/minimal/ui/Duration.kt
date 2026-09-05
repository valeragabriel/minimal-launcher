package com.gabriel.minimal.ui

/**
 * Compact duration for a single line: "2h 15m", "45m", "0m".
 *
 * Raw minutes stop being readable past an hour or so — "135 min" makes you do
 * arithmetic before you know whether it is a lot.
 */
fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${remainder}m"
        remainder == 0 -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}
