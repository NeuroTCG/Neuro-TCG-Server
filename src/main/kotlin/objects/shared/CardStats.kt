package objects.shared

import kotlinx.serialization.*

@Serializable
class CardStats(val maxHP: Int, val baseATK: Int) {
    companion object {
        var cardIDMapping: HashMap<Int, CardStats>? = null

        fun getCardByID(id: Int): CardStats {
            return cardIDMapping!!.getValue(id)
        }
    }
}


