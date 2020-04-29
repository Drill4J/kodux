package com.epam.kodux

import kotlinx.serialization.Serializable

@Serializable
data class BigData(
    @Id val id: String,
    val map: Map<String, List<SomeData>>

)

@Serializable
data class SomeData(
    val id: String,
    val type: String,
    val data: List<String>
)
