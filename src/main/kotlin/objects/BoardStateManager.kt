package objects

import objects.packets.*
import objects.packets.objects.*
import kotlin.math.*

enum class Player {
    Player1,
    Player2;

    operator fun not(): Player {
        return if (this == Player1)
            Player2
        else
            Player1
    }
}

class BoardStateManager(
    private val db: GameDatabase,
    private val player1Connection: GameConnection,
    private val player2Connection: GameConnection
) {
    private var boardState = BoardState()
    private val player1ID: Int = 1
    private val player2ID: Int = 2
    val gameID = db.createGame(player1ID, player2ID)

    fun getBoardState(): BoardState {
        return this.boardState
    }

    private fun isTurnOfPlayer(player: Player): Boolean {
        return boardState.first_player_active == (player == Player.Player1)
    }

    private fun playerToIndex(player: Player): Int {
        return if (player == Player.Player1)
            0
        else
            1
    }

    private fun getConnection(player: Player): GameConnection {
        return if (player == Player.Player1)
            player1Connection
        else
            player2Connection
    }

    private fun handContains(player: Player, id: Int): Boolean {
        return this.boardState.hands[playerToIndex(player)].contains(id)
    }

    private fun getCard(player: Player, position: CardPosition): CardState? {
        return this.boardState.cards[playerToIndex(player)][position.row][position.column]
    }

    private fun setCard(player: Player, position: CardPosition, card: CardState?) {
        this.boardState.cards[playerToIndex(player)][position.row][position.column] = card
    }

    private fun placeInHand(player: Player, cardID: Int) {
        this.boardState.hands[playerToIndex(player)].add(cardID)
    }

    private fun removeFromHand(player: Player, cardID: Int) {
        this.boardState.hands[playerToIndex(player)].remove(cardID)
    }

    private fun getRam(player: Player): Int {
        return this.boardState.ram[playerToIndex(player)]
    }

    private fun getMaxRam(player: Player): Int {
        return this.boardState.max_ram[playerToIndex(player)]
    }

    private fun removeRam(player: Player, amount: Int) {
        assert(amount > 0)
        this.boardState.ram[playerToIndex(player)] -= amount
        assert(this.boardState.ram[playerToIndex(player)] in 0..getMaxRam(player))
    }

    private fun refreshRam(player: Player) {
        if (getMaxRam(player) < 10) {
            this.boardState.max_ram[playerToIndex(player)] += 1
        }
        this.boardState.ram[playerToIndex(player)] = getMaxRam(player)
    }

    suspend fun handleSummonPacket(packet: SummonRequestPacket, player: Player) {
        if (!isTurnOfPlayer(player)) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    isYou = true,
                    valid = false,
                    newCard = null,
                    newRam = -1
                )
            )
            return
        }
        if (getCard(player, packet.position) != null) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    newCard = null,
                    newRam = -1
                )
            )
            return
        }
        if (!handContains(player, packet.card_id)) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    newCard = null,
                    newRam = -1
                )
            )
            return
        }
        if (getRam(player) < CardStats.getCardByID(packet.card_id).summoning_cost) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    newCard = null,
                    newRam = -1
                )
            )
            return
        }

        val newCardState = CardState(packet.card_id, CardStats.getCardByID(packet.card_id).max_hp)
        setCard(
            player,
            packet.position,
            newCardState
        )

        removeFromHand(player, packet.card_id)
        removeRam(player, CardStats.getCardByID(packet.card_id).summoning_cost)

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                true,
                valid = true,
                newCard = newCardState,
                newRam = getRam(player)
            )
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                newCard = newCardState,
                newRam = getRam(player)
            )
        )
    }


    suspend fun handleAttackPacket(packet: AttackRequestPacket, player: Player) {
        val sendInvalid = suspend {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    isYou = true,
                    valid = false,
                    targetCard = null,
                    attackerCard = null,
                )
            )
        }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }

        var attacker = getCard(player, packet.attacker_position)
        var target = getCard(!player, packet.target_position)



        if (attacker == null || target == null) {
            sendInvalid()
            return
        }

        if (attacker.phase < CardTurnPhase.Action) {
            sendInvalid()
            return
        }

        if (!isSlotReachable(player, packet.attacker_position, packet.target_position)) {
            sendInvalid()
            return
        }

        val canAttackBack = isSlotReachable(!player, packet.target_position, packet.attacker_position)

        attacker.phase = CardTurnPhase.Done
        target.health -= CardStats.getCardByID(attacker.id).base_atk
        if (canAttackBack)
            attacker.health -= max(CardStats.getCardByID(target.id).base_atk - 1, 0)
        if (attacker.health <= 0)
            attacker = null
        if (target.health <= 0)
            target = null


        setCard(player, packet.attacker_position, attacker)
        setCard(!player, packet.target_position, target)

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
                targetCard = target,
                attackerCard = attacker,
            )
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                targetCard = target,
                attackerCard = attacker,
            )
        )
    }

    private fun isSlotReachable(player: Player, attackerPosition: CardPosition, targetPosition: CardPosition): Boolean {
        val isFrontEmpty = getCard(!player, CardPosition(CardPosition.FRONT_ROW, 0)) == null
            && getCard(!player, CardPosition(CardPosition.FRONT_ROW, 1)) == null
            && getCard(!player, CardPosition(CardPosition.FRONT_ROW, 2)) == null
            && getCard(!player, CardPosition(CardPosition.FRONT_ROW, 3)) == null

        val attackerReach = CardStats.getCardByID(getCard(player, attackerPosition)!!.id).attack_range

        assert(attackerPosition.row in 0..1)
        assert(targetPosition.row in 0..1)

        return when (attackerPosition.row to targetPosition.row) {
            CardPosition.FRONT_ROW to CardPosition.FRONT_ROW -> true
            CardPosition.FRONT_ROW to CardPosition.BACK_ROW -> isFrontEmpty || attackerReach == AttackRange.REACH
            CardPosition.BACK_ROW to CardPosition.FRONT_ROW -> attackerReach == AttackRange.REACH
            CardPosition.BACK_ROW to CardPosition.BACK_ROW -> false
            else -> {
                assert(false) { "unreachable" }
                false
            }
        }
    }

    suspend fun handleSwitchPlacePacket(packet: SwitchPlaceRequestPacket, player: Player) {
        val sendInvalid = suspend {
            getConnection(player).sendPacket(packet.getResponsePacket(isYou = true, valid = false))
        }
        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }
        if (packet.position1 == packet.position2) {
            sendInvalid()
            return
        }

        val c1 = getCard(player, packet.position1)
        val c2 = getCard(!player, packet.position2)

        if ((c1 != null && c1.phase < CardTurnPhase.MoveOrAction) || (c2 != null && c2.phase < CardTurnPhase.MoveOrAction)) {
            sendInvalid()
            return
        }

        c1?.phase = CardTurnPhase.Action
        c2?.phase = CardTurnPhase.Action

        setCard(player, packet.position1, c2)
        setCard(player, packet.position2, c1)

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true
            )
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true
            )
        )
    }

    suspend fun handleEndTurn(player: Player) {
        if (!isTurnOfPlayer(player)) {
            return
        }

        getConnection(!player).sendPacket(StartTurnPacket())
        refreshRam(player)

        foreachSlot(player, ::resetCard)

        boardState.first_player_active = !boardState.first_player_active
    }

    private fun foreachSlot(player: Player, f: (Player, CardPosition) -> Unit) {
        for (i in 0..<4) {
            f(player, CardPosition(CardPosition.FRONT_ROW, i))
        }
        for (i in 0..<3) {
            f(player, CardPosition(CardPosition.BACK_ROW, i))
        }
    }

    private fun resetCard(player: Player, position: CardPosition) {
        val card = getCard(player, position)
        card?.phase = CardTurnPhase.MoveOrAction
    }

    private val firstQueue = mutableListOf(2, 1, 1, 1, 0, 0)
    private val secondQueue = mutableListOf(2, 1, 0, 0, 1, 1)

    suspend fun handleDrawCard(player: Player) {
        if (!isTurnOfPlayer(player)) {
            getConnection(player).sendPacket(DrawCard(-1, true))
            return
        }
        if (this.boardState.hands[playerToIndex(player)].size > 5) {
            getConnection(player).sendPacket(DrawCard(-1, true))
            return
        }

        val cardID = if (player == Player.Player1) firstQueue.removeAt(0) else secondQueue.removeAt(0)

        placeInHand(player, cardID)

        getConnection(player).sendPacket(DrawCard(cardID, true))
        getConnection(!player).sendPacket(DrawCard(cardID, false))
    }

    suspend fun handleUseAbilityPacket(packet: UseAbilityRequestPacket, player: Player) {
        val sendInvalid = suspend {
            getConnection(player).sendPacket(packet.getResponsePacket(isYou = true, valid = false, null, null))
        }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }

        val abilityCard = getCard(player, packet.ability_position)
        if (abilityCard == null || abilityCard.phase < CardTurnPhase.Action || abilityCard.ability_was_used) {
            sendInvalid()
            return
        }

        val ability = CardStats.getCardByID(abilityCard.id).ability
        when (ability.effect) {
            AbilityEffect.NONE -> TODO()
            AbilityEffect.ADD_HP_TO_ALLY_CARD -> {
                val ally = getCard(player, packet.target_position)
                if (ally == null) {
                    sendInvalid()
                    return
                }
                if (getRam(player) < ability.cost) {
                    sendInvalid()
                    return
                }
                assert(ability.range == AbilityRange.ALLY_CARD)

                ally.health += ability.value
                ally.health = ally.health.coerceIn(0, CardStats.getCardByID(ally.id).max_hp)
                setCard(player, packet.target_position, ally)

                removeRam(player, ability.cost)

                getConnection(player).sendPacket(
                    packet.getResponsePacket(
                        isYou = true,
                        valid = true,
                        targetCard = ally,
                        abilityCard = abilityCard
                    )
                )
                getConnection(!player).sendPacket(
                    packet.getResponsePacket(
                        isYou = false,
                        valid = true,
                        targetCard = ally,
                        abilityCard = abilityCard
                    )
                )
            }

            AbilityEffect.SEAL_ENEMY_CARD -> TODO()
            AbilityEffect.ATTACK -> TODO()
            AbilityEffect.ATTACK_ROW -> TODO()
        }

        abilityCard.phase = CardTurnPhase.Done
        abilityCard.ability_was_used = true
    }


}
