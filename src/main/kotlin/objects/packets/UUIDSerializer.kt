package objects.packets

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.*

class UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) {
        encoder.encodeString(value.toString())
    }
}
