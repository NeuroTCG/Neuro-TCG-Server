package objects

import objects.packets.*
import objects.packets.objects.*
import kotlin.math.*

class BoardStateManager(
    val db: GameDatabase,
    val player1Connection: GameConnection,
    val player2Connection: GameConnection
) {
    private var boardState = BoardState()
    val player1ID: Int = 1
    val player2ID: Int = 2
    val gameID = db.createGame(player1ID, player2ID)

    fun getBoardState(): BoardState {
        return this.boardState
    }

    private fun isTurnOfPlayer(isFirstPlayer: Boolean): Boolean {
        return isFirstPlayer == boardState.first_player_active
    }

    private fun playerBoolToIndex(isFirstPlayer: Boolean): Int {
        return if (isFirstPlayer) 0
        else 1
    }

    private fun getConnection(isFirstPlayer: Boolean): GameConnection {
        return if (isFirstPlayer)
            player1Connection
        else
            player2Connection
    }

    private fun handContains(isFirstPlayer: Boolean, id: Int): Boolean {
        return this.boardState.hands[playerBoolToIndex(isFirstPlayer)].contains(id)
    }

    private fun getCard(isFirstPlayer: Boolean, position: CardPosition): CardState? {
        return this.boardState.cards[playerBoolToIndex(isFirstPlayer)][position.row][position.column]
    }

    private fun setCard(isFirstPlayer: Boolean, position: CardPosition, card: CardState?) {
        this.boardState.cards[playerBoolToIndex(isFirstPlayer)][position.row][position.column] = card
    }

    private fun placeInHand(isFirstPlayer: Boolean, cardID: Int) {
        this.boardState.hands[playerBoolToIndex(isFirstPlayer)].add(cardID)
    }

    private fun removeFromHand(isFirstPlayer: Boolean, cardID: Int) {
        this.boardState.hands[playerBoolToIndex(isFirstPlayer)].remove(cardID)
    }

    private fun getRam(isFirstPlayer: Boolean): Int {
        return this.boardState.ram[playerBoolToIndex(isFirstPlayer)]
    }

    private fun getMaxRam(isFirstPlayer: Boolean): Int {
        return this.boardState.max_ram[playerBoolToIndex(isFirstPlayer)]
    }

    private fun removeRam(isFirstPlayer: Boolean, amount: Int) {
        assert(amount > 0)
        this.boardState.ram[playerBoolToIndex(isFirstPlayer)] -= amount
        assert(this.boardState.ram[playerBoolToIndex(isFirstPlayer)] in 0..getMaxRam(isFirstPlayer))
    }

    private fun refreshRam(isFirstPlayer: Boolean) {
        if (getMaxRam(isFirstPlayer) < 10) {
            this.boardState.max_ram[playerBoolToIndex(isFirstPlayer)] += 1
        }
        this.boardState.ram[playerBoolToIndex(isFirstPlayer)] = getMaxRam(isFirstPlayer)
    }

    suspend fun handleSummonPacket(packet: SummonRequestPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            getConnection(isFirstPlayer).sendPacket(
                packet.getResponsePacket(
                    is_you = true,
                    valid = false,
                    new_card = null,
                    new_ram = -1
                )
            )
            return
        }
        if (getCard(isFirstPlayer, packet.position) != null) {
            getConnection(isFirstPlayer).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    new_card = null,
                    new_ram = -1
                )
            )
            return
        }
        if (!handContains(isFirstPlayer, packet.card_id)) {
            getConnection(isFirstPlayer).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    new_card = null,
                    new_ram = -1
                )
            )
            return
        }
        if (getRam(isFirstPlayer) < CardStats.getCardByID(packet.card_id).summoning_cost) {
            getConnection(isFirstPlayer).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    new_card = null,
                    new_ram = -1
                )
            )
            return
        }

        val newCardState = CardState(packet.card_id, CardStats.getCardByID(packet.card_id).max_hp)
        setCard(
            isFirstPlayer,
            packet.position,
            newCardState
        )

        removeFromHand(isFirstPlayer, packet.card_id)
        removeRam(isFirstPlayer, CardStats.getCardByID(packet.card_id).summoning_cost)

        getConnection(isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                true,
                valid = true,
                new_card = newCardState,
                new_ram = getRam(isFirstPlayer)
            )
        )
        getConnection(!isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                is_you = false,
                valid = true,
                new_card = newCardState,
                new_ram = getRam(isFirstPlayer)
            )
        )
    }


    suspend fun handleAttackPacket(packet: AttackRequestPacket, isFirstPlayer: Boolean) {
        val sendInvalid = suspend {
            getConnection(isFirstPlayer).sendPacket(
                packet.getResponsePacket(
                    is_you = true,
                    valid = false,
                    target_card = null,
                    attacker_card = null,
                    packet.counterattack
                )
            )
        }

        if (!isTurnOfPlayer(isFirstPlayer)) {
            sendInvalid()
            return
        }

        var attacker = getCard(isFirstPlayer, packet.attacker_position)
        var target = getCard(!isFirstPlayer, packet.target_position)

        if (attacker == null || target == null) {
            sendInvalid()
            return
        }

        target.health -= CardStats.getCardByID(attacker.id).base_atk
        attacker.health -= max(CardStats.getCardByID(target.id).base_atk - 1, 0)
        if (attacker.health <= 0)
            attacker = null
        if (target.health <= 0)
            target = null

        setCard(isFirstPlayer, packet.attacker_position, attacker)
        setCard(!isFirstPlayer, packet.target_position, target)

        getConnection(isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                is_you = true,
                valid = true,
                target_card = target,
                attacker_card = attacker,
                packet.counterattack
            )
        )
        getConnection(!isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                is_you = false,
                valid = true,
                target_card = target,
                attacker_card = attacker,
                packet.counterattack
            )
        )
    }

    suspend fun handleSwitchPlacePacket(packet: SwitchPlaceRequestPacket, isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            getConnection(isFirstPlayer).sendPacket(packet.getResponsePacket(is_you = true, valid = false))
            return
        }
        if (packet.position1 == packet.position2) {
            getConnection(isFirstPlayer).sendPacket(packet.getResponsePacket(is_you = true, valid = false))
        }

        val c1 = getCard(isFirstPlayer, packet.position1)
        val c2 = getCard(!isFirstPlayer, packet.position2)

        setCard(isFirstPlayer, packet.position1, c2)
        setCard(isFirstPlayer, packet.position2, c1)

        getConnection(isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                is_you = true,
                valid = true
            )
        )
        getConnection(!isFirstPlayer).sendPacket(
            packet.getResponsePacket(
                is_you = false,
                valid = true
            )
        )
    }

    suspend fun handleEndTurn(isFirstPlayer: Boolean) {
        getConnection(!isFirstPlayer).sendPacket(StartTurnPacket())
        refreshRam(isFirstPlayer)

        boardState.first_player_active = !boardState.first_player_active
    }

    private val firstQueue = mutableListOf(2, 1, 1, 1, 0, 0)
    private val secondQueue = mutableListOf(2, 1, 0, 0, 1, 1)

    suspend fun handleDrawCard(isFirstPlayer: Boolean) {
        if (!isTurnOfPlayer(isFirstPlayer)) {
            getConnection(isFirstPlayer).sendPacket(DrawCard(-1, true))
            return
        }
        if (this.boardState.hands[playerBoolToIndex(isFirstPlayer)].size > 5) {
            getConnection(isFirstPlayer).sendPacket(DrawCard(-1, true))
            return
        }

        val cardID = if (isFirstPlayer) firstQueue.removeAt(0) else secondQueue.removeAt(0)

        placeInHand(isFirstPlayer, cardID)

        getConnection(isFirstPlayer).sendPacket(DrawCard(cardID, true))
        getConnection(!isFirstPlayer).sendPacket(DrawCard(cardID, false))
    }

    suspend fun handleUseAbilityPacket(packet: UseAbilityRequestPacket, firstPlayer: Boolean) {
        val sendInvalid = suspend {
            getConnection(firstPlayer).sendPacket(packet.getResponsePacket(is_you = true, valid = false, null, null))
        }

        if (!isTurnOfPlayer(firstPlayer)) {
            sendInvalid()
            return
        }

        val ability_card = getCard(firstPlayer, packet.ability_position)
        if (ability_card == null) {
            sendInvalid()
            return
        }

        val ability = CardStats.getCardByID(ability_card.id).ability
        when (ability.effect) {
            AbilityEffect.NONE -> TODO()
            AbilityEffect.ADD_HP_TO_ALLY_CARD -> {
                val ally = getCard(firstPlayer, packet.target_position)
                if (ally == null) {
                    sendInvalid()
                    return
                }
                assert(ability.range == AbilityRange.ALLY_CARD)

                ally.health += ability.value
                ally.health = ally.health.coerceIn(0, CardStats.getCardByID(ally.id).max_hp)
                setCard(firstPlayer, packet.target_position, ally)

                removeRam(firstPlayer, ability.cost)

                getConnection(firstPlayer).sendPacket(
                    packet.getResponsePacket(
                        is_you = true,
                        valid = true,
                        target_card = ally,
                        ability_card = ability_card
                    )
                )
                getConnection(!firstPlayer).sendPacket(
                    packet.getResponsePacket(
                        is_you = false,
                        valid = true,
                        target_card = ally,
                        ability_card = ability_card
                    )
                )
            }

            AbilityEffect.SEAL_ENEMY_CARD -> TODO()
            AbilityEffect.ATTACK -> TODO()
            AbilityEffect.ATTACK_ROW -> TODO()
        }
    }


}
