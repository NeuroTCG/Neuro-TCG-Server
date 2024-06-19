package objects.packets

class PacketType {
    companion object {
        const val CLIENT_INFO = "client_info"
        const val CLIENT_INFO_ACCEPT = "client_info_accept"
        const val DISCONNECT = "disconnect"
        const val AUTHENTICATE = "authenticate"
        const val AUTHENTICATION_VALID = "authentication_valid"
        const val MATCH_FOUND = "match_found"
        const val UNKNOWN_PACKET = "unknown_packet"
        const val GET_BOARD_STATE = "get_board_state"
        const val GET_BOARD_STATE_RESPONSE = "get_board_state_response"
        const val SUMMON = "summon"
        const val SUMMON_RESPONSE = "summon_response"
        const val SUMMON_OPPONENT = "summon_opponent"
        const val ATTACK = "attack"
        const val ATTACK_RESPONSE = "attack_response"
        const val ATTACK_OPPONENT = "attack_opponent"
    }
}
