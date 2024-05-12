package entity

import kotlinx.serialization.Serializable

@Serializable
data class Node(
    private val id: String,
    private val label: String
)
