package objects.packets.objects

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = BoardPositionSerializer::class)
/**
 *
 * Card positions are always relative to the side of the board, which is usually inferred.
 * The numbering looks like this:
 *
 * ```
 *           opponent
 *
 *     [1, 2] [1, 1] [1, 0]
 * [0, 3] [0, 2] [0, 1] [0, 0]
 * ---------------------------
 * [0, 0] [0, 1] [0, 2] [0, 3]
 *    [1, 0] [1, 1] [1, 2]
 *
 *            you
 * ```
 */
data class CardPosition(
    val row: Int,
    val column: Int,
) {
    init {
        require(row in 0..1)
        when (row) {
            FRONT_ROW -> require(column in 0..<4)
            BACK_ROW -> require(column in 0..<3)
            else -> require(false)
        }
    }

    companion object {
        /**
         * The one with 4 spaces
         */
        const val FRONT_ROW = 0

        /**
         * The one with 3 spaces
         */
        const val BACK_ROW = 1
    }
}

class BoardPositionSerializer : KSerializer<CardPosition> {
    private val delegateSerializer = IntArraySerializer()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("BoardPosition", delegateSerializer.descriptor)

    override fun deserialize(decoder: Decoder): CardPosition {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return CardPosition(array[0], array[1])
    }

    override fun serialize(
        encoder: Encoder,
        value: CardPosition,
    ) {
        encoder.encodeSerializableValue(delegateSerializer, intArrayOf(value.row, value.column))
    }
}
