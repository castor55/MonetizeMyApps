package net.monetizemyapp.toolbox.extentions

import com.google.gson.Gson
import net.monetizemyapp.network.endOfString

fun Any?.toJson() = Gson().toJson(this) + endOfString()
