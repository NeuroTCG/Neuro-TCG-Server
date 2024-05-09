package objects.shared

import kotlinx.serialization.*

@Serializable
class PlayedCard(@Required val id: Int) {
    @Required
    var HP: Int = CardStats.getCardByID(id).maxHP
}
