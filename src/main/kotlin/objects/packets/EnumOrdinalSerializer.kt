package objects.packets

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

// https://slack-chats.kotlinlang.org/t/2476858/hi-is-there-a-way-to-serialize-an-enum-as-the-ordinal-withou
@OptIn(ExperimentalSerializationApi::class)
open class EnumOrdinalSerializer<E : Enum<E>>(serialName: String, private val values: Array<E>) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: E) {
        val index = values.indexOf(value)
        if (index == -1) {
            throw SerializationException(
                "$value is not a valid enum ${descriptor.serialName}, " +
                    "must be one of ${values.contentToString()}"
            )
        }
        encoder.encodeInt(index)
    }

    override fun deserialize(decoder: Decoder): E {
        val index = decoder.decodeInt()
        if (index !in values.indices) {
            throw SerializationException(
                "$index is not among valid ${descriptor.serialName} enum values, " +
                    "values size is ${values.size}"
            )
        }
        return values[index]
    }

    override fun toString(): String = "EnumOrdinalSerializer<${descriptor.serialName}>"
}
