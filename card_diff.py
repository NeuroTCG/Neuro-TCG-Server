import json
import csv
import os
import sys
from pprint import pprint
import difflib
import traceback
from typing import Any


COLUMN_ASSIGNMENTS = {
    "name": ord("B") - ord("A"),
    "summoning_cost": ord("E") - ord("A"),
    "base_atk": ord("F") - ord("A"),
    "max_hp": ord("G") - ord("A"),
    "tactics": ord("H") - ord("A"),
    "atk_range": ord("I") - ord("A"),
    "ability_range": ord("J") - ord("A"),
    "ability_cost": ord("K") - ord("A"),
    "ability_value": ord("L") - ord("A"),
    "ability_effect": ord("M") - ord("A"),
    "passive_effect": ord("O") - ord("A"),
}


class Passive:
    def __init__(self, effect: str, values: list[int]):
        self.effect: str = effect
        self.values: list[int] = values


class Ability:
    def __init__(self, cost: int, effect: str, range: str, value: int):
        self.cost: int = cost
        self.effect: str = effect.upper().replace(" ", "_")
        self.range: str = range.upper().replace(" ", "_")
        self.value: int = value


class Card:
    def __init__(self, card_type: str, row: list[str]):
        self.card_type: str = card_type
        self.name: str = row[COLUMN_ASSIGNMENTS["name"]].strip()

        sc = row[COLUMN_ASSIGNMENTS["summoning_cost"]]
        if sc == "N/A":
            self.summoning_cost: int | None = 0  # deckmasters
        elif sc == "":
            self.summoning_cost = None
        else:
            self.summoning_cost = int(sc)

        ba = row[COLUMN_ASSIGNMENTS["base_atk"]]
        if ba == "":
            self.base_atk: int = -1  # unfinished cards
        else:
            self.base_atk = int(ba)

        hp = row[COLUMN_ASSIGNMENTS["max_hp"]]
        if hp == "":
            self.max_hp = -1  # unfinished cards
        else:
            self.max_hp = int(hp)

        self.tactics: tuple[str, ...] = tuple(
            x.strip()
            for x in row[COLUMN_ASSIGNMENTS["tactics"]]
            .upper()
            .replace("N/A", "")
            .replace(",", "/")
            .split("/")
            if x
        )
        # self.atk_range: str = row[COLUMN_ASSIGNMENTS["atk_range"]].upper().strip()

        ab_value = row[COLUMN_ASSIGNMENTS["ability_value"]].strip()
        ab_effect = row[COLUMN_ASSIGNMENTS["ability_effect"]].strip()
        ab_range = row[COLUMN_ASSIGNMENTS["ability_range"]].strip()
        ab_cost = row[COLUMN_ASSIGNMENTS["ability_cost"]].strip()

        if any([ab_value, ab_effect, ab_range, ab_cost]):
            if ab_value == "":
                ab_value = -1
            if ab_cost == "":
                ab_cost = -1
            if ab_range == "":
                ab_range = "NONE"
            if ab_effect == "":
                ab_range = "UNKNOWN"
            if ab_effect.upper() == "NONE":
                ab_range = "NONE"
                ab_cost = 0
                ab_value = -1

            self.ability: Ability | None = Ability(
                int(ab_cost),
                ab_effect,
                ab_range,
                int(ab_value),
            )
        else:
            self.ability = None
        self.passive: Passive = Passive(row[COLUMN_ASSIGNMENTS["passive_effect"]], [])


def serialize(obj):
    return vars(obj)


def consume_empty_lines(reader):
    while True:
        value = next(reader)
        if any(value):
            break


def read_and_parse_spreadsheet(
    spreadsheet_path: str,
) -> tuple[list[Card], list[list[str]]]:
    reader = csv.reader(open(spreadsheet_path).readlines())

    sheet_cards: list[Card] = []

    while True:
        row = next(reader)
        if row[0] == "Deck Masters":
            break

    unparsable_cards: list[list[str]] = []

    current_spreadsheet_type = "Deck Masters"
    for card_type, next_spreadsheet_type in [
        ("DECK_MASTER", "Creatures"),
        ("CREATURE", "Magic Cards"),
        ("MAGIC", "Trap Cards"),
        ("TRAP", "Token Cards"),
        ("TOKEN", "Cards to be"),
    ]:
        print(f"Parsing {current_spreadsheet_type}")
        while True:
            row = next(reader)
            if not any(row):
                continue
            if row[0].strip() == next_spreadsheet_type:
                break
            if row[COLUMN_ASSIGNMENTS["name"]].strip() == "":
                continue

            print(f"    Parsing card {row[COLUMN_ASSIGNMENTS["name"]]}")

            try:
                card = Card(card_type, row)
            except Exception as e:
                print(traceback.format_exc())
                unparsable_cards.append(row)
                continue

            sheet_cards.append(card)

        current_spreadsheet_type = next_spreadsheet_type

    return sheet_cards, unparsable_cards


