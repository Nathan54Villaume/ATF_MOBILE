// file: app/src/main/java/com/riva/atsmobile/model/EtapeValidationDto.kt
package com.riva.atsmobile.model

import com.google.gson.annotations.SerializedName

data class EtapeValidationDto(
    @SerializedName("id_etape")
    val id_Etape: Int,

    @SerializedName("commentaire")
    val commentaire: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("tempsReel")
    val tempsReel: Int?
)
