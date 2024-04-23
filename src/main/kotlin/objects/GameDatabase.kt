package objects
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.sql.Connection
import java.util.*

class GameDatabase {
    private val db = Database.connect("jdbc:sqlite:/data/data.db", "org.sqlite.JDBC")
    suspend fun createTables() {
        TransactionManager.defaultDatabase = db
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        SchemaUtils.createMissingTablesAndColumns(CurrentGames, PreviousGames)
    }
}

object CurrentGames: Table() {
    val game_ID: Column<EntityID<UUID>> = uuid("id")
        .autoGenerate()
        .autoIncrement()
        .entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(game_ID) }
    val player1_ID: Column<Int> = integer("player1")
    val player2_ID: Column<Int> = integer("player2")
    val current_game_state: Column<ByteArray> = binary("current_game_state") // Array as a ByteArray. Decode with CBOR.
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
    // Like: [[Base state], [Change 1, from, to, ...], [Change 2, ...]]
    // Base state is the state of the board before the first move.
    // The changes are incremental.
    // We can decode the ByteArray back into nested arrays with CBOR for replays or training.
}
