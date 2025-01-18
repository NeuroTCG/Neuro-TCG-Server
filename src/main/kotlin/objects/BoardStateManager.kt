package objects

import objects.packets.*
import objects.packets.objects.*
import objects.passives.*
import kotlin.math.*

enum class Player {
    Player1,
    Player2,
    ;

    operator fun not(): Player =
        if (this == Player1) {
            Player2
        } else {
            Player1
        }
}

class BoardStateManager(
    private val db: GameDatabase,
    private val player1Connection: GameConnection,
    private val player2Connection: GameConnection,
) {
    private var boardState = BoardState()
    private val player1ID: Int = 1
    private val player2ID: Int = 2
    private val passiveManager: PassiveManager = PassiveManager(this)
    val gameID = db.createGame(player1ID, player2ID)

    fun getBoardState(): BoardState = this.boardState

    private fun isTurnOfPlayer(player: Player): Boolean = boardState.first_player_active == (player == Player.Player1)

    private fun playerToIndex(player: Player): Int =
        if (player == Player.Player1) {
            0
        } else {
            1
        }

    private fun getConnection(player: Player): GameConnection =
        if (player == Player.Player1) {
            player1Connection
        } else {
            player2Connection
        }

    private fun handContains(
        player: Player,
        id: Int,
    ): Boolean = this.boardState.hands[playerToIndex(player)].contains(id)

    fun getCard(
        player: Player,
        position: CardPosition,
    ): CardData? = this.boardState.cards[playerToIndex(player)][position.row][position.column]

    private fun setCard(
        player: Player,
        position: CardPosition,
        cardData: CardData?,
    ) {
        this.boardState.cards[playerToIndex(player)][position.row][position.column] = cardData
    }

    fun placeInHand(
        player: Player,
        cardID: Int,
    ) {
        this.boardState.hands[playerToIndex(player)].add(cardID)
    }

    private fun removeFromHand(
        player: Player,
        cardID: Int,
    ) {
        this.boardState.hands[playerToIndex(player)].remove(cardID)
    }

    private fun getRam(player: Player): Int = this.boardState.ram[playerToIndex(player)]

    private fun getMaxRam(player: Player): Int = this.boardState.max_ram[playerToIndex(player)]

    private fun removeRam(
        player: Player,
        amount: Int,
    ) {
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

    // TODO: remove this function after deck masters are no longer null
    // This is here to encapsulate code that doesn't need to be in the
    // final server
    private suspend fun isGameOverTemporarySpecialLogic(
        deckMasterPlayer1: CardState?,
        deckMasterPlayer2: CardState?,
    ): Player? {
        val player1Died = (deckMasterPlayer1?.run { health < 0 }) ?: false
        val player2Died = (deckMasterPlayer2?.run { health < 0 }) ?: false
        if (player1Died && player2Died) {
            return null
        }

        if (player1Died) {
            return Player.Player2
        }

        if (player2Died) {
            return Player.Player1
        }

        return null
    }

    private suspend fun getGameWinner(): Player? {
        if (this.boardState.deck_masters.all { (it?.run { health > 0 }) != false }) {
            return null
        }

        val player1 = Player.Player1
        val player2 = Player.Player2

        // TODO: These are temporary after we consider that deck_masters must exist
        // TODO: Hence, when they do exist, remove the next few lines
        val deckMasterPlayer1Opt = this.boardState.deck_masters[playerToIndex(Player.Player1)]
        val deckMasterPlayer2Opt = this.boardState.deck_masters[playerToIndex(Player.Player2)]
        val temporarySpecialGameOver =
            isGameOverTemporarySpecialLogic(deckMasterPlayer1Opt, deckMasterPlayer2Opt)

        if (temporarySpecialGameOver != null) {
            return temporarySpecialGameOver
        }

        // NOTE: at this point, deckMasterPlayer1 & 2 are guaranteed to exist
        val deckMasterPlayer1 = deckMasterPlayer1Opt!!
        val deckMasterPlayer2 = deckMasterPlayer2Opt!!

        if (deckMasterPlayer1.health > 0 && deckMasterPlayer2.health > 0) {
            return null
        }

        return if (deckMasterPlayer1.health > deckMasterPlayer2.health) player1 else player2
    }

    suspend fun gameOverHandler() {
        val winner = getGameWinner()
        if (winner == null) {
            return
        }

        getConnection(winner).let {
            it.sendPacket(GameOverPacket(true))
            it.sendPacket(DisconnectPacket(DisconnectPacket.Reason.game_over, "Game is over"))
        }
        getConnection(!winner).let {
            it.sendPacket(GameOverPacket(false))
            it.sendPacket(DisconnectPacket(DisconnectPacket.Reason.game_over, "Game is over"))
        }
    }

    suspend fun handleSummonPacket(
        packet: SummonRequestPacket,
        player: Player,
    ) {
        val sendInvalid =
            suspend {
                getConnection(player).sendPacket(
                    packet.getResponsePacket(
                        isYou = true,
                        valid = false,
                        newCard = null,
                        newRam = -1,
                    ),
                )
            }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }
        if (getCard(player, packet.position) != null) {
            sendInvalid()
            return
        }
        if (!handContains(player, packet.card_id)) {
            sendInvalid()
            return
        }

        val cardStat = CardStats.getCardByID(packet.card_id)
        if (cardStat == null) {
            sendInvalid()
            return
        }

        if (getRam(player) < cardStat.summoning_cost) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    newCard = null,
                    newRam = -1,
                ),
            )
            return
        }

        val newCardState =
            CardState(
                packet.card_id,
                cardStat.max_hp,
                false,
                if (cardStat.has_summoning_sickness) CardTurnPhase.Done else CardTurnPhase.MoveOrAction,
                0,
                0,
            )

        val newCardData =
            CardData(
                playerToIndex(player),
                packet.position,
                newCardState,
            )

        setCard(
            player,
            packet.position,
            newCardData,
        )

        removeFromHand(player, packet.card_id)
        removeRam(player, cardStat.summoning_cost)
        passiveManager.addPassive(newCardData, player)

        // If it's a deck master, we put it in the board state
        if (cardStat.card_type == CardType.DECK_MASTER) {
            this.boardState.deck_masters[playerToIndex(player)] = newCardState
        }

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                true,
                valid = true,
                newCard = newCardState,
                newRam = getRam(player),
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                newCard = newCardState,
                newRam = getRam(player),
            ),
        )
    }

    suspend fun handleAttackPacket(
        packet: AttackRequestPacket,
        player: Player,
    ) {
        val sendInvalid =
            suspend {
                getConnection(player).sendPacket(
                    packet.getResponsePacket(
                        isYou = true,
                        valid = false,
                        targetCard = null,
                        attackerCard = null,
                    ),
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

        val attackerState = attacker.state
        val targetState = target.state

        if (attackerState.phase < CardTurnPhase.Action) {
            sendInvalid()
            return
        }

        if (!isSlotReachable(player, packet.attacker_position, packet.target_position)) {
            sendInvalid()
            return
        }

        val canAttackBack = isSlotReachable(!player, packet.target_position, packet.attacker_position)

        attackerState.phase = CardTurnPhase.Done

        val attackerCardStat = CardStats.getCardByID(attackerState.id)
        val targetCardStat = CardStats.getCardByID(targetState.id)
        if (attackerCardStat == null || targetCardStat == null) {
            sendInvalid()
            return
        }

        if (targetState.shield == 0) {
            targetState.health -= (attackerCardStat.base_atk + attackerState.attack_bonus)
        } else {
            targetState.shield -= 1
        }

        // NOTE: if the game is already over, we don't have to process anything else
        if (getGameWinner() != null) {
            return
        }

        if (canAttackBack) {
            if (attackerState.shield == 0) {
                attackerState.health -= max(targetCardStat.base_atk + targetState.attack_bonus - 1, 0)
            } else {
                attackerState.shield -= 1
            }
        }

        if (attackerState.health <= 0) {
            attacker = null
        }
        if (targetState.health <= 0) {
            target = null
        }

        setCard(player, packet.attacker_position, attacker)
        setCard(!player, packet.target_position, target)

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
                targetCard = targetState,
                attackerCard = attackerState,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                targetCard = targetState,
                attackerCard = attackerState,
            ),
        )
    }

    suspend fun updatePassives(
        packet: Packet?,
        player: Player,
    ) {
        if (packet == null) {
            return
        }

        val updatePacket: PassiveUpdatePacket = passiveManager.updatePassives(packet)

        getConnection(player).sendPacket(updatePacket)
        getConnection(!player).sendPacket(updatePacket)
    }

    private fun isSlotReachable(
        player: Player,
        attackerPosition: CardPosition,
        targetPosition: CardPosition,
    ): Boolean {
        val isPlayerFrontEmpty =
            getCard(player, CardPosition(CardPosition.FRONT_ROW, 0)) == null &&
                getCard(player, CardPosition(CardPosition.FRONT_ROW, 1)) == null &&
                getCard(player, CardPosition(CardPosition.FRONT_ROW, 2)) == null &&
                getCard(player, CardPosition(CardPosition.FRONT_ROW, 3)) == null

        val isOppoonentFrontEmpty =
            getCard(!player, CardPosition(CardPosition.FRONT_ROW, 0)) == null &&
                getCard(!player, CardPosition(CardPosition.FRONT_ROW, 1)) == null &&
                getCard(!player, CardPosition(CardPosition.FRONT_ROW, 2)) == null &&
                getCard(!player, CardPosition(CardPosition.FRONT_ROW, 3)) == null

        val cardStat = CardStats.getCardByID(getCard(player, attackerPosition)!!.state.id)
        if (cardStat == null) return false

        val attackerHasReach = cardStat.tactics.contains(Tactic.REACH)

        assert(attackerPosition.row in 0..1)
        assert(targetPosition.row in 0..1)

        return when (attackerPosition.row to targetPosition.row) {
            CardPosition.FRONT_ROW to CardPosition.FRONT_ROW -> true
            CardPosition.FRONT_ROW to CardPosition.BACK_ROW -> isOppoonentFrontEmpty || attackerHasReach
            CardPosition.BACK_ROW to CardPosition.FRONT_ROW -> isPlayerFrontEmpty || attackerHasReach
            CardPosition.BACK_ROW to CardPosition.BACK_ROW -> attackerHasReach
            else -> {
                assert(false) { "unreachable" }
                false
            }
        }
    }

    suspend fun handleSwitchPlacePacket(
        packet: SwitchPlaceRequestPacket,
        player: Player,
    ) {
        val sendInvalid =
            suspend {
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
        val c2 = getCard(player, packet.position2)

        if ((c1 != null && c1.state.phase < CardTurnPhase.MoveOrAction) || (c2 != null && c2.state!!.phase < CardTurnPhase.MoveOrAction)) {
            sendInvalid()
            return
        }

        c1?.state?.phase = CardTurnPhase.Action
        c2?.state?.phase = CardTurnPhase.Action

        setCard(player, packet.position1, c2)
        setCard(player, packet.position2, c1)

        c2?.position = packet.position1
        c1?.position = packet.position2

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
            ),
        )
    }

    suspend fun handleEndTurn(player: Player) {
        if (!isTurnOfPlayer(player)) {
            return
        }

        refreshRam(player)

        foreachSlot(player, ::endTurnForCard)

        boardState.first_player_active = !boardState.first_player_active
        // we are now in the new turn

        foreachSlot(!player, ::startTurnForCard)

        getConnection(!player).sendPacket(StartTurnPacket())
        if (this.boardState.hands[playerToIndex(!player)].size < 5) {
            drawCard(!player)
        }
    }

    private fun foreachSlot(
        player: Player,
        f: (Player, CardPosition) -> Unit,
    ) {
        for (i in 0..<4) {
            f(player, CardPosition(CardPosition.FRONT_ROW, i))
        }
        for (i in 0..<3) {
            f(player, CardPosition(CardPosition.BACK_ROW, i))
        }
    }

    private fun endTurnForCard(
        player: Player,
        position: CardPosition,
    ) {
        val card = getCard(player, position)
        if (card == null) {
            return
        }

        val cardState = card.state!!

        if (cardState.sealed_turns_left > 0) {
            cardState.phase = CardTurnPhase.Done
        } else {
            cardState.phase = CardTurnPhase.MoveOrAction
        }
    }

    private fun startTurnForCard(
        player: Player,
        position: CardPosition,
    ) {
        val card = getCard(player, position)
        if (card == null) {
            return
        }

        val cardState = card.state

        cardState.sealed_turns_left = maxOf(0, cardState.sealed_turns_left - 1)
        if (cardState.sealed_turns_left > 0) {
            cardState.phase = CardTurnPhase.Done
        } else {
            cardState.phase = CardTurnPhase.MoveOrAction
        }
    }

    val cardDecks = listOf(CardDeck(), CardDeck())

    suspend fun handleDrawCard(player: Player) {
        if (!isTurnOfPlayer(player)) {
            getConnection(player).sendPacket(DrawCard(-1, true))
            return
        }
        if (this.boardState.hands[playerToIndex(player)].size > 5) {
            getConnection(player).sendPacket(DrawCard(-1, true))
            return
        }

        drawCard(player)
    }

    suspend fun drawCard(player: Player) {
        val cardID = cardDecks[playerToIndex(player)].drawCard()

        placeInHand(player, cardID)

        getConnection(player).sendPacket(DrawCard(cardID, true))
        getConnection(!player).sendPacket(DrawCard(cardID, false))
    }

    suspend fun useAbility(
        player: Player,
        ability: Ability,
        target_position: CardPosition?,
    ): Boolean {
        when (ability.effect) {
            AbilityEffect.NONE -> TODO()
            AbilityEffect.ADD_HP -> {
                if (ability.range != AbilityRange.ALLY_CARD && ability.range != AbilityRange.ALLY_FIELD) {
                    return false
                }

                if (target_position == null) {
                    return false
                }

                val ally = getCard(player, target_position)
                if (ally == null && ability.range == AbilityRange.ALLY_CARD) {
                    return false
                }

                foreachInRange(player, target_position, ability.range) { p, pos ->
                    val card = getCard(p, pos)
                    if (card != null) {
                        card.state!!.health += ability.value // isn't capped by design
                    }
                    setCard(player, pos, card)
                }

                return true
            }

            AbilityEffect.SEAL -> {
                if (!arrayOf(
                        AbilityRange.ENEMY_ROW,
                        AbilityRange.ENEMY_FIELD,
                        AbilityRange.ENEMY_CARD,
                    ).contains(ability.range)
                ) {
                    return false
                }

                if (target_position == null) {
                    return false
                }

                val target = getCard(!player, target_position)
                if (target == null && ability.range == AbilityRange.ENEMY_CARD) {
                    return false
                }

                foreachInRange(player, target_position, ability.range) { p, pos ->
                    val card = getCard(p, pos)
                    if (card != null) {
                        card.state.sealed_turns_left = ability.value
                        card.state.phase = CardTurnPhase.Done // is renewed in startTurnForCard
                    }
                    setCard(p, pos, card)
                }

                return true
            }

            AbilityEffect.ATTACK -> {
                if (!arrayOf(
                        AbilityRange.ENEMY_ROW,
                        AbilityRange.ENEMY_FIELD,
                        AbilityRange.ENEMY_CARD,
                    ).contains(ability.range)
                ) {
                    return false
                }

                if (target_position == null) {
                    return false
                }

                val target = getCard(!player, target_position)
                if (target == null && ability.range == AbilityRange.ENEMY_CARD) {
                    return false
                }

                foreachInRange(player, target_position, ability.range) { p, pos ->
                    var card = getCard(p, pos)
                    if (card != null) {
                        card.state!!.health -= ability.value
                        if (card.state!!.health <= 0) {
                            card = null
                        }
                    }
                    setCard(player, pos, card)
                }

                return true
            }

            AbilityEffect.SHIELD -> {
                if (!arrayOf(
                        AbilityRange.ALLY_FIELD,
                        AbilityRange.ALLY_CARD,
                    ).contains(ability.range)
                ) {
                    return false
                }

                if (target_position == null) {
                    return false
                }

                var target = getCard(player, target_position)
                if (target == null && ability.range == AbilityRange.ENEMY_CARD) {
                    return false
                }

                foreachInRange(player, target_position, ability.range) { p, pos ->
                    val card = getCard(p, pos)
                    card?.let {
                        it.state.shield += 1
                    }
                    setCard(player, pos, card)
                }

                return true
            }
        }
    }

    suspend fun handleUseMagicCardPacket(
        packet: UseMagicCardRequestPacket,
        player: Player,
    ) {
        val sendInvalid =
            suspend {
                getConnection(player).sendPacket(packet.getResponsePacket(isYou = true, valid = false, null, null))
            }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }

        if (!handContains(player, packet.card_id)) {
            sendInvalid()
            return
        }

        val cardStat = CardStats.getCardByID(packet.card_id)
        if (cardStat == null) {
            sendInvalid()
            return
        }

        val ability = cardStat.ability
        val targetCards = useAbility(player, ability, packet.target_position)

        if (!targetCards) {
            sendInvalid()
            return
        }

        if (getRam(player) < ability.cost) {
            sendInvalid()
            return
        }

        val target =
            packet.target_position?.let {
                getCard(!player, it)
            }

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
                ability = ability,
                target_card = target?.state,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                ability = ability,
                target_card = target?.state,
            ),
        )

        removeRam(player, ability.cost)
    }

    suspend fun handleUseAbilityPacket(
        packet: UseAbilityRequestPacket,
        player: Player,
    ) {
        val sendInvalid =
            suspend {
                getConnection(player).sendPacket(packet.getResponsePacket(isYou = true, valid = false, null, null))
            }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }

        val abilityCard = getCard(player, packet.ability_position)?.state
        if (abilityCard == null || abilityCard.phase < CardTurnPhase.Action || abilityCard.ability_was_used) {
            sendInvalid()
            return
        }

        val cardStat = CardStats.getCardByID(abilityCard.id)
        if (cardStat == null) {
            sendInvalid()
            return
        }

        val ability = cardStat.ability
        val targetCards = useAbility(player, ability, packet.target_position)

        if (!targetCards) {
            sendInvalid()
            return
        }

        if (getRam(player) < ability.cost) {
            sendInvalid()
            return
        }

        val target = getCard(player, packet.target_position)!!.state

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
                targetCard = target,
                abilityCard = abilityCard,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                targetCard = target,
                abilityCard = abilityCard,
            ),
        )

        removeRam(player, ability.cost)
        abilityCard.phase = CardTurnPhase.Done
        abilityCard.ability_was_used = true
    }

    private fun foreachInRange(
        player: Player,
        target: CardPosition,
        range: AbilityRange,
        f: (Player, CardPosition) -> Unit,
    ) {
        when (range) {
            AbilityRange.NONE -> {}
            AbilityRange.ALLY_FIELD -> foreachSlot(player, f)
            AbilityRange.ENEMY_FIELD -> foreachSlot(!player, f)
            AbilityRange.ALLY_CARD -> f(player, target)
            AbilityRange.ENEMY_CARD -> f(!player, target)
            AbilityRange.ENEMY_ROW -> {
                if (target.row == CardPosition.BACK_ROW) {
                    for (i in 0..<3) {
                        f(!player, CardPosition(CardPosition.BACK_ROW, i))
                    }
                } else {
                    for (i in 0..<4) {
                        f(!player, CardPosition(CardPosition.FRONT_ROW, i))
                    }
                }
            }

            AbilityRange.PLAYER_DECK -> TODO()
        }
    }
}
