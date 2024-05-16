package objects.packets

import kotlinx.serialization.*

@Serializable
enum class PacketType {
    client_info,
    client_info_accept,
    disconnect,
    authenticate,
    authentication_valid,
    match_found,
    unknown_packet,
    get_game_state,
    summon,
    summon_response,
    summon_opponent,
    attack,
    attack_response,
    attack_opponent,
}
