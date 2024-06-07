package objects.packets

import kotlinx.serialization.*
import java.util.concurrent.atomic.*

@Serializable
sealed class Packet {
    companion object {
        fun generateResponseID(): Int {
            return responseIDCounter.incrementAndGet()
        }

        private var responseIDCounter = AtomicInteger(0)

    }
}
