package objects.packets

import kotlinx.serialization.*

/**
 * Sent by: Server
 *
 * The connection was closed by the server. It will no longer respond to packets and has closed the websocket connection.
 *
 * The client may choose to use its own message instead of the provided one, but it
 * is expected to inform the user before returning to a main menu or similar.
 *
 * @param message a human-readable string explaining the disconnect.
 */
@Serializable
@SerialName(PacketType.DISCONNECT)
class DisconnectPacket(
    @Required val reason: Reason,
    @Required val message: String,
) : Packet() {
    @Serializable
    @Suppress("EnumEntryName")
    enum class Reason {
        /**
         * authentication failed
         */
        auth_invalid,

        /**
         * the user is banned
         */
        auth_user_banned,

        /**
         * the server uses a newer protocol than the client supports
         */
        protocol_too_old,

        /**
         * the opponent has disconnected
         */
        opponent_disconnect,

        /**
         * game over
         */
        game_over,

        /**
         * this user is already in a game / queue
         */
        double_login,
    }
}
