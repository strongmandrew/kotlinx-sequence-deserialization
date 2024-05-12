package entity

import kotlinx.serialization.Serializable

@Serializable
data class Graph(
    val nodes: List<Node>,
    val edges: List<Edge>
) {

    init {
        println("Создаю экземпляр")
    }
}
