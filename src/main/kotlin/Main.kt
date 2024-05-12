import entity.Edge
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import sequence.supplySequence
import stream.TokenArrayFilteringInputStream

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val graphSource =
        TokenArrayFilteringInputStream::class.java.getResourceAsStream("/graph.json") ?: error("Ресурс не найден")

    val graphBytes = graphSource.readBytes()

    val supplier = {
        val graphInputStream = graphBytes.inputStream()
        val nodesFilteringInputStream = TokenArrayFilteringInputStream(graphInputStream, "\"edges\": ")

        Json.decodeToSequence(
            stream = nodesFilteringInputStream,
            deserializer = Edge.serializer()
        )
    }

    val suppliedSequence = supplySequence(supplier)

    suppliedSequence.forEach(::println)
    suppliedSequence.forEach(::println)
}