package com.programovil.aura.regionsync.domain.model

data class RegionalDataItem(
    val id: String,
    val regionId: String,
    val name: String,
    val value: String // Assuming value can be any string representation
)
