package objects.packets

import kotlinx.serialization.*
import java.util.concurrent.atomic.*

@Serializable
open class Packet(@Required val type: PacketType) {
    companion object {
        fun generateResponseID(): Int {
            return responseIDCounter.incrementAndGet()
        }

        private var responseIDCounter = AtomicInteger(0)

    }
}
