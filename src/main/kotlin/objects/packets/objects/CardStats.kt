package objects.packets.objects

import kotlinx.serialization.*

@Serializable
class CardStats(@Required val max_hp: Int, val base_atk: Int) {
    companion object {
        // TODO: maybe move this out of here since it isn't networking related
        var cardIDMapping: HashMap<Int, CardStats> = hashMapOf(
            Pair(0, CardStats(100, 50)),
            Pair(1, CardStats(200, 5)),
        )


        fun getCardByID(id: Int): CardStats {
            return cardIDMapping.getValue(id)
        }
    }
}


