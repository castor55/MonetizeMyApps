package net.monetizemyapp.toolbox

import kotlinx.coroutines.Dispatchers

object CoroutineContextPool {
    val default = Dispatchers.Default
    val network = Dispatchers.IO
    val ui = Dispatchers.Main
}