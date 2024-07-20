All gameplay related connections happen on the `/game` endpoint. A new connection starts out with only the init packets
being valid.

All packets include a `type` entry.

# To Custom Clients

If any of this information is not accurate, please inform us of this. Many things that indicate a potential bug in the
client are marked and should not be reported to our issue tracker, but yours. Make sure your users are aware of this and
that they are not using the official client. If you find a potential bug in the server, you are encouraged to report
this to us.

# Objects

The api uses the same data layouts multiple times. These are called objects.

## UserInfo Object

```json
{
    "username": "Evil",
    // maybe
    "region": "Vedals PC"
}
```

## CardState Object

The state of a card when on the board

```json
{
    "id": 3,
    "health": 100
}
```

## CardStats Object

The stats, name, etc. for all instances of one card. IDs are not given out in order.

```json
{
    "max_hp": 100,
    "base_atk": 50
}
```

## CardPosition Object

Card positions are always relative to the side of the board, which is usually inferred.
The numbering looks like this:

```
          opponent
          
    [1, 2] [1, 1] [1, 0]
[0, 3] [0, 2] [0, 1] [0, 0]
-------------------------
[0, 0] [0, 1] [0, 2] [0, 3]
   [1, 0] [1, 1] [1, 2] 
   
           you
```

```json
[
    0,
    0
]
```

## BoardState Object

Each of the nested objects in `cards` can be a CardState or null.
Each of the nested objects in `traps` can be a TrapState(unimplemented) or null.

```json
{
    "cards": [
        // player 1
        [
            // local bottom row
            [
                null,
                null,
                null,
                null
            ],
            // local top row
            [
                null,
                null,
                null
            ]
        ],
        // player 2
        [
            // local bottom row
            [
                null,
                null,
                null,
                null
            ],
            // local top row
            [
                null,
                null,
                null
            ]
        ]
    ],
    "traps": [
        // player 1
        [
            null,
            // local left
            null
            // local right
        ],
        // player 2
        [
            null,
            // local left
            null
            // local right
        ]
    ],
    "first_player_active": true,
    "hands": [
        // player 1
        [
            1,
            2,
            17
        ],
        // player 2
        [
            2,
            4
        ]
    ]
}
```

# Init Packets

## ClientInfo Packet

Sent by: Client

This is the first packet that is sent for any connection to `/game`.

The client name and version can be up to 15 and 40 characters long respectively. They are only used for statistics.

The protocol version is the latest version that the client supports. The server checks this and determines if it is
compatible. If not, a `Disconnect` packet is sent.

```json
{
    "type": "client_info",
    "client_name": "Official Client",
    "client_version": "0.0.1",
    "protocol_version": 1
}
```

## ClientInfoAccept Packet

Sent by: Server

The server has received and accepted the client info. It is now waiting for an `Authenticate` packet.

```json
{
    "type": "client_info_accept"
}
```

## Disconnect Packet

Sent by: Server

The connection was closed by the server. It will no longer respond to packets and has closed the websocket connection.

`reason` is one of the following:

- `auth_invalid` authentication failed
- `auth_user_banned` the user is banned
- `protocol_too_old` the server uses a newer protocol than the client supports
- `opponent_disconnect` the opponent has disconnected

`message` is a human-readable string speaking to the user. It explains the reason for this disconnect. The client may
choose to use its own message, but it is expected to inform the user before returning to a main menu or similar.

```json
{
    "type": "disconnect",
    "reason": "protocol_too_old",
    "message": "Your game is outdated. Please update it or inform your client maintainer."
}
```

## Authenticate Packet

Sent by: Client

This packet associates a connection with a user.

If authentication fails, a `Disconnect` packet is sent.

```json
{
    "type": "authenticate",
    "username": "Neuro"
}
```

## AuthenticationValid Packet

Sent by: Server

Authentication was successful. The client now waits for a `MatchFound` packet. The client should inform the user that
matchmaking is happening.

`has_running_game` informs the client that a game is still running and the `MatchFound` packet will be an existing game.
This information is duplicated in the `MatchFound` packet. (this is so the client can display "reconnecting..." instead
of "waiting for opponent...")

`you` is a UserInfo object.

```json
{
    "type": "authentication_valid",
    "has_running_game": false,
    "you": {
        "username": "Neuro",
        "region": "Vedals PC"
    }
}
```

## RuleInfo Packet

Sent by: Server

Informs the client of the rules used for the upcoming match. This is sent directly after the AuthenticationValid packet.

Each of the values in `card_id_mapping` is a CardStats object.

```json
{
    "type": "rule_info",
    "card_id_mapping": {
        "0": {
            "max_hp": 100,
            "base_atk": 50
        },
        "1": {
            "max_hp": 200,
            "base_atk": 5
        }
    }
}
```

## MatchFound Packet

Sent by: Server

This informs the client that a match is found. It includes information on the opponent. The client and server may now
use all gameplay packets. The client can get the game state using the `GetGameState` packet with the reason as `connect`
or `reconnect`. Do **not** generate this state on your own.

The `game_id` can be used to spectate the game, if we decide to implement that.

`opponent` is a UserInfo object.
`is_first_player` indicates if you are the first or second player

```json
{
    "type": "match_found",
    "opponent": {
        "username": "Evil",
        "region": "Vedals PC"
    },
    "game_id": "1243",
    "is_reconnect": false,
    "is_first_player": true
}

```

## UnknownPacket Packet

Sent by: Both

This informs the other party that an unknown packet was received. If a client receives this, it is either out of date or
has a bug.

