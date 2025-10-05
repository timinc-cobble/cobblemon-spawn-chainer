package us.timinc.mc.cobblemon.spawnchaining.extension

import kotlin.math.max
import kotlin.math.min

fun IntRange.constrain(value: Int) = min(max(value, this.min()), this.max())