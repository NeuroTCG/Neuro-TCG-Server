/*
 * Created by BSGMatt on 2024.11.11
 */

package objects.passives

import objects.packets.*

class PassiveManager (
    val passives: MutableList<Passive>,
) {
    //A Packet used to describe the last action that occurred.
    // Used to provide context when updating passives.
    var lastPacket: Packet? = null;

    fun addNewPassive() {

    }

    fun getPassiveByCardId(): Passive? {
        return null;
    }
}
