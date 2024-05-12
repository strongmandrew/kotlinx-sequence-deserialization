package entity

import kotlinx.serialization.Serializable

@Serializable
data class Edge(
    private val id: String,
    private val sourceId: String,
    private val targetId: String,
    private val properties: List<Property>
)
