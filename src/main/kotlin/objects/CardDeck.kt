package objects

import objects.packets.objects.*

class CardDeck {
    // TODO: replace with actual implementation

    // This gets consumed first. Fill this up with cards you want to have in
    // the player's hand when testing.
    private val queue = mutableListOf(13)

    private val loop = arrayListOf<Int>()

    init {
        for (id: Int in CardStats.cardIDMapping.keys) {
            if (CardStats.cardIDMapping[id]!!.card_type != CardType.DECK_MASTER) {
                loop.add(id)
            }
        }
    }

    fun drawCard(): Int {
        if (queue.size > 0) {
            return queue.removeAt(0)
        } else {
            val id = loop.removeAt(0)
            loop.add(id)
            return id
        }
    }
}
