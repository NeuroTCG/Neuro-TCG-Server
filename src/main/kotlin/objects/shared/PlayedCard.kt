package objects.shared

import kotlinx.serialization.*

@Serializable
class PlayedCard(val id: Int) {
    var HP: Int = CardStats.getCardByID(id).maxHP
}
