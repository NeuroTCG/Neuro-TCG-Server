/*
 * Created by Matthew Clifton on 2024.11.11
 * Copyright © 2024 Osman Balci. All rights reserved.
 */

package objects.passives

import objects.*
import objects.packets.*
import objects.packets.objects.*

open class Passive (
    var cardId: Int,
    var player: Player
) {
    fun update(lastChange: Packet, boardState: BoardState): UpdateAction? {
        return null;
    }
}