The client may send this packet to the server, but if a client doesn't understand a packet, it is likely their fault.
Sending this packet to the server will result in the connection being closed. Use this opportunity to inform the user of
this, and they can maybe continue the game with a newer version or the official client. Don't try to ignore an
unknown packet, as it will most likely result in state conflicts later and will just make for a bad user experience.

This is the only packet that is valid in both the init phase and the gameplay phase.

```json
{
    "type": "unknown_packet",
    "message": "packet type 'i_win_now' does not exist"
}
```

# Gameplay Packets

To allow for both parties to communicate simultaneously, all packets that expect a response must include a
unique `response_id` that will be included in the response packet. Client and server only have to be unique by
themselves, meaning both a client and a server packet may have the same `response_id`. They obviously also don't have to
be unique with respect to the opponent. If you really want to, you can reuse ids after having received all responses to
it, but this is not recommended, just use a counter.

For all response packets, there is a `valid` entry.

If `valid` is `true`:

- The action attempted by the client is valid and was performed
- The opponent was informed of this action
- The client must update its state as in [Updating Client State](#updating-client-state).

If `valid` is `false`:

- The action attempted by the client is invalid and was not performed
- The opponent does not get informed
- Any additional fields are set to null
- If this action should have been valid, the client must refresh its state using a `GetGameState` packet. If the action
  still fails, this is most likely a bug in the client.

## Updating Client State

The client is expected to update its game state using the given information in the packet. In case of a conflict with
existing state (e.g. no card where the opponent is attacking from), the client must disregard its own state and request
the full state with a `GetGameState` packet.

It should also inform the user of this to reduce confusion

Any state conflict is most likely a bug in the client.

## GetBoardState Packet

Sent by: Client

`reason` must be one of the following:

- `state_conflict`: The client cannot apply some state update due to a conflict
- `reconnect`: The client has reconnected to an existing game
- `connect`: Same as `reconnect`, but for a new game.
- `debug`: The client wants to get the current state for debugging purposes, this includes proactively checking if the
  current state matches what the server has

If your reason isn't covered by these options, please inform us of this and don't just use `debug`.

```json
{
    "type": "get_board_state",
    "reason": "reconnect"
}
```

## GetBoardStateResponse Packet

Sent by: Server

Contains the full game state. The client should validate that this matches its own state and/or replace it.

`board` is a BoardState object.

```json
{
    "type": "get_board_state_response",
    "board": {
        "cards": [
            [
                [
                    null,
                    null,
                    null,
                    null
                ],
                [
                    null,
                    null,
                    null
                ]
            ],
            [
                [
                    null,
                    null,
                    null,
                    null
                ],
                [
                    null,
                    null,
                    null
                ]
            ]
        ],
        "traps": [
            [
                null,
                null
            ],
            [
                null,
                null
            ]
        ],
        "first_player_active": true,
        "hands": [
            [
                3,
                19
            ],
            [
                9,
                2,
                987
            ]
        ]
    }
}
```

## SummonRequest Packet

Sent by: Client

Tries to summon a card at a given position.

`position` is a CardPosition Object.

```json
{
    "type": "summon_request",
    "card_id": 3,
    "position": [
        1,
        2
    ]
}
```

## Summon Packet

Sent by: Server

Informs the client of a summon by either it or the opponent. `is_you` indicates whose action it was.

`new_card` is a CardState object or null.
`position` is a CardPosition object or null.

```json
{
    "type": "summon",
    "is_you": true,
    "valid": true,
    "position": [
        1,
        2
    ],
    "new_card": {
        "id": 3,
        "health": 100
    }
}
```

## AttackRequest Packet

Sent by: Client

Tries to attack the card at `target_position` with the card at `attacker_position`.

`target_position` is a CardPosition Object.
`attacker_position` is a CardPosition Object.

```json
{
    "type": "attack_request",
    "target_position": [
        1,
        1
    ],
    "attacker_position": [
        0,
        2
    ]
}
```

## Attack Packet

Sent by: Server

Informs the client of an attack by either it or the opponent. `is_you` indicates whose action it was.

If any of the cards were killed by this attack, they will be set to `null`.

`target_card` is a CardState object or null.
`attacker_card` is a CardState object or null.
`target_position` is a CardPosition Object.
`attacker_position` is a CardPosition Object.

```json

{
    "type": "attack",
    "is_you": true,
    "valid": true,
    "target_position": [
        1,
        1
    ],
    "attacker_position": [
        0,
        2
    ],
    "target_card": {
        "id": 1,
        "health": 29
    },
    "attacker_card": {
        "id": 3,
        "health": 90
    }
}
```

## SwitchPlaceRequest Packet

Sent by: Client

Tries to swap two cards or a card and null.

`position1` is a CardPosition Object.
`position2` is a CardPosition Object.

```json
{
    "type": "switch_place_request",
    "position1": [
        2,
        0
    ],
    "position2": [
        1,
        1
    ]
}
```

## SwitchPlace Packet

Sent by: Server

Informs the client that two cards or a card and `null` have swapped places. `is_you` indicates whose action it was.

`position1` is a CardPosition Object.
`position2` is a CardPosition Object.

```json
{
    "type": "switch_place",
    "is_you": false,
    "valid": true,
    "position1": [
        2,
        0
    ],
    "position2": [
        1,
        1
    ]
}
```

## StartTurn Packet

Sent by: Server

```json
{
    "type": "start_turn"
}
```

## EndTurn Packet

Sent by: Server or Client

```json
{
    "type": "end_turn"
}
```

## DrawCardRequest Packet

Sent by: Client

```json
{
    "type": "draw_card_request"
}
```

## DrawCard Packet

Sent by: Server

A negative card ID indicates that the action was invalid.

```json
{
    "type": "draw_card",
    "card_id": 3
}
```
