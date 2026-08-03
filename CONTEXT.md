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
