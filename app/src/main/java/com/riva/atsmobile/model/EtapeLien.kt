// file: app/src/main/java/com/riva/atsmobile/model/EtapeLien.kt
package com.riva.atsmobile.model

import com.google.gson.annotations.SerializedName

data class EtapeLien(
    @SerializedName("operateur")
    val operateur: String,

    @SerializedName("ids")
    val ids: List<Int>
)
