package net.kuama.documentscanner.extensions


fun Int.percentOf(percent: Int): Double = (this / 100 * percent).toDouble()
