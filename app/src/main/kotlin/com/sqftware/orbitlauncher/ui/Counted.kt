package com.sqftware.orbitlauncher.ui

/** [n] of what [one] names, as words: "1 app", "3 apps". */
fun counted(n: Int, one: String): String = if (n == 1) "1 $one" else "$n ${one}s"
