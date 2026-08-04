# Jape

Jape is a save-game editor for Jagged Alliance 2. It edits mercenary
records, including their inventory and equipment slots.

## Language

**Armor slot**:
One of the three equipment slots on a mercenary that take armor: the
Helmet slot (head), the Body Armor slot (chest), and the Leg Armor slot
(legs). The two headgear slots are not armor slots.

**Equippable**:
An item is equippable to a slot when its item category matches the
slot's category (e.g. a HELMET_CATEGORY item fits the Helmet slot).
This is Jape's proxy for the game's real equip rules, which Jape does
not model.

**Ammo**:
The item loaded into a weapon's ammo slot. Only AMMO_CATEGORY items
can be loaded.

**Attachment**:
An item fitted to a weapon or armor. Weapons take weapon attachments
(WEAPON_ATTACHMENT_CATEGORY); body armor takes armor attachments
(ARMOR_ATTACHMENT_CATEGORY, i.e. ceramic plates).

**Active**:
The group of mercenaries in the actor tree (labeled "Active") that
have an active SOLDIERTYPE record in the save game, i.e. the mercs
actually on the squad. Determined by joining actor nicknames to the
merc records.

**Recruitable**:
The group of mercenaries in the actor tree that can be hired or
recruited during a campaign: the AIM and MERC agency rosters (the
first 51 MERCPROFILE entries, through Bubba) plus a hand-curated list
of rebel and special RPC nicknames. Kept as nicknames in source so the
list survives across saves.

**Other actors**:
The actor-tree group for every remaining MERCPROFILE: NPCs, enemies,
and unused profile slots.

**Autosave**:
The toolbar checkbox (on by default) that saves the open save game
immediately after every change.

**Equipped**:
The mercenary's loadout slots — the two headgear slots, the three
armor slots, and the two hand slots — as copied and pasted by the
"Copy/Paste Equipped" toolbar buttons.