def check_missing_graphics(server_cards: dict[str, dict]) -> list[dict[str, dict]]:
    missing_graphics = []
    print("Checking for missing graphics")
    for card in server_cards.values():
        image_path = card["graphics"].replace("res://", "../Neuro-TCG-Client/")
        if not os.path.isfile(image_path):
            missing_graphics.append(card)

    return missing_graphics


def diff_cards(
    server_cards: dict[str, dict[str, Any]], sheet_cards: list[Card]
) -> tuple[list[Card], list[tuple[Card, dict[str, Any]]]]:
    server_missing_cards: list[Card] = []
    different_cards: list[tuple[Card, dict[str, Any]]] = []

    for card in sheet_cards:
        matches = [
            x for x in server_cards.values() if x["name"].strip() == card.name.strip()
        ]
        if len(matches) == 0:
            server_card_json = ""
            server_missing_cards.append(card)
            server_card = {}
        elif len(matches) > 1:
            print(f"Card {card.name} has multiple matches on server: {matches}")
            continue
        else:
            server_card = matches[0].copy()
            del server_card["graphics"]
            server_card_json = json.dumps(
                server_card, default=serialize, sort_keys=True, indent=2
            )

        sheet_card_json = json.dumps(card, default=serialize, sort_keys=True, indent=2)

        if (
            sheet_card_json != server_card_json
            and sheet_card_json != ""
            and server_card_json != ""
        ):
            different_cards.append((card, server_card))

        print(
            "\n".join(
                difflib.unified_diff(
                    server_card_json.split("\n"),
                    sheet_card_json.split("\n"),
                    f"server/{card.name}",
                    f"spreadsheet/{card.name}",
                    n=99999,
                    lineterm="",
                )
            )
        )

    return server_missing_cards, different_cards


def find_sheet_missing_cards(
    server_cards: dict[str, dict[str, Any]], sheet_cards: list[Card]
) -> list[dict[str, Any]]:
    spreadsheet_missing_cards: list[dict[str, Any]] = []
    for card in server_cards.values():
        matches = [x for x in sheet_cards if x.name.strip() == card["name"].strip()]
        if len(matches) != 1:
            spreadsheet_missing_cards.append(card)

    return spreadsheet_missing_cards


def main():
    print("Building and running server to dump cards")
    server_cards: dict[str, dict] = json.loads(
        "\n".join(os.popen('./gradlew run -q --args="--dump-cards"').readlines())
    )

    try:
        spreadsheet_path = sys.argv[1]
    except IndexError:
        print("No spreadsheet given. Exiting.")
        sys.exit(1)

    print(f"Reading spreadsheet from {spreadsheet_path}")
    sheet_cards, unparsable_cards = read_and_parse_spreadsheet(spreadsheet_path)
    server_missing_cards, different_cards = diff_cards(server_cards, sheet_cards)
    missing_graphics = check_missing_graphics(server_cards)

    print("# Summary")
    print(f"Parsed {len(sheet_cards)} cards")

    print(f"{len(unparsable_cards)} columns couldn't be parsed as a card:")
    for card in unparsable_cards:
        print("   ", card)

    print(f"{len(missing_graphics)} cards have missing graphics:")
    for card in missing_graphics:
        print("   ", card["name"])

    print(f"{len(server_missing_cards)} cards are missing from the server:")
    for card in server_missing_cards:
        print("   ", card.name)

    spreadsheet_missing_cards = find_sheet_missing_cards(server_cards, sheet_cards)
    print(f"{len(spreadsheet_missing_cards)} cards are missing from the spreadsheet:")
    for card in spreadsheet_missing_cards:
        print("   ", card["name"])

    print(f"{len(different_cards)} cards are different in the server and spreadsheet:")
    for card, server_card in different_cards:
        print("   ", card.name)


if __name__ == "__main__":
    main()
