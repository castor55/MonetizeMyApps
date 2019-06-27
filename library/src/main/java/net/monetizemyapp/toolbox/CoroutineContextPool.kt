package net.monetizemyapp.toolbox

import kotlinx.coroutines.Dispatchers

object CoroutineContextPool {
    val network = Dispatchers.IO
    val ui = Dispatchers.Main
}