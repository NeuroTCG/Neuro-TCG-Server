All gameplay related connections happen on the `/game` endpoint. A new connection starts out with only the init packets being valid.

All packets include a `type` entry.
# To Custom Clients
If any of this information is not accurate, please inform us of this. Many things that indicate a potential bug in the client are marked and should not be reported to our issue tracker, but yours. Make sure your users are aware of this and that they are not using the official client. If you find a potential bug in the server, you are encouraged to report this to us.
# Init Packets
## ClientInfo Packet
Sent by: Client

This is the first packet that is sent for any connection to `/game`.

The client name and version can be up to 15 and 40 characters long respectively. They are only used for statistics.

The protocol version is the latest version that the client supports. The server checks this and determines if it is compatible. If not, a `Disconnect` packet is sent.

``` json
{
  "type": "client_info",
  "client_name": "Official Client",
  "client_version": "0.0.1",
  "protocol_version": 1,
}
```
## ClientInfoAccept Packet
Sent by: Server

The server has received and accepted the client info. It is now waiting for an `Authenticate` packet.

``` json
{
  "type": "client_info_accept",
}
```
## Disconnect Packet
Sent by: Server

The connection was closed by the server. It will no longer respond to packets and has closed the websocket connection.

`reason` is one of the following:
- `auth_invalid` authentication failed
- `auth_user_banned` the user is banned
- `protocol_too_old` the server uses a newer protocol than the client supports

`message` is a human readable string speaking to the user. It explains the reason for this disconnect. The client may choose to use its own message, but it is expected to inform the user before returning to a main menu or similar.

``` json
{
  "type": "disconnect",
  "reason": "protocol_too_old",
  "message": "Your game is outdated. Please update it or inform your client maintainer.",
}
```
## Authenticate Packet
Sent by: Client

This packet associates a connection with a user.

If authentication fails, a `Disconnect` packet is sent.

``` json
{
  "type": "authenticate",
  "username": "Neuro",
}
```
## AuthenticationValid Packet
Sent by: Server

Authentication was successful. The client now waits for a `MatchFound` packet. The client should inform the user that matchmaking is happening.

`has_running_game` informs the client that a game is still running and the `MatchFound` packet will be an existing game. This information is duplicated in the `MatchFound` packet. (this is so the client can display "reconnecting..." instead of "waiting for opponent...")

``` json
{
  "type": "authentication_valid",
  "has_running_game": false,
}
```
## MatchFound Packet
Sent by: Server

This informs the client that a match is found. It includes information on the opponent. The client and server may now use all gameplay packets. The client can get the game state using the `GetGameState` packet with the reason as `connect` or `reconnect`. Do **not** generate this state on your own.

The `game_id` can be used to spectate the game, if we decide to implement that.

``` json
{
  "type": "match_found",
  "opponent": {
    "username": "Evil",
    "region": "Vedals PC", // maybe
  },
  "game_id": "7c178559-9611-46a7-96e0-37fc9fe5241a",
  "is_reconnect": false,
}

```
## UnknownPacket Packet
Sent by: Both

This informs the other party that an unknown packet was received. If a client receives this, it is either out of date or has a bug.

The client may send this packet to the server, but if a client doesn't understand a packet, it is likely their fault. Sending this packet to the server will result in the connection being closed. Use this opportunity to inform the user of this and they can maybe continue the game with a newer version or the official client. Please don't try to ignore an unknown packet, as it will most likely result in state conflicts later and will just make for a bad user experience.

If the causing packet had a `response_id`, it will be repeated here. If not, it is set to null.

This is the only packet that is valid in both the init phase and the gameplay phase.

``` json
{
  "type": "unknown_packet",
  "response_id": 17,
}
```
# Gameplay Packets
To allow for both parties to communicate simultaniously, all packets that expect a response must include a unique `response_id` that will be included in the response packet. Client and server only have to be unique by themselves, meaning both a client and a server packet may have the same `response_id`. They obviously also don't have to be unique with respect to the opponent. If you really want to, you can reuse ids after having received all responses to it, but this is not recommended, just use a counter.

For all response packets, there is a `valid` entry.

If `valid` is `true`:
- The action attempted by the client is valid and was performed
- The opponent was informed of this action
- The client must update its state as in [Updating Client State](#updating-client-state).

If `valid` is `false`:
- The action attempted by the client is invalid and was not performed
- The opponent does not get informed
- Any additional fields are set to null
- If this action should have been valid, the client must refresh its state using a `GetGameState` packet. If the action still fails, this is most likely a bug in the client.

Any opponent packets indicate that the opponent did something. The client must update its state as in [Updating Client State](#updating-client-state).
## Updating Client State
The client is expected to update its game state using the given information in the packet. In case of a conflict with existing state (e.g. no card where the opponent is attacking from), the client must disregard its own state and request the full state with a `GetGameState` packet.

It should also inform the user of this to reduce confusion

Any state conflict is most likely a bug in the client.
## GetGameState Packet
Sent by: Client

`reason` must be one of the following:
- `state_conflict`: The client cannot apply some state update due to a conflict
- `reconnect`: The client has reconnected to an existing game
- `connect`: Same as `reconnect`, but for a new game.
- `debug`: The client wants to get the current state for debugging purposes, this includes proactively checking if the current state matches what the server has

If your reason isn't covered by these options, please inform us of this and don't just use `debug`.

``` json
{
  "type": "get_game_state",
  "response_id": 1,
  "reason": "reconnect",
}
```
## Summon Packet
Sent by: Client

Tries to summon a card at a given position.

``` json
{
  "type": "summon",
  "response_id": 2,
  "card_id": 3,
  "position": [1, 2],
}
```
## SummonResponse Packet
Sent by: Server

``` json
{
  "type": "summon_response",
  "response_id": 2,
  "valid": true,
  "new_card": {
    "id": 3,
    "health": 100,
  },
}
```
## SummonOpponent Packet
Sent by: Server

``` json
{
  "type": "summon_opponent",
  "position": [1, 2],
  "new_card": {
    "id": 3,
    "health": 100,
  },
}
```
## Attack Packet
Sent by: Client

Tries to attack the card at `target_position` with the card at `attacker_position`.
``` json
{
  "type": "attack",
  "response_id": 3,
  "target_position": [1, 1],
  "attacker_position": [0, 2],
}
```
## AttackResponse Packet
Sent by: Server

If any of the cards were killed by this attack, they will be set to `null`.

``` json
{
  "type": "attack_response",
  "response_id": 3,
  "valid": true,
  "target_card": {
    "id": 1,
    "health": 29,
  },
  "attacker_card": {
    "id": 3,
    "health": 90,
  },
}
```
## AttackOpponent Packet
Sent by: Server

If any of the cards were killed by this attack, they will be set to `null`.

``` json
{
  "type": "attack_opponent",
  "target_position": [1, 1],
  "attacker_position": [0, 2],
  "target_card": {
    "id": 1,
    "health": 29,
  },
  "attacker_card": {
    "id": 3,
    "health": 90,
  },
}
```
