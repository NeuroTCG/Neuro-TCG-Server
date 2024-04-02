import kotlin.system.exitProcess
import objects.Game
suspend fun main() {
    val game = Game()
    game.connection.start()
//    // boardState test:
//    println(game.boardState.getBoardState())
//    for (i in 0..1) {
//        for (j in 0..2) {
//            for (k in 0..3-j) {
//                game.boardState.update(i, j, k, (1..32).random())
//                println(game.boardState.getBoardState())
//            }
//        }
//    }
    exitProcess(0)
}
