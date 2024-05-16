package objects.packets

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = BoardPositionSerializer::class)
data class BoardPosition(val row: Int, val column: Int) {
    init {
        require(row in 0..1)
        when (row) {
            0 -> require(column in 0..<3)
            1 -> require(column in 0..<3)
            else -> require(false)
        }
    }
}

class BoardPositionSerializer : KSerializer<BoardPosition> {
    private val delegateSerializer = IntArraySerializer()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("BoardPosition", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): BoardPosition {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return BoardPosition(array[0], array[1])
    }

    override fun serialize(encoder: Encoder, value: BoardPosition) {
        encoder.encodeSerializableValue(delegateSerializer, intArrayOf(value.row, value.column))
    }
}
