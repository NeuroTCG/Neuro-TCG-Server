package objects.packets

class PacketType {
    companion object {
        const val CLIENT_INFO = "client_info"
        const val CLIENT_INFO_ACCEPT = "client_info_accept"
        const val DISCONNECT = "disconnect"
        const val AUTHENTICATE = "authenticate"
        const val AUTHENTICATION_VALID = "authentication_valid"
        const val RULE_INFO = "rule_info"
        const val MATCH_FOUND = "match_found"

        const val UNKNOWN_PACKET = "unknown_packet"
        const val GET_BOARD_STATE = "get_board_state"
        const val GET_BOARD_STATE_RESPONSE = "get_board_state_response"
        const val SUMMON_REQUEST = "summon_request"
        const val SUMMON = "summon"
        const val ATTACK_REQUEST = "attack_request"
        const val ATTACK = "attack"
    }
}
