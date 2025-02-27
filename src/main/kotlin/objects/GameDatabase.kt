package objects

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import objects.DiscordUsers.userID
import objects.accounts.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.*
import org.jetbrains.exposed.sql.transactions.*
import java.sql.*
import java.time.*
import java.util.*

// All transactions are blocking, so use them wrapped in withContext(), if you need concurrency.
class GameDatabase(
    val dbPath: String,
) {
    private val db = Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json

    fun createTables() {
        TransactionManager.defaultDatabase = db
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction {
            SchemaUtils.create(
                CurrentGames,
                PreviousGames,
                Cards,
                DeckMasters,
                Creatures,
                MagicCards,
                TrapCards,
                Users,
                UserTokens,
                DiscordUsers,
                DevelopmentUsers,
                UserFlags,
                AdminTokens,
            )
            commit()
        }

        require(transaction { CurrentGames.selectAll().count() } == 0L, { "Not all games were finished correctly" })
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun createGame(
        player1ID: TcgId,
        player2ID: TcgId,
    ): GameId {
        val startingGameState: String =
            """
                |[
                |   [
                |       [
                |           [0], [0], [0], [0]
                |       ], 
                |       [
                |           [0], [0], [0]
                |       ], 
                |       [
                |           [0], [0]
                |       ]
                |   ],
                |   [
                |       [
                |           [0], [0], [0], [0]
                |       ], 
                |       [
                |           [0], [0], [0]
                |       ], 
                |       [
                |           [0], [0]
                |       ]
                |   ],
                |]
            """.trimMargin()

        val newGameid = UUID.randomUUID().toString()

        transaction {
            val gameId =
                CurrentGames
                    .insert {
                        it[this.game_ID] = newGameid
                        it[this.player1_ID] = player1ID.id
                        it[this.player2_ID] = player2ID.id
                        it[this.current_game_state] = startingGameState
                        it[this.incremental_moves] = ""
                    }.resultedValues!!
                    .last()[CurrentGames.game_ID]
            commit()
        }
        return GameId(newGameid)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun updateGame(
        gameID: GameId,
        givenMoveList: String? = null,
        givenGameState: String? = null,
        givenChange: String,
    ) {
        // Like: [[[Player], [field], [change]], [[Player], [field], [change]]...]
        // Not completely sure how exactly, but the idea is that we pass in a list of lists integers that represent a change in the game state.
        // Something like:
        // [
        //  [player (Either 0 or 1 for Player 1 and 2 respectfully)],
        //  [index of row (0, 1 , 2), index of column (0-3 depending on row)],
        //  [what to change the card slot to (card ID, health and all the other card parameters)]
        // ]
        // For the change, we might implement preambles or action IDs, like 100 for card ID, 101 for health, 102 for status, etc.
        // That way, we won't have to replace the whole card info every time.
        // But, as of now, we only replace the whole info.
        transaction {
            val currentGameState: MutableList<MutableList<MutableList<MutableList<Int>>>> =
                if (givenGameState.isNullOrEmpty()) {
                    json.decodeFromString<MutableList<MutableList<MutableList<MutableList<Int>>>>>(
                        CurrentGames
                            .select(
                                CurrentGames.current_game_state,
                            ).where { CurrentGames.game_ID eq gameID.id }
                            .firstOrNull() as String,
                    )
                } else {
                    json.decodeFromString<MutableList<MutableList<MutableList<MutableList<Int>>>>>(givenGameState)
                }
            val moveList: MutableList<MutableList<Int>> =
                if (givenMoveList.isNullOrEmpty()) {
                    json.decodeFromString<MutableList<MutableList<Int>>>(
                        CurrentGames
                            .select(CurrentGames.incremental_moves)
                            .where { CurrentGames.game_ID eq gameID.id }
                            .firstOrNull() as String,
                    )
                } else {
                    json.decodeFromString<MutableList<MutableList<Int>>>(givenMoveList)
                }
            // givenGameState and givenMoveList are just there for better performance. If they are null, we will get the game state and move list from the database and decode them using CBOR.
            // If they are passed in, however, the server will use them instead and won't have to waste time and resources decoding them from the database every turn.
            val change: MutableList<MutableList<Int>> =
                json.decodeFromString<MutableList<MutableList<Int>>>(givenChange)
            val newMoveList = moveList + change
            CurrentGames.update({ CurrentGames.game_ID eq gameID.id }) {
                it[this.incremental_moves] = newMoveList.toString()
            }
            // Player row column
            currentGameState[change[0][0]][change[1][0]][change[1][1]] =
                change[2] // TODO: Gotta figure out how we want to handle changes. For now, it's a List of Integers.
            CurrentGames.update({ CurrentGames.game_ID eq gameID.id }) {
                it[this.current_game_state] = currentGameState.toString()
            }
            commit()
        }
    }

    fun moveGameToArchive(gameID: GameId) {
        transaction {
            val game = CurrentGames.selectAll().where { CurrentGames.game_ID eq gameID.id }.singleOrNull()
            require(game != null)

            PreviousGames.insert {
                it[this.game_ID] = game[CurrentGames.game_ID]
                it[this.player1_ID] = game[CurrentGames.player1_ID]
                it[this.player2_ID] = game[CurrentGames.player2_ID]
                it[this.current_game_state] = game[CurrentGames.current_game_state]
                it[this.incremental_moves] = game[CurrentGames.incremental_moves]
            }

            CurrentGames.deleteWhere { CurrentGames.game_ID eq gameID.id }
            commit()
        }
    }

    fun getUserByDiscordId(discordId: DiscordId): TcgId? {
        val result =
            transaction {
                (Users innerJoin DiscordUsers)
                    .select(Users.userId, DiscordUsers.linkedTo)
                    .where {
                        userID.eq(discordId.id)
                    }.singleOrNull()
            }

        if (result == null) {
            return null
        } else {
            return TcgId(result[Users.userId])
        }
    }

    fun createNewUser(authProvider: AuthProviderName): TcgId {
        val newUserId = UUID.randomUUID().toString()

        transaction {
            Users.insert {
                // TODO: User id generation should probably live in some kind of singleton
                it[this.userId] = newUserId
                it[this.providerName] = authProvider.name
            }[Users.userId]

            commit()
        }

        return TcgId(newUserId)
    }

    fun generateTokenFor(tcgUserId: TcgId): Token? {
        // TODO: this is 100% not a good enough token
        val token = UUID.randomUUID().toString()

        val success =
            transaction {
                // invalidate all previous tokens
                UserTokens.deleteWhere { (UserTokens.userId eq tcgUserId.id) }

                val success =
                    UserTokens.insert {
                        it[this.userId] = tcgUserId.id
                        it[UserTokens.token] = token
                    }

                commit()

                success
            }

        if (success.insertedCount == 0) {
            return null
        }

        return Token(token)
    }

    // TODO: this might be better with `discordUserInfo` as a different class to decouple it from `DiscordLoginProvider`
    // TODO: same with `discordTokenInfo`
    fun createLinkedDiscordInfo(
        discordUserInfo: DiscordLoginProvider.DiscordOauthUserInfo,
        discordTokenInfo: DiscordLoginProvider.DiscordOauthTokenResponse,
        tcgUserId: TcgId,
    ) {
        transaction {
            DiscordUsers.insert {
                it[this.linkedTo] = tcgUserId.id
                it[this.userID] = discordUserInfo.id
                it[this.accessToken] = discordTokenInfo.accessToken
                it[this.accessTokenExpiry] = LocalDateTime.now().plusSeconds(discordTokenInfo.expiresIn.toLong())
                it[this.refreshToken] = discordTokenInfo.refreshToken
            }

            commit()
        }
    }

    fun updateDiscordUserInfo(
        discordUserInfo: DiscordLoginProvider.DiscordOauthUserInfo,
        discordTokenInfo: DiscordLoginProvider.DiscordOauthTokenResponse,
    ) {
        transaction {
            DiscordUsers.update({ DiscordUsers.userID.eq(discordUserInfo.id) }) {
                it[this.userID] = discordUserInfo.id
                it[this.accessToken] = discordTokenInfo.accessToken
                it[this.accessTokenExpiry] = LocalDateTime.now().plusSeconds(discordTokenInfo.expiresIn.toLong())
                it[this.refreshToken] = discordTokenInfo.refreshToken
            }

            commit()
        }
    }

    fun getUserIdFromToken(token: Token): TcgId? {
        val col =
            transaction {
                UserTokens
                    .selectAll()
                    .where(UserTokens.token.eq(token.token))
                    .singleOrNull()
            }

        if (col == null) {
            return null
        }

        return TcgId(col[UserTokens.userId])
    }

    fun getUserByDevelopmentId(
        id: DevelopmentId,
        ownerId: TcgId,
    ): TcgId? {
        val result =
            transaction {
                (Users.innerJoin(DevelopmentUsers, { DevelopmentUsers.linkedToId }, { Users.userId }))
                    .select(Users.userId)
                    .where {
                        (DevelopmentUsers.developmentId eq id.id) and (DevelopmentUsers.ownedById eq ownerId.id)
                    }.singleOrNull()
            }

        if (result == null) {
            return null
        } else {
            return TcgId(result[Users.userId])
        }
    }

    fun createLinkedDevelopmentInfo(
        devUserId: DevelopmentId,
        ownerId: TcgId,
        userId: TcgId,
    ) {
        transaction {
            DevelopmentUsers.insert {
                it[this.linkedToId] = userId.id
                it[this.ownedById] = ownerId.id
                it[this.developmentId] = devUserId.id
            }

            commit()
        }
    }

    // TODO: this also returns false if the userId is invalid, which sucks
    // TODO: it would be better if this function returned a nullable boolean or something
    fun userHasFlag(
        userId: TcgId,
        flag: Flag,
    ): Boolean? =
        transaction {
            Users
                .innerJoin(
                    UserFlags,
                    { UserFlags.userId },
                    { Users.userId },
                ).select(Users.userId)
                .where { (UserFlags.flag eq flag.flag) and (UserFlags.userId eq userId.id) }
                .singleOrNull()
        } != null

    fun userSetFlag(
        userId: TcgId,
        flag: Flag,
    ) {
        transaction {
            UserFlags.insert {
                it[this.userId] = userId.id
                it[this.flag] = flag.flag
            }

            commit()
        }
    }

    fun userUnsetFlag(
        userId: TcgId,
        flag: Flag,
    ) {
        transaction {
            UserFlags.deleteWhere { (UserFlags.userId eq userId.id) and (UserFlags.flag eq flag.flag) }
        }
    }

    fun userListFlags(userId: TcgId): List<Flag> =
        transaction {
            UserFlags
                .selectAll()
                .where { UserFlags.userId eq userId.id }
                .sortedBy { UserFlags.flag }
                .map { Flag(it[UserFlags.flag]) }
        }

    fun checkToken(token: Token): Boolean =
        transaction {
            UserTokens.selectAll().where { UserTokens.token eq token.token }.singleOrNull()
        } != null

    fun checkAdminToken(token: Token): Boolean =
        transaction {
            AdminTokens.selectAll().where { AdminTokens.token eq token.token }.singleOrNull()
        } != null

    fun getAdminTokenComment(token: Token): String? {
        val result =
            transaction {
                AdminTokens.selectAll().where { AdminTokens.token eq token.token }.singleOrNull()
            }

        if (result == null) {
            return null
        }

        return result[AdminTokens.comment]
    }

    fun userGetCurrentGame(userId: TcgId): GameId? {
        val result =
            transaction {
                CurrentGames
                    .selectAll()
                    .where {
                        (CurrentGames.player1_ID eq userId.id) or (CurrentGames.player2_ID eq userId.id)
                    }.singleOrNull()
            }

        if (result == null) {
            return null
        }

        return GameId(result[CurrentGames.game_ID])
    }
}

object CurrentGames : Table() {
    val game_ID: Column<String> = text("game_id")
    val player1_ID: Column<String> = reference("player1", Users.userId)
    val player2_ID: Column<String> = reference("player2", Users.userId)
    val current_game_state: Column<String> = text("current_game_state") // Array as a String. Parse with JSON.
    val incremental_moves: Column<String> = text("incremental_moves") // same here.

    override val primaryKey = PrimaryKey(game_ID)
}

object PreviousGames : Table() {
    val game_ID: Column<String> = text("game_id")
    val player1_ID: Column<String> = reference("player1", Users.userId)
    val player2_ID: Column<String> = reference("player2", Users.userId)
    val current_game_state: Column<String> = text("current_game_state") // Array as a String. Parse with JSON.
    val incremental_moves: Column<String> = text("incremental_moves") // same here.

    override val primaryKey = PrimaryKey(game_ID)
}

// For now, store tactics as enums. We will change it to a few bits later.
// Superpower, passive and abilities should be parsed separately. We should just give them a name or an ID.
object Cards : Table("cards") {
    val card_ID: Column<Int> = integer("card_id").autoIncrement()
    val cardName: Column<String> = varchar("card_name", 100)
    val cardType: Column<String> = varchar("card_type", 100)

    override val primaryKey = PrimaryKey(card_ID)
}

object DeckMasters : Table("deck_masters") {
    val card_ID: Column<Int> = reference("card_id", Cards.card_ID)
    val hp: Column<Short> = short("hp")
    val attack: Column<Short> = short("attack")
    val abilityRamCost: Column<Short> = short("ability_ram_cost")
    val passive: Column<String> = text("passive")
    val superPower: Column<String> = text("super_power")
    val tactics: Column<DeckMasterTactics> = enumeration("tactics", DeckMasterTactics::class)

    override val primaryKey = PrimaryKey(card_ID)
}

enum class DeckMasterTactics {
    REACH,
    NIMBLE,
    BOTH,
}

object Creatures : Table("creatures") {
    val card_ID: Column<Int> = reference("card_id", Cards.card_ID)
    val ramCost: Column<Short> = short("ram_cost")
    val attack: Column<Short> = short("attack")
    val hp: Column<Short> = short("hp")
    val passive: Column<String> = text("passive")
    val tactics: Column<CreatureTactics> = enumeration("tactics", CreatureTactics::class)
}

enum class CreatureTactics {
    REACH,
    NIMBLE,
    BOTH,
}

object MagicCards : Table("magic_cards") {
    val card_ID: Column<Int> = reference("card_id", Cards.card_ID)
    val cost: Column<Short> = short("cost")
    val deltaHp: Column<Short> = short("delta_hp")
    val attackRow: Column<Byte> = byte("attack_row")
    val effect: Column<String> = text("effect")
}

object TrapCards : Table("trap_cards") {
    val card_ID: Column<Int> = reference("card_id", Cards.card_ID)
    val cost: Column<Short> = short("cost")
    val deltaHp: Column<Short> = short("delta_hp")
    val condition: Column<String> = text("condition")
    val attackRow: Column<Byte> = byte("attack_row")
}

object Users : Table("users") {
    // TODO: ids probably won't 128ch long, but I don't know what else to put
    val userId: Column<String> = text("user_id")
    val providerName: Column<String> = text("provider_name")

    override val primaryKey = PrimaryKey(userId)
}

object UserTokens : Table("user_tokens") {
    // TODO: same as above in regards to length
    val userId: Column<String> = reference("user_id", Users.userId)
    val token: Column<String> = text("token")
}

object DiscordUsers : Table("discord_users") {
    val linkedTo: Column<String> = reference("linked_to_user_id", Users.userId)
    val userID: Column<String> = text("discord_user_id")
    val accessToken: Column<String> = text("access_token")
    val accessTokenExpiry: Column<LocalDateTime> = datetime("access_token_expiry")
    val refreshToken: Column<String> = text("refresh_token")

    override val primaryKey = PrimaryKey(linkedTo)
}

object DevelopmentUsers : Table("development_users") {
    val linkedToId: Column<String> = reference("linked_to_user_id", Users.userId)
    val ownedById: Column<String> = reference("owner_user_id", Users.userId)
    val developmentId: Column<String> = text("development_id")

    override val primaryKey = PrimaryKey(linkedToId)
}

object UserFlags : Table("user_flags") {
    val userId: Column<String> = reference("user_id", Users.userId)
    val flag: Column<String> = text("flag")

    override val primaryKey = PrimaryKey(userId, flag)
}

object AdminTokens : Table("admin_tokens") {
    val token: Column<String> = text("token")
    val comment: Column<String> = text("comment")

    override val primaryKey = PrimaryKey(token)
}

// These all get inlined when serialized because they are value classes

@JvmInline
@Serializable
value class TcgId(
    val id: String,
)

@JvmInline
@Serializable
value class DiscordId(
    val id: String,
)

@JvmInline
@Serializable
value class DevelopmentId(
    val id: String,
)

@JvmInline
@Serializable
value class Token(
    val token: String,
)

@JvmInline
@Serializable
value class Flag(
    val flag: String,
)

@JvmInline
@Serializable
value class AuthProviderName(
    val name: String,
)

@JvmInline
@Serializable
value class GameId(
    val id: String,
)
