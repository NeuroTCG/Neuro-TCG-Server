package objects

class CardDeck {
    // TODO: replace with actual implementation

    // used to force feed some cards
    private val queue = mutableListOf(2)

    // gets repeated after `queue` is empty
    private val loop = mutableListOf(0, 1, 3)

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
