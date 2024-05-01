package objects
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.sql.Connection
import java.util.*

// All transactions are blocking, so use them wrapped in withContext(), if you need concurrency.
class GameDatabase {
    private val db = Database.connect("jdbc:sqlite:/data/data.db", "org.sqlite.JDBC")
    @OptIn(ExperimentalSerializationApi::class)
    private val cbor = Cbor
    fun createTables() {
        TransactionManager.defaultDatabase = db
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        SchemaUtils.createMissingTablesAndColumns(CurrentGames, PreviousGames)
    }
    @OptIn(ExperimentalSerializationApi::class)
    fun createGame (player1ID: Int, player2ID: Int) {
        val startingGameState: MutableList<MutableList<MutableList<Int>>> =
            mutableListOf(
                    mutableListOf(mutableListOf(0), mutableListOf(0), mutableListOf(0), mutableListOf(0)), // Row 1
                    mutableListOf(mutableListOf(0), mutableListOf(0), mutableListOf(0)), // Row 2
                    mutableListOf(mutableListOf(0), mutableListOf(0)) // Trap cards
            )
        transaction {
            CurrentGames.insert {
                it[player1_ID] = player1ID
                it[player2_ID] = player2ID
                it[current_game_state] = cbor.encodeToByteArray(startingGameState)
                it[incremental_moves] = cbor.encodeToByteArray(mutableListOf<MutableList<MutableList<Int?>?>?>())
            }
        }
    }
    @OptIn(ExperimentalSerializationApi::class)
    fun updateGame (gameID: UUID, givenMoveList: MutableList<MutableList<MutableList<Int>>>? = null, givenGameState: MutableList<MutableList<MutableList<Int>>>? = null, change: MutableList<MutableList<Int>>) {
        // Like: [[Change 1, from, to, ...], [Change 2, ...]]
        // Not completely sure how exactly, but the idea is that we pass in a list of lists integers that represent a change in the game state.
        // Something like: [[index of row (0, 1 , 2), index of column (0-3 depending on row)], [what to change the card slot to (card ID, health and all the other card parameters)]]
        // For the change, we might implement preambles or action IDs, like 100 for card ID, 101 for health, 102 for status, etc.
        // That way, we won't have to replace the whole card info every time.
        // But, as of now, we only replace the whole info.
        transaction {
            val currentGameState: MutableList<MutableList<MutableList<Int>>> = if (givenGameState.isNullOrEmpty()) {
                cbor.decodeFromByteArray(CurrentGames.select(CurrentGames.current_game_state).where { CurrentGames.game_ID eq gameID }.firstOrNull() as ByteArray)
            }
            else {
                givenGameState
            }
            val moveList: MutableList<MutableList<MutableList<Int>>> = if (givenMoveList.isNullOrEmpty()) {
                cbor.decodeFromByteArray(CurrentGames.select(CurrentGames.incremental_moves).where { CurrentGames.game_ID eq gameID }.firstOrNull() as ByteArray)
            } else {
                givenMoveList
            }
            // givenGameState and givenMoveList are just there for better performance. If they are null, we will get the game state and move list from the database and decode them using CBOR.
            // If they are passed in, however, the server will use them instead and won't have to waste time and resources decoding them from the database every turn.
            CurrentGames.update({ CurrentGames.game_ID eq gameID }) {
                it[incremental_moves] = cbor.encodeToByteArray(moveList.add(element=change))
            }
            currentGameState[change[0][0]/* row */][change[0][1] /* column */] = change[1] /* card info, given we are replacing the whole info */
            CurrentGames.update({ CurrentGames.game_ID eq gameID }) {
                it[current_game_state] = cbor.encodeToByteArray(currentGameState)
            }
            commit()
        }
    }
    fun saveGameToArchive (
        gameID: UUID,
        player1ID: Int,
        player2ID: Int,
        player1Deck: ByteArray,
        player2Deck: ByteArray,
        gameWinner: Boolean,
        gameLength: Int,
        moveList: ByteArray
    ) {
        transaction {
            PreviousGames.insert {
                it[game_ID] = gameID
                it[player1_ID] = player1ID
                it[player1_deck] = player1Deck
                it[player2_ID] = player2ID
                it[player2_deck] = player2Deck
                it[winner] = gameWinner
                it[game_length] = gameLength
                it[moves] = moveList
            }
            commit()
        }
    }
}


object CurrentGames: Table() {
    val game_ID: Column<EntityID<UUID>> = uuid("id")
        .autoGenerate()
        .autoIncrement()
        .entityId()
    // Not sure if this is how you make a primary key, yet to be tested.
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(game_ID) }
    val player1_ID: Column<Int> = integer("player1")
    val player2_ID: Column<Int> = integer("player2")
    val current_game_state: Column<ByteArray> = binary("current_game_state") // Array as a ByteArray. Decode with CBOR.
    val incremental_moves: Column<ByteArray> = binary("incremental_moves") // same here.
}

object PreviousGames: Table() {
    val game_ID: Column<UUID> = uuid("id")
    val player1_ID: Column<Int> = integer("player1")
    val player1_deck: Column<ByteArray> = binary("player1_deck") // Array of card IDs as a ByteArray.
    val player2_ID: Column<Int> = integer("player2")
    val player2_deck: Column<ByteArray> = binary("player2_deck") // same here.
    val winner: Column<Boolean> = bool("winner") // False for player 1, True for player 2.
    val game_length: Column<Int> = integer("game_length")
    val moves: Column<ByteArray> = binary("moves")
    // Moves will be stored as nested lists encoded into a ByteArray.
    // Like: [[Change 1, from, to, ...], [Change 2, ...]]
    // Base state is the state of the board before the first move. Because it's always the same, we don't have to store it in the database.
    // The changes are incremental.
    // We can decode the ByteArray back into nested arrays with CBOR for replays or training.
}
