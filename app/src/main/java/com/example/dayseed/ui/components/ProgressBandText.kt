package com.example.dayseed.ui.components

import com.example.dayseed.data.ProgressBand

/**
 * Plain-words form of a D-040 progress band range, for screen readers and explanatory text.
 *
 * The visible chip shows only the band label (the exact percentage is always shown next to it).
 * The model's strict `shortBadge` strings (for example ">0–<25%") are unchanged and still pinned by tests;
 * this spells the same boundaries in words so assistive technology does not have to read `<`, `>` and dashes.
 */
fun ProgressBand.spokenRange(): String = when (this) {
    ProgressBand.ZERO -> "0 percent, nothing logged yet"
    ProgressBand.TINY_STEP -> "more than 0 percent and under 25 percent of your target"
    ProgressBand.GETTING_STARTED -> "25 percent to under 50 percent of your target"
    ProgressBand.HALFWAY -> "50 percent to under 75 percent of your target"
    ProgressBand.ALMOST_THERE -> "75 percent to under 100 percent of your target"
    ProgressBand.GOAL_MET -> "exactly 100 percent of your target"
    ProgressBand.EXTRA -> "over 100 percent and up to 150 percent of your target"
    ProgressBand.HUGE -> "over 150 percent of your target"
}

/** Spoken description for the band chip, e.g. "Halfway: 50 percent to under 75 percent of your target". */
fun ProgressBand.spokenDescription(): String = "$label: ${spokenRange()}"
