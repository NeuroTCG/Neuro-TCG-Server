package objects

import objects.packets.*
import objects.packets.objects.*
import objects.passives.*

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
    private val player1ID: TcgId = player1Connection.getUserInfo().id
    private val player2ID: TcgId = player2Connection.getUserInfo().id
    private val passiveManager: PassiveManager = PassiveManager(this)
    val gameID = db.createGame(player1ID, player2ID)

    fun getBoardState(): BoardState = this.boardState

    fun isTurnOfPlayer(player: Player): Boolean = boardState.first_player_active == (player == Player.Player1)

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

    private fun isHandFull(player: Player): Boolean = this.boardState.hands[playerToIndex(player)].size >= 5

    fun handContains(
        player: Player,
        card: Card,
    ): Boolean = findHandPositionOf(player, card) != -1

    fun findHandPositionOf(
        player: Player,
        card: Card,
    ): Int {
        for (i in 0..<this.boardState.hands[playerToIndex(player)].size) {
            val thisCard = this.boardState.hands[playerToIndex(player)][i]
            if (thisCard === card) {
                return i
            }

            if (thisCard.playerIdx == card.playerIdx && thisCard.position == card.position && thisCard.state == card.state) {
                return i
            }
        }

        return -1
    }

    fun getCard(
        player: Player,
        position: CardPosition,
    ): Card? {
        if (position.row == CardPosition.HAND) {
            return this.boardState.hands[playerToIndex(player)][position.column]
        }
        return this.boardState.cards[playerToIndex(player)][position.row][position.column]
    }

    private fun setCard(
        player: Player,
        position: CardPosition,
        card: Card?,
    ) {
        this.boardState.cards[playerToIndex(player)][position.row][position.column] = card
    }

    fun placeInHand(
        player: Player,
        cardID: Int,
    ) {
        val cardStat: CardStats = CardStats.getCardByID(cardID)!!

        val newCardState =
            CardState(
                cardID,
                cardStat.max_hp,
                false,
                CardTurnPhase.Done,
                0,
                0,
            )

        val nextHandIdx = this.boardState.hands[playerToIndex(player)].size

        val newCard =
            Card(
                playerToIndex(player),
                CardPosition(-1, nextHandIdx),
                newCardState,
            )

        this.boardState.hands[playerToIndex(player)].add(newCard)
    }

    private fun removeFromHand(
        player: Player,
        card: Card,
    ) {
        this.boardState.hands[playerToIndex(player)].remove(card)
        updateHandCardPositions(player)
    }

    private fun updateHandCardPositions(player: Player) {
        for (i: Int in 0..<boardState.hands[playerToIndex(player)].size) {
            boardState.hands[playerToIndex(player)][i].position = CardPosition(CardPosition.HAND, i)
        }
    }

    private fun getRam(player: Player): Int = this.boardState.ram[playerToIndex(player)]

    private fun getMaxRam(player: Player): Int = this.boardState.max_ram[playerToIndex(player)]

    private fun removeRam(
        player: Player,
        amount: Int,
    ) {
        require(amount > 0)
        this.boardState.ram[playerToIndex(player)] -= amount
        check(this.boardState.ram[playerToIndex(player)] in 0..getMaxRam(player)) {
            "Unexpected ram value: ${this.boardState.ram[playerToIndex(player)]}"
        }
    }

    private fun refreshRam(player: Player) {
        if (getMaxRam(player) < 10) {
            this.boardState.max_ram[playerToIndex(player)] += 1
        }
        this.boardState.ram[playerToIndex(player)] = getMaxRam(player)
    }

    private suspend fun getGameWinner(): Player? {
        if (this.boardState.deck_masters.all { (it?.run { state.health > 0 }) != false }) {
            return null
        }

        val player1 = Player.Player1
        val player2 = Player.Player2

        val deckMasterPlayer1 = this.boardState.deck_masters[playerToIndex(Player.Player1)]!!.state
        val deckMasterPlayer2 = this.boardState.deck_masters[playerToIndex(Player.Player2)]!!.state

        if (deckMasterPlayer1.health > 0 && deckMasterPlayer2.health > 0) {
            return null
        }

        return if (deckMasterPlayer1.health > deckMasterPlayer2.health) player1 else player2
    }

    fun getVersusString(): String {
        val player1Dm = boardState.deck_masters[playerToIndex(Player.Player1)]!!
        val player2Dm = boardState.deck_masters[playerToIndex(Player.Player2)]!!

        println(boardState.cards[playerToIndex(Player.Player1)][1][1])
        println(boardState.cards[playerToIndex(Player.Player2)][1][1])

        return "${CardStats.getCardByID(player1Dm.state.id)!!.name} VS. ${CardStats.getCardByID(player2Dm.state.id)!!.name}"
    }

    suspend fun handleDeckMasterRequest(
        player: Player,
        packet: DeckMasterRequestPacket,
    ): Int {
        val sendInvalid =
            suspend {
                getConnection(player).sendPacket(DeckMasterSelectedPacket(packet.response_id, false, true))
            }

        val card: CardStats? = CardStats.getCardByID(packet.card_id)

        // Check if id belongs to a valid Deck Master.
        if (card == null) {
            sendInvalid()
            return -1
        } else if (card.card_type != CardType.DECK_MASTER) {
            sendInvalid()
            return -1
        }

        return packet.card_id
    }

    suspend fun initDeckMaster(
        player: Player,
        deckMasterID: Int,
    ) {
        val dmStat = CardStats.getCardByID(deckMasterID)
        require(dmStat != null) {
            "Could not find a deck master with ID: $deckMasterID"
        }

        val deckMasterCard =
            Card(
                playerToIndex(player),
                CardPosition(1, 1),
                CardState(
                    deckMasterID,
                    dmStat.max_hp,
                    false,
                    CardTurnPhase.MoveOrAction,
                    0,
                    0,
                ),
            )

        setCard(
            player,
            deckMasterCard.position,
            deckMasterCard,
        )

        this.boardState.deck_masters[playerToIndex(player)] = deckMasterCard

        passiveManager.addPassive(deckMasterCard, player)

        getConnection(player).sendPacket(DeckMasterInitPacket(true, true, deckMasterCard.position, deckMasterCard.state))
        getConnection(!player).sendPacket(DeckMasterInitPacket(false, true, deckMasterCard.position, deckMasterCard.state))
    }

    suspend fun gameOverHandler() {
        val winner = getGameWinner() ?: return

        getConnection(winner).let {
            it.readyToPlay = false
            it.sendPacket(GameOverPacket(true))
            // it.sendPacket(DisconnectPacket(DisconnectPacket.Reason.game_over, "Game is over"))
        }
        getConnection(!winner).let {
            it.readyToPlay = false
            it.sendPacket(GameOverPacket(false))
            // it.sendPacket(DisconnectPacket(DisconnectPacket.Reason.game_over, "Game is over"))
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

        val cardStat = CardStats.getCardByID(packet.card.state.id)
        if (cardStat == null) {
            println("invalid summon: bad packet id")
            sendInvalid()
            return
        }

        if (getCard(player, packet.position) != null) {
            println("invalid summon: bad packet position")
            sendInvalid()
            return
        }

        val summoningCardIdx: Int = findHandPositionOf(player, packet.card)
        if (summoningCardIdx == -1) {
            println("invalid summon: bad card: ${packet.card}, HAND: (Size ${boardState.hands[playerToIndex(player)].size})")
            for (state: Card in boardState.hands[playerToIndex(player)]) {
                println(state)
            }
            sendInvalid()
            return
        }

        if (getRam(player) < cardStat.summoning_cost) {
            getConnection(player).sendPacket(
                packet.getResponsePacket(
                    true,
                    valid = false,
                    newCard = packet.card,
                    newRam = -1,
                ),
            )
            return
        }

        val cardToSummon: Card = boardState.hands[playerToIndex(player)][summoningCardIdx]

        cardToSummon.state.phase = if (!cardStat.tactics.contains(Tactic.NIMBLE)) CardTurnPhase.Done else CardTurnPhase.MoveOrAction
        cardToSummon.position = packet.position

        setCard(
            player,
            packet.position,
            cardToSummon,
        )

        removeFromHand(player, cardToSummon)
        removeRam(player, cardStat.summoning_cost)

        passiveManager.addPassive(cardToSummon, player)

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                true,
                valid = true,
                newCard = packet.card,
                newRam = getRam(player),
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                newCard = packet.card,
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

        var attackerState: CardState? = attacker.state
        var targetState: CardState? = target.state

        check(attackerState != null)
        check(targetState != null)

        if (attackerState.phase < CardTurnPhase.AttackOnly) {
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
                attackerState.health -=
                    (targetCardStat.base_atk + targetState.attack_bonus - 1).coerceIn(
                        targetCardStat.min_counter_attack,
                        targetCardStat.max_counter_attack,
                    )
            } else {
                attackerState.shield -= 1
            }
        }

        if (attackerState.health <= 0) {
            attacker = null
            attackerState = null
        }
        if (targetState.health <= 0) {
            target = null
            targetState = null
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

    suspend fun initPassives(player: Player) {
        val updatePacket: PassiveUpdatePacket = passiveManager.initPassives()

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

        require(attackerPosition.row in 0..1)
        require(targetPosition.row in 0..1)

        return when (attackerPosition.row to targetPosition.row) {
            CardPosition.FRONT_ROW to CardPosition.FRONT_ROW -> true
            CardPosition.FRONT_ROW to CardPosition.BACK_ROW -> isOppoonentFrontEmpty || attackerHasReach
            CardPosition.BACK_ROW to CardPosition.FRONT_ROW -> isPlayerFrontEmpty || attackerHasReach
            CardPosition.BACK_ROW to CardPosition.BACK_ROW -> attackerHasReach
            else -> {
                check(false) { "unreachable" }
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

        if ((c1 != null && c1.state.phase < CardTurnPhase.MoveOrAbility) ||
            (c2 != null && c2.state.phase < CardTurnPhase.MoveOrAbility)
        ) {
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
            drawCard(!player, null)
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

        val cardState = card.state

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

        // Refresh Deck Master's abilities
        val cardstat: CardStats = CardStats.getCardByID(cardState.id)!!

        println("End Turn For ${cardstat.name}")
        println(cardstat.card_type)

        if (cardstat.card_type == CardType.DECK_MASTER) {
            cardState.ability_was_used = false
        }
    }

    val cardDecks = listOf(CardDeck(), CardDeck())

    suspend fun handleDrawCard(
        packet: DrawCardRequestPacket,
        player: Player,
    ) {
        if (!isTurnOfPlayer(player)) {
            getConnection(player).sendPacket(DrawCard(packet.response_id, -1, true))
            return
        }
        if (this.boardState.hands[playerToIndex(player)].size > 5) {
            getConnection(player).sendPacket(DrawCard(packet.response_id, -1, true))
            return
        }

        drawCard(player, packet)
    }

    suspend fun drawCard(
        player: Player,
        packet: DrawCardRequestPacket?,
    ) {
        val cardID = cardDecks[playerToIndex(player)].drawCard()
        placeInHand(player, cardID)

        getConnection(player).sendPacket(DrawCard(packet?.response_id ?: -1, cardID, true))
        getConnection(!player).sendPacket(DrawCard(-1, cardID, false))
    }

    suspend fun useAbility(
        player: Player,
        abilityCard: Card,
        ability: Ability,
        target_position: CardPosition?,
    ): Boolean {

        //Player cannot use abilities while in a sealed state.
        if (abilityCard.state.sealed_turns_left > 0) {
            return false
        }

        when (ability.effect) {
            AbilityEffect.NONE -> TODO()
            AbilityEffect.NOT_IMPLEMENTED -> TODO()
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
                        card.state.health += ability.value // isn't capped by design
                    }
                    setCard(player, pos, card)
                }

                return true
            }

            /**
             *  TODO: Might want to add the ability to have separate values for attack and hp.
             */
            AbilityEffect.ADD_ATTACK_HP -> {
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
                        card.state.health += ability.value // isn't capped by design
                        card.state.attack_bonus += ability.value
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
                        card.state.health -= ability.value
                        if (card.state.health <= 0) {
                            card = null
                        }
                    }
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
                        it.state.shield += ability.value
                    }
                    setCard(player, pos, card)
                }

                return true
            }

            AbilityEffect.DRAW_CARD -> {
                if (ability.range != AbilityRange.PLAYER_DECK) {
                    return false
                }

                if (isHandFull(player)) {
                    return false
                }

                /*val cardID = cardDecks[playerToIndex(player)].drawCard()
                placeInHand(player, cardID)*/

                drawCard(player, null)

                return true
            }

            AbilityEffect.BUFF_SELF_REMOVE_CARD -> {
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
                if (target == null) {
                    return false
                }

                // We don't want the player to remove deck masters, so mark any attempt to do so
                // as invalid.
                if (CardStats.cardIDMapping[target.state.id]!!.card_type == CardType.DECK_MASTER) {
                    return false
                }

                if (abilityCard == null) {
                    return false
                }

                // Apply +1/+1 to self
                abilityCard.state.attack_bonus += ability.value
                abilityCard.state.health += ability.value

                /* TODO:
                    Just setting the card at that location to be null, but will need to verify that
                    this is the only thing that needs to be done.
                 */

                boardState.cards[target.playerIdx][target.position.row][target.position.column] = null

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
                getConnection(player).sendPacket(packet.getResponsePacket(isYou = true, valid = false, Ability(), packet.card))
            }

        if (!isTurnOfPlayer(player)) {
            sendInvalid()
            return
        }

        val magicCardIdx = findHandPositionOf(player, packet.card)
        if (magicCardIdx == -1) {
            println("find card failed!: ${packet.card.state} ${packet.card.position}")
            sendInvalid()
            return
        }

        val magicCard: Card = boardState.hands[playerToIndex(player)][magicCardIdx]

        val cardStat = CardStats.getCardByID(magicCard.state.id)
        if (cardStat == null) {
            println("card state failed!")
            sendInvalid()
            return
        }

        val ability = cardStat.ability

        val targetCards = useAbility(player, magicCard, ability, packet.target_position)

        if (!targetCards) {
            println("Ability use failed!")
            sendInvalid()
            return
        }

        if (getRam(player) < magicCard.state.currentAbilityCost()) {
            println("use ram failed!")
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
                target_card = target,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                ability = ability,
                target_card = target,
            ),
        )

        removeRam(player, magicCard.state.currentAbilityCost())
        removeFromHand(player, magicCard)
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

        val abilityCard = getCard(player, packet.ability_position)

        if (abilityCard != null) {
            println("Player: ${playerToIndex(player)}")
            println("Card's owner: ${abilityCard.playerIdx}")
            println("Card's name = ${CardStats.getCardByID(abilityCard.state.id)?.name}")
            println("Ability was used: ${abilityCard.state.ability_was_used}")
            println("Current turn phase: ${abilityCard.state.phase}")
        }
        
        if (abilityCard == null || abilityCard.state.phase < CardTurnPhase.Action || abilityCard.state.ability_was_used) {
            sendInvalid()
            return
        }

        val cardStat = CardStats.getCardByID(abilityCard.state.id)
        if (cardStat == null) {
            sendInvalid()
            return
        }

        val ability = cardStat.ability

        val target = getCard(player, packet.target_position)!!.state

        val targetCards = useAbility(player, abilityCard, ability, packet.target_position)

        if (!targetCards) {
            sendInvalid()
            return
        }

        if (getRam(player) < ability.cost) {
            sendInvalid()
            return
        }

        getConnection(player).sendPacket(
            packet.getResponsePacket(
                isYou = true,
                valid = true,
                targetCard = target,
                abilityCard = abilityCard.state,
            ),
        )
        getConnection(!player).sendPacket(
            packet.getResponsePacket(
                isYou = false,
                valid = true,
                targetCard = target,
                abilityCard = abilityCard.state,
            ),
        )

        removeRam(player, ability.cost)
        abilityCard.state.phase = CardTurnPhase.Done
        abilityCard.state.ability_was_used = true

        if (abilityCard != null) {
            println("Card's name = ${CardStats.getCardByID(abilityCard.state.id)?.name}")
            println("Ability was used: ${abilityCard.state.ability_was_used}")
            println("Current turn phase: ${abilityCard.state.phase}")
        }
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
            AbilityRange.PLAYER_DECK -> {}
        }
    }
}
