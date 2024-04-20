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
    val current_game_state: Column<String> = varchar("current_game_state", 8000) // Array as a string. Don't have any better idea how to store it.
}

object PreviousGames: Table() {
    val game_ID: Column<UUID> = uuid("id")
    val player1_ID: Column<Int> = integer("player1")
    val player1_Deck: Column<String> = varchar("player1_deck", 8000) // Array of card IDs as a string.
    val player2_ID: Column<Int> = integer("player2")
    val player2_Deck: Column<String> = varchar("player2_deck", 8000) // same here.
    val winner: Column<ByteArray> = binary("winner") // 0 for player 1, 1 for player 2.
    val game_length: Column<Int> = integer("game_length")
    val moves: Column<String> = varchar("moves", 8000)
    // Moves will be stored as a string of nested Arrays.
    // Like: [[Base state], [Change 1, from, to, ...], [Change 2, ...]]
    // Base state is the state of the board before the first move.
    // The changes are incremental.
    // The string will later be converted to a list of moves for replays or training.
}

/*
//   String to Array reference:
fun main() {
    val string = "[[\"base state\"], [\"change 1\", \"from\", \"to\"], [\"change 2\", \"from\", \"to\"]]"
    val arrayOfArrays: List<List<String>> = parseStringToArrayOfArrays(string)
    println(arrayOfArrays)
}

fun parseStringToArrayOfArrays(input: String): List<List<String>> {
    val trimmedInput = input.trim('[', ']')
    val parts = trimmedInput.split("], [")
    return parts.map { part ->
        part.split(", ")
    }
}
 */

