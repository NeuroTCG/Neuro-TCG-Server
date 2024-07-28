package objects.packets.objects

import kotlinx.serialization.*

@Serializable
class CardStats(
    @Required val max_hp: Int, @Required val base_atk: Int, @Required val summoning_cost: Int
) {
    companion object {
        // TODO: maybe move this out of here since it isn't networking related
        val cardIDMapping: HashMap<Int, CardStats> = hashMapOf(
            Pair(0, CardStats(2, 2, 1)), // Pirate Evil / Neuro
            Pair(1, CardStats(3, 3, 5)), // Filipino Boy
        )

        init {
            cardIDMapping.forEach { (_, v) ->
                assert(v.summoning_cost in 1..10) // may be 0..10
            }
        }

        fun getCardByID(id: Int): CardStats {
            return cardIDMapping.getValue(id)
        }
    }
}


